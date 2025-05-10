package com.example.payments.service; // In your Payment Service

import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.response.CreatePaymentIntentResponseDto;
import com.stripe.exception.StripeException; // For Stripe-specific exceptions

public interface PaymentService {

    /**
     * Handles incoming webhook events from Stripe.
     * Verifies the event signature, processes known event types,
     * and publishes internal events accordingly.
     *
     * @param payload The raw JSON payload from Stripe.
     * @param sigHeader The value of the "Stripe-Signature" header.
     * @throws StripeException if signature verification fails or other Stripe errors occur.
     */
    void handleStripeWebhookEvent(String payload, String sigHeader) throws StripeException;

    /**
     * Creates a Stripe PaymentIntent for a given order.
     * @param requestDto Details needed to create the payment intent.
     * @return Response DTO containing the clientSecret for the frontend.
     * @throws StripeException if there's an error communicating with Stripe.
     */
    CreatePaymentIntentResponseDto createPaymentIntent(CreatePaymentIntentRequestDto requestDto) throws StripeException;

    // We will add methods later for handling webhook events, e.g.:
    // void handlePaymentIntentSucceeded(PaymentIntent paymentIntent);
    // void handlePaymentIntentFailed(PaymentIntent paymentIntent);
}