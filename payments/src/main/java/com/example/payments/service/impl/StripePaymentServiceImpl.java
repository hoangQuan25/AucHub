package com.example.payments.service.impl;

import com.example.payments.config.RabbitMqConfig;
import com.example.payments.config.StripeConfig;
import com.example.payments.dto.event.PaymentFailedEventDto;
import com.example.payments.dto.event.PaymentSucceededEventDto;
import com.example.payments.dto.request.ConfirmStripePaymentMethodRequestDto;
import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.request.CreateStripeSetupIntentRequestDto;
import com.example.payments.dto.request.RefundRequestedEventDto;
import com.example.payments.dto.response.*;
// Assuming you have a PaymentIntentRecord entity and repository
// import com.example.payments.entity.PaymentIntentRecord;
// import com.example.payments.repository.PaymentIntentRecordRepository;
import com.example.payments.service.PaymentService;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook; // For signature verification
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // If updating local DB
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentServiceImpl implements PaymentService {

    private final StripeConfig stripeConfig; // To get the webhookSigningSecret
    private final RabbitTemplate rabbitTemplate;


    @Override
    @Transactional
    public CreatePaymentIntentResponseDto createPaymentIntent(CreatePaymentIntentRequestDto requestDto) throws StripeException {
        log.info("Creating PaymentIntent for orderId: {}, userId: {}, usingStripeCustomerId: {}, usingStripePaymentMethodId: {}",
                requestDto.getOrderId(), requestDto.getUserId(),
                requestDto.getStripeCustomerId() != null, requestDto.getStripePaymentMethodId() != null);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", requestDto.getOrderId().toString());
        metadata.put("user_id", requestDto.getUserId());
        if (StringUtils.hasText(requestDto.getDescription())) {
            metadata.put("description", requestDto.getDescription());
        }

        PaymentIntentCreateParams.Builder paramsBuilder =
                PaymentIntentCreateParams.builder()
                        .setAmount(requestDto.getAmount())
                        .setCurrency(requestDto.getCurrency().toLowerCase())
                        .putAllMetadata(metadata);

        // --- LOGIC TO USE SAVED PAYMENT METHOD ---
        if (StringUtils.hasText(requestDto.getStripeCustomerId())) {
            paramsBuilder.setCustomer(requestDto.getStripeCustomerId());
            log.debug("Setting customer for PaymentIntent: {}", requestDto.getStripeCustomerId());
        }

        if (StringUtils.hasText(requestDto.getStripePaymentMethodId())) {
            paramsBuilder.setPaymentMethod(requestDto.getStripePaymentMethodId());
            log.debug("Setting specific payment_method for PaymentIntent: {}", requestDto.getStripePaymentMethodId());
            // If using a specific payment method, it's usually good practice to also provide the customer it belongs to.
            if (!StringUtils.hasText(requestDto.getStripeCustomerId())) {
                log.warn("stripePaymentMethodId provided without stripeCustomerId. This might work for some cases but is generally not recommended.");
                // Stripe might infer customer if PM is uniquely attached, but explicitly providing customer is better.
            }
        }

        // Confirmation Logic
        if (Boolean.TRUE.equals(requestDto.getConfirmImmediately())) {
            paramsBuilder.setConfirm(true);
            log.debug("PaymentIntent will be attempted to confirm immediately.");

            if (!StringUtils.hasText(requestDto.getReturnUrl())) {

                log.warn("confirmImmediately is true, but no returnUrl provided in request for orderId: {}. " +
                        "Stripe might require it if redirection is needed.", requestDto.getOrderId());

            } else {
                paramsBuilder.setReturnUrl(requestDto.getReturnUrl()); // <<< SET THE RETURN URL
                log.debug("Setting return_url for PaymentIntent: {}", requestDto.getReturnUrl());
            }

            if (Boolean.TRUE.equals(requestDto.getOffSession()) && StringUtils.hasText(requestDto.getStripeCustomerId())) {
                paramsBuilder.setOffSession(true);
                if (!StringUtils.hasText(requestDto.getStripePaymentMethodId()) && StringUtils.hasText(requestDto.getStripeCustomerId())) {
                    log.debug("Attempting off-session confirm using customer's default payment method.");
                } else if (StringUtils.hasText(requestDto.getStripePaymentMethodId())) {
                    log.debug("Attempting off-session confirm using specific payment_method: {}", requestDto.getStripePaymentMethodId());
                } else {
                    log.warn("Off-session confirm requested but no customer or specific payment method ID provided. This will likely fail.");
                }
            }
        } else {

            if (!StringUtils.hasText(requestDto.getStripePaymentMethodId())) { // Only enable auto PM if not using a specific saved one already
                paramsBuilder.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build());
                log.debug("Automatic payment methods enabled for PaymentIntent.");
            }
        }


        PaymentIntentCreateParams params = paramsBuilder.build();
        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("PaymentIntent created/retrieved: ID={}, Status={}, ClientSecret relevant for FE",
                paymentIntent.getId(), paymentIntent.getStatus());

        return CreatePaymentIntentResponseDto.builder()
                .paymentIntentId(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret()) // Always return client_secret for frontend
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



            default:
                log.info("Received unhandled Stripe event type via Webhook: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void processRefundRequest(RefundRequestedEventDto event) throws StripeException {
        log.info("Processing refund request for Order ID: {}, PaymentIntent: {}, Amount: {} {}",
                event.getOrderId(), event.getPaymentTransactionRef(), event.getAmountToRefund(), event.getCurrency());


        long amountToRefundInSmallestUnit = getAmountToRefundInSmallestUnit(event);

        RefundCreateParams.Builder refundParamsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(event.getPaymentTransactionRef());


        if (event.getAmountToRefund() != null && event.getAmountToRefund().compareTo(BigDecimal.ZERO) > 0) {
            refundParamsBuilder.setAmount(amountToRefundInSmallestUnit);
        }


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

    @Override
    @Transactional // Not strictly needed if not writing to local DB here, but good practice
    public CreateStripeSetupIntentResponseDto createStripeSetupIntent(CreateStripeSetupIntentRequestDto requestDto) throws StripeException {
        log.info("Creating Stripe SetupIntent for userId: {}", requestDto.getUserId());
        String stripeCustomerId = requestDto.getStripeCustomerId();
        String userId = requestDto.getUserId();
        String userEmail = requestDto.getUserEmail();
        String userName = requestDto.getUserName();

        // 1. Get or Create Stripe Customer
        if (!StringUtils.hasText(stripeCustomerId)) {

            if (!StringUtils.hasText(stripeCustomerId)) {
                CustomerCreateParams.Builder customerParamsBuilder = CustomerCreateParams.builder()
                        .setDescription("Customer for user_id: " + userId)
                        .putMetadata("user_id", userId);

                if (StringUtils.hasText(userEmail)) {
                    customerParamsBuilder.setEmail(userEmail); // <<< USE THE PROVIDED EMAIL
                } else {
                    log.warn("User email not provided for Stripe Customer creation for userId: {}. This might be an issue.", userId);
                }
                if (StringUtils.hasText(userName)) {
                    customerParamsBuilder.setName(userName); // <<< USE THE PROVIDED NAME
                }

                Customer customer = Customer.create(customerParamsBuilder.build());
                stripeCustomerId = customer.getId();
                log.info("Created new Stripe Customer ID: {} for userId: {}", stripeCustomerId, requestDto.getUserId());
            }
        } else {
            try {
                Customer existingCustomer = Customer.retrieve(stripeCustomerId);
                CustomerUpdateParams.Builder updateParamsBuilder = CustomerUpdateParams.builder();
                boolean needsUpdate = false;
                if (StringUtils.hasText(userEmail) && !userEmail.equals(existingCustomer.getEmail())) {
                    updateParamsBuilder.setEmail(userEmail);
                    needsUpdate = true;
                }
                if (StringUtils.hasText(userName) && !userName.equals(existingCustomer.getName())) {
                    updateParamsBuilder.setName(userName);
                    needsUpdate = true;
                }
                if(!userId.equals(existingCustomer.getMetadata().get("user_id"))){
                    updateParamsBuilder.putMetadata("user_id", userId);
                    needsUpdate = true;
                }


                if (needsUpdate) {
                    existingCustomer.update(updateParamsBuilder.build());
                    log.info("Updated existing Stripe Customer ID: {} with new details for userId: {}", stripeCustomerId, userId);
                } else {
                    log.info("Using existing Stripe Customer ID: {} for userId: {}", stripeCustomerId, userId);
                }

            } catch (StripeException e) {
                log.error("Failed to retrieve or update existing Stripe Customer ID {}: {}. Check if ID is valid.", stripeCustomerId, e.getMessage());
                throw e;
            }
        }

        // 2. Create SetupIntent
        List<String> pmTypes = List.of("card");
        SetupIntentCreateParams setupIntentParams = SetupIntentCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .addAllPaymentMethodType(pmTypes)                  // accepts List<String>
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .putMetadata("user_id", requestDto.getUserId())
                .build();

        SetupIntent setupIntent = SetupIntent.create(setupIntentParams);

        log.info("Stripe SetupIntent created: ID={}, ClientSecret available.", setupIntent.getId());
        return CreateStripeSetupIntentResponseDto.builder()
                .clientSecret(setupIntent.getClientSecret())
                .stripeCustomerId(stripeCustomerId) // Return the customer ID used/created
                .setupIntentId(setupIntent.getId())
                .build();
    }

    @Override
    @Transactional
    public StripePaymentMethodDetailsDto confirmAndSavePaymentMethod(ConfirmStripePaymentMethodRequestDto requestDto) throws StripeException {
        log.info("Confirming payment method setup for userId: {}, PaymentMethodId: {}", requestDto.getUserId(), requestDto.getStripePaymentMethodId());
        String stripePaymentMethodId = requestDto.getStripePaymentMethodId();
        String stripeCustomerId = requestDto.getStripeCustomerId();

        // 1. Retrieve PaymentMethod details from Stripe to get card brand, last4, expiry
        PaymentMethod paymentMethod = PaymentMethod.retrieve(stripePaymentMethodId);
        if (paymentMethod.getCustomer() != null && StringUtils.hasText(stripeCustomerId) && !paymentMethod.getCustomer().equals(stripeCustomerId)) {
            log.warn("PaymentMethod {} is already attached to a different customer ({}). Expected {}. This might be an issue or require detaching and reattaching.",
                    stripePaymentMethodId, paymentMethod.getCustomer(), stripeCustomerId);

        }


        // 2. Ensure Stripe Customer exists (or use the one from the PM if it's already attached and matches)
        // If stripeCustomerId was not passed in request, or if paymentMethod isn't attached yet.
        if (!StringUtils.hasText(stripeCustomerId)) {
            if (paymentMethod.getCustomer() != null) {
                stripeCustomerId = paymentMethod.getCustomer();
                log.info("Using Stripe Customer ID {} from retrieved PaymentMethod {}", stripeCustomerId, stripePaymentMethodId);
            } else {
                // If no customer ID from request AND no customer attached to PM, create one.
                // This scenario should ideally be handled by ensuring customer is created during SetupIntent creation.
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setDescription("Customer for user_id: " + requestDto.getUserId())
                        .putMetadata("user_id", requestDto.getUserId())
//                         .setEmail(userEmail) // Ideally get user's email
                        .build();
                Customer customer = Customer.create(customerParams);
                stripeCustomerId = customer.getId();
                log.info("Created new Stripe Customer {} for userId {} during PM confirmation", stripeCustomerId, requestDto.getUserId());
            }
        }

        // 3. Attach PaymentMethod to Customer (idempotent - won't error if already attached to this customer)
        try {
            if (paymentMethod.getCustomer() == null || !paymentMethod.getCustomer().equals(stripeCustomerId)) {
                PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder().setCustomer(stripeCustomerId).build();
                paymentMethod = paymentMethod.attach(attachParams);
                log.info("Attached PaymentMethod {} to Customer {}", stripePaymentMethodId, stripeCustomerId);
            }
        } catch (StripeException e) {
            log.error("Failed to attach PaymentMethod {} to Customer {}: {}", stripePaymentMethodId, stripeCustomerId, e.getMessage());
            throw e; // Re-throw, UsersService needs to know attachment failed.
        }


        // 4. Optionally, update Customer to set this as the default payment method
        CustomerUpdateParams customerUpdateParams = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                        CustomerUpdateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod(stripePaymentMethodId)
                                .build())
                .build();
        Customer updatedCustomer = Customer.retrieve(stripeCustomerId).update(customerUpdateParams);
        log.info("Set PaymentMethod {} as default for Customer {}", stripePaymentMethodId, updatedCustomer.getId());


        // 5. Extract card details for returning to UsersService
        String cardBrand = null;
        String last4 = null;
        String expMonth = null;
        String expYear = null;
        boolean isDefault = (updatedCustomer.getInvoiceSettings() != null &&
                stripePaymentMethodId.equals(updatedCustomer.getInvoiceSettings().getDefaultPaymentMethod()));


        if (paymentMethod.getCard() != null) {
            cardBrand = paymentMethod.getCard().getBrand();
            last4 = paymentMethod.getCard().getLast4();
            expMonth = String.format("%02d", paymentMethod.getCard().getExpMonth()); // Ensure two digits
            expYear = String.valueOf(paymentMethod.getCard().getExpYear());
        } else {
            log.warn("PaymentMethod {} does not have card details. Type: {}", stripePaymentMethodId, paymentMethod.getType());
        }

        return StripePaymentMethodDetailsDto.builder()
                .stripeCustomerId(stripeCustomerId)
                .stripePaymentMethodId(stripePaymentMethodId)
                .cardBrand(cardBrand)
                .last4(last4)
                .expiryMonth(expMonth)
                .expiryYear(expYear)
                .isDefaultSource(isDefault)
                .build();
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

    // --- Helper methods for publishing refund events ---
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