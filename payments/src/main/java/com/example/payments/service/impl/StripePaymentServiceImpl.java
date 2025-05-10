package com.example.payments.service.impl;

import com.example.payments.config.RabbitMqConfig;
import com.example.payments.config.StripeConfig;
import com.example.payments.dto.event.PaymentFailedEventDto;
import com.example.payments.dto.event.PaymentSucceededEventDto;
import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.response.CreatePaymentIntentResponseDto;
// Assuming you have a PaymentIntentRecord entity and repository
// import com.example.payments.entity.PaymentIntentRecord;
// import com.example.payments.repository.PaymentIntentRecordRepository;
import com.example.payments.service.PaymentService;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook; // For signature verification
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // If updating local DB

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentServiceImpl implements PaymentService {

    private final StripeConfig stripeConfig; // To get the webhookSigningSecret
    private final RabbitTemplate rabbitTemplate;
    // private final PaymentIntentRecordRepository paymentIntentRecordRepository; // If using local DB

    // ... createPaymentIntent method from before ...

    @Override
    @Transactional // Make transactional if you update your own DB based on webhook
    public void handleStripeWebhookEvent(String payload, String sigHeader) throws StripeException {
        Event event;
        try {
            // Verify the event by using the webhook signing secret
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSigningSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Webhook error while validating signature: {}", e.getMessage());
            throw e; // Re-throw to return 400 Bad Request
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to Stripe's documentation on working with API versions and webhooks.
            log.warn("Webhook event data object deserialization failed. Event ID: {}, Type: {}", event.getId(), event.getType());
            // Still acknowledge with 200 OK to Stripe, as we can't process it further.
            return;
        }

        // For idempotency: check if we've already processed this event ID
        // String eventId = event.getId();
        // if (processedEventRepository.existsById(eventId)) {
        //     log.info("Webhook event {} already processed.", eventId);
        //     return; // Return 200 OK
        // }

        PaymentIntent paymentIntent;

        switch (event.getType()) {
            case "payment_intent.succeeded":
                paymentIntent = (PaymentIntent) stripeObject;
                log.info("PaymentIntent Succeeded: ID={}, OrderID (from metadata)={}, UserID (from metadata)={}",
                        paymentIntent.getId(),
                        paymentIntent.getMetadata().get("order_id"),
                        paymentIntent.getMetadata().get("user_id"));

                // TODO: (Optional but recommended) Update local PaymentIntentRecord status to SUCCEEDED
                // PaymentIntentRecord record = paymentIntentRecordRepository.findByPaymentIntentId(paymentIntent.getId())
                //    .orElse(null); // Or create a new one if it somehow doesn't exist
                // if (record != null && !record.getStatus().equals("SUCCEEDED")) { // Idempotency check
                //    record.setStatus("SUCCEEDED");
                //    record.setStripeChargeId(paymentIntent.getLatestCharge()); // Store charge ID
                //    paymentIntentRecordRepository.save(record);

                PaymentSucceededEventDto successEvent = PaymentSucceededEventDto.builder()
                        .eventId(UUID.randomUUID()) // New internal event ID
                        .eventTimestamp(LocalDateTime.now())
                        .orderId(UUID.fromString(paymentIntent.getMetadata().get("order_id")))
                        .userId(paymentIntent.getMetadata().get("user_id"))
                        .paymentIntentId(paymentIntent.getId())
                        .chargeId(paymentIntent.getLatestCharge()) // Get the charge ID
                        .amountPaid(paymentIntent.getAmountReceived()) // Amount in smallest unit
                        .currency(paymentIntent.getCurrency())
                        .paymentMethodType(paymentIntent.getPaymentMethodTypes().isEmpty() ? null : paymentIntent.getPaymentMethodTypes().get(0))
                        .paidAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(paymentIntent.getCreated()), ZoneOffset.UTC)) // Or use succeeded_at if available and preferred
                        .build();
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                        RabbitMqConfig.PAYMENT_EVENT_SUCCEEDED_ROUTING_KEY,
                        successEvent);
                log.info("Published PaymentSucceededEvent for Order ID: {}", successEvent.getOrderId());

                //  processedEventRepository.save(new ProcessedEvent(eventId)); // Mark event as processed
                // } else if (record != null) {
                //    log.info("PaymentIntent {} was already marked SUCCEEDED.", paymentIntent.getId());
                // }
                break;

            case "payment_intent.payment_failed":
                paymentIntent = (PaymentIntent) stripeObject;
                String failureMessage = paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getMessage() : "Unknown failure";
                String failureCode = paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getCode() : null;
                log.warn("PaymentIntent Failed: ID={}, OrderID (metadata)={}, UserID (metadata)={}, Reason: '{}'",
                        paymentIntent.getId(),
                        paymentIntent.getMetadata().get("order_id"),
                        paymentIntent.getMetadata().get("user_id"),
                        failureMessage);

                // TODO: (Optional) Update local PaymentIntentRecord status to FAILED

                PaymentFailedEventDto failedEvent = PaymentFailedEventDto.builder()
                        .eventId(UUID.randomUUID())
                        .eventTimestamp(LocalDateTime.now())
                        .orderId(UUID.fromString(paymentIntent.getMetadata().get("order_id")))
                        .userId(paymentIntent.getMetadata().get("user_id"))
                        .paymentIntentId(paymentIntent.getId())
                        .failureCode(failureCode)
                        .failureMessage(failureMessage)
                        .failedAt(LocalDateTime.now()) // Or a timestamp from the payment_intent if available
                        .build();
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                        RabbitMqConfig.PAYMENT_EVENT_FAILED_ROUTING_KEY,
                        failedEvent);
                log.info("Published PaymentFailedEvent for Order ID: {}", failedEvent.getOrderId());
                break;

            // Add other event types if needed, e.g., payment_intent.canceled, charge.refunded
            default:
                log.info("Received unhandled Stripe event type: {}", event.getType());
        }
        // Event processing is complete for the switch cases that publish.
        // If you had an idempotency store, you'd mark the event.getId() as processed here.
    }

    // --- createPaymentIntent method from previous step ---
    @Override
    @Transactional // Assuming you might save PaymentIntentRecord locally
    public CreatePaymentIntentResponseDto createPaymentIntent(CreatePaymentIntentRequestDto requestDto) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", requestDto.getOrderId().toString());
        metadata.put("user_id", requestDto.getUserId());
        if(requestDto.getDescription() != null) {
            metadata.put("description", requestDto.getDescription());
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(requestDto.getAmount())
                        .setCurrency(requestDto.getCurrency().toLowerCase())
                        .putAllMetadata(metadata)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.debug("Stripe PaymentIntent created: ID={}, Status={}, ClientSecret relevant for FE",
                paymentIntent.getId(), paymentIntent.getStatus());

        // Optional: Save PaymentIntentRecord to local DB
        // PaymentIntentRecord pire = PaymentIntentRecord.builder() ... .build();
        // paymentIntentRecordRepository.save(pire);

        return CreatePaymentIntentResponseDto.builder()
                .paymentIntentId(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .status(paymentIntent.getStatus())
                .build();
    }
}