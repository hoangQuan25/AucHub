package com.example.payments.service.impl;

import com.example.payments.config.RabbitMqConfig;
import com.example.payments.config.StripeConfig;
import com.example.payments.dto.event.PaymentFailedEventDto;
import com.example.payments.dto.event.PaymentSucceededEventDto;
import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.request.RefundRequestedEventDto;
import com.example.payments.dto.response.CreatePaymentIntentResponseDto;
// Assuming you have a PaymentIntentRecord entity and repository
// import com.example.payments.entity.PaymentIntentRecord;
// import com.example.payments.repository.PaymentIntentRecordRepository;
import com.example.payments.dto.response.RefundFailedEventDto;
import com.example.payments.dto.response.RefundSucceededEventDto;
import com.example.payments.service.PaymentService;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook; // For signature verification
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // If updating local DB

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Transactional
    public CreatePaymentIntentResponseDto createPaymentIntent(CreatePaymentIntentRequestDto requestDto) throws StripeException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", requestDto.getOrderId().toString());
        metadata.put("user_id", requestDto.getUserId());
        if(requestDto.getDescription() != null) {
            metadata.put("description", requestDto.getDescription());
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(requestDto.getAmount()) // Amount in smallest currency unit (long)
                        .setCurrency(requestDto.getCurrency().toLowerCase())
                        .putAllMetadata(metadata)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build())
                        // You can add 'capture_method' = 'manual' if you plan to authorize then capture later
                        // For direct charges, this is fine.
                        .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.debug("Stripe PaymentIntent created: ID={}, Status={}, ClientSecret relevant for FE",
                paymentIntent.getId(), paymentIntent.getStatus());

        return CreatePaymentIntentResponseDto.builder()
                .paymentIntentId(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .status(paymentIntent.getStatus())
                .build();
    }


    @Override
    @Transactional
    public void handleStripeWebhookEvent(String payload, String sigHeader) throws StripeException {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSigningSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Webhook error while validating signature: {}", e.getMessage());
            throw e;
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.warn("Webhook event data object deserialization failed. Event ID: {}, Type: {}", event.getId(), event.getType());
            return;
        }

        PaymentIntent paymentIntent;

        switch (event.getType()) {
            case "payment_intent.succeeded":
                paymentIntent = (PaymentIntent) stripeObject;
                log.info("PaymentIntent Succeeded via Webhook: ID={}, OrderID (metadata)={}, UserID (metadata)={}",
                        paymentIntent.getId(),
                        paymentIntent.getMetadata().get("order_id"),
                        paymentIntent.getMetadata().get("user_id"));

                PaymentSucceededEventDto successEvent = PaymentSucceededEventDto.builder()
                        .eventId(UUID.randomUUID())
                        .eventTimestamp(LocalDateTime.now())
                        .orderId(UUID.fromString(paymentIntent.getMetadata().get("order_id")))
                        .userId(paymentIntent.getMetadata().get("user_id"))
                        .paymentIntentId(paymentIntent.getId())
                        .chargeId(paymentIntent.getLatestCharge())
                        .amountPaid(paymentIntent.getAmountReceived())
                        .currency(paymentIntent.getCurrency())
                        .paymentMethodType(paymentIntent.getPaymentMethodTypes().isEmpty() ? null : paymentIntent.getPaymentMethodTypes().get(0))
                        .paidAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(event.getCreated()), ZoneOffset.UTC)) // Use event.getCreated for webhook timestamp
                        .build();
                publishPaymentSucceededEvent(successEvent);
                break;

            case "payment_intent.payment_failed":
                paymentIntent = (PaymentIntent) stripeObject;
                String failureMessage = paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getMessage() : "Unknown failure";
                String failureCode = paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getCode() : null;
                log.warn("PaymentIntent Failed via Webhook: ID={}, OrderID (metadata)={}, UserID (metadata)={}, Reason: '{}'",
                        paymentIntent.getId(),
                        paymentIntent.getMetadata().get("order_id"),
                        paymentIntent.getMetadata().get("user_id"),
                        failureMessage);

                PaymentFailedEventDto failedEvent = PaymentFailedEventDto.builder()
                        .eventId(UUID.randomUUID())
                        .eventTimestamp(LocalDateTime.now())
                        .orderId(UUID.fromString(paymentIntent.getMetadata().get("order_id")))
                        .userId(paymentIntent.getMetadata().get("user_id"))
                        .paymentIntentId(paymentIntent.getId())
                        .failureCode(failureCode)
                        .failureMessage(failureMessage)
                        .failedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(event.getCreated()), ZoneOffset.UTC)) // Use event.getCreated
                        .build();
                publishPaymentFailedEvent(failedEvent);
                break;

            // Listen for refund updates from Stripe if needed, e.g., 'charge.refunded'
            // case "charge.refunded":
            //    Charge charge = (Charge) stripeObject;
            //    log.info("Charge refunded event received from Stripe: {}", charge.getId());
            //    // This can be used for reconciliation or if refunds are initiated outside your system
            //    break;

            default:
                log.info("Received unhandled Stripe event type via Webhook: {}", event.getType());
        }
    }

    // --- NEW METHOD IMPLEMENTATION ---
    @Override
    @Transactional // If you have local DB updates related to refunds
    public void processRefundRequest(RefundRequestedEventDto event) throws StripeException {
        log.info("Processing refund request for Order ID: {}, PaymentIntent: {}, Amount: {} {}",
                event.getOrderId(), event.getPaymentTransactionRef(), event.getAmountToRefund(), event.getCurrency());


        long amountToRefundInSmallestUnit = getAmountToRefundInSmallestUnit(event);

        RefundCreateParams.Builder refundParamsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(event.getPaymentTransactionRef());

        // Only set amount if you want a partial refund.
        // If event.getAmountToRefund() represents the full amount or if Stripe should refund the full available amount,
        // you might not need to set the amount explicitly, or ensure it matches.
        // For explicit partial/full refund based on event:
        if (event.getAmountToRefund() != null && event.getAmountToRefund().compareTo(BigDecimal.ZERO) > 0) {
            refundParamsBuilder.setAmount(amountToRefundInSmallestUnit);
        }
        // You can also add a reason or metadata to the refund if needed
        // refundParamsBuilder.putMetadata("reason", event.getReason());
        // refundParamsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER); // Or other Stripe reason codes

        try {
            Refund refund = Refund.create(refundParamsBuilder.build());
            log.info("Stripe refund successful: RefundID={}, PaymentIntentID={}, Status={}, AmountRefunded={}",
                    refund.getId(), refund.getPaymentIntent(), refund.getStatus(), refund.getAmount());

            RefundSucceededEventDto refundSucceededEvent = RefundSucceededEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .eventTimestamp(LocalDateTime.now())
                    .orderId(event.getOrderId())
                    .buyerId(event.getBuyerId())
                    .paymentIntentId(refund.getPaymentIntent())
                    .refundId(refund.getId())
                    .amountRefunded(refund.getAmount()) // This is Long, in smallest currency unit
                    .currency(refund.getCurrency())
                    .status(refund.getStatus()) // e.g., "succeeded"
                    .refundedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(refund.getCreated()), ZoneOffset.UTC))
                    .build();
            publishRefundSucceededEvent(refundSucceededEvent);

        } catch (StripeException e) {
            log.error("StripeException during refund for Order ID {}: {}", event.getOrderId(), e.getMessage());
            RefundFailedEventDto refundFailedEvent = RefundFailedEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .eventTimestamp(LocalDateTime.now())
                    .orderId(event.getOrderId())
                    .buyerId(event.getBuyerId())
                    .paymentIntentId(event.getPaymentTransactionRef())
                    .failureReason(e.getMessage())
                    .failureCode(e.getCode())
                    .build();
            publishRefundFailedEvent(refundFailedEvent);
            throw e; // Re-throw to allow listener to handle AMQP aspects if needed
        }
    }

    private static long getAmountToRefundInSmallestUnit(RefundRequestedEventDto event) {
        long amountToRefundInSmallestUnit;
        if ("vnd".equalsIgnoreCase(event.getCurrency())) {
            // VND has 0 decimal places in Stripe. Amount is as is.
            amountToRefundInSmallestUnit = event.getAmountToRefund().setScale(0, RoundingMode.HALF_UP).longValueExact();
        } else {
            // For currencies with 2 decimal places like USD, EUR
            amountToRefundInSmallestUnit = event.getAmountToRefund().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        return amountToRefundInSmallestUnit;
    }

    // --- Helper methods to publish events (add new ones for refund outcomes) ---
    private void publishPaymentSucceededEvent(PaymentSucceededEventDto event) {
        log.info("Publishing PaymentSucceededEvent for Order ID: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                RabbitMqConfig.PAYMENT_EVENT_SUCCEEDED_ROUTING_KEY,
                event);
    }

    private void publishPaymentFailedEvent(PaymentFailedEventDto event) {
        log.info("Publishing PaymentFailedEvent for Order ID: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                RabbitMqConfig.PAYMENT_EVENT_FAILED_ROUTING_KEY,
                event);
    }

    // --- NEW Helper methods for publishing refund events ---
    private void publishRefundSucceededEvent(RefundSucceededEventDto event) {
        log.info("Publishing RefundSucceededEvent for Order ID: {}, RefundID: {}", event.getOrderId(), event.getRefundId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                RabbitMqConfig.PAYMENT_EVENT_REFUND_SUCCEEDED_ROUTING_KEY, // Use the new routing key
                event);
    }

    private void publishRefundFailedEvent(RefundFailedEventDto event) {
        log.info("Publishing RefundFailedEvent for Order ID: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.PAYMENTS_EVENTS_EXCHANGE,
                RabbitMqConfig.PAYMENT_EVENT_REFUND_FAILED_ROUTING_KEY, // Use the new routing key
                event);
    }
}