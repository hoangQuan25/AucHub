package com.example.payments.service; // In your Payment Service

import com.example.payments.dto.request.ConfirmStripePaymentMethodRequestDto;
import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.request.CreateStripeSetupIntentRequestDto;
import com.example.payments.dto.request.RefundRequestedEventDto;
import com.example.payments.dto.response.CreatePaymentIntentResponseDto;
import com.example.payments.dto.response.CreateStripeSetupIntentResponseDto;
import com.example.payments.dto.response.StripePaymentMethodDetailsDto;
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

    /**
     * Processes a refund request for a specific order.
     * @param event The event containing details about the refund request.
     * @throws StripeException if there's an error communicating with Stripe.
     */
    void processRefundRequest(RefundRequestedEventDto event) throws StripeException;

    /**
     * Creates a Stripe SetupIntent to set up a payment method for future use.
     * Associates it with a Stripe Customer (creates one if necessary).
     *
     * @param requestDto Contains userId and optional existing stripeCustomerId.
     * @return DTO containing client_secret for the SetupIntent and stripeCustomerId.
     */
    CreateStripeSetupIntentResponseDto createStripeSetupIntent(CreateStripeSetupIntentRequestDto requestDto) throws StripeException;

    /**
     * Confirms a Stripe PaymentMethod (obtained from a SetupIntent on the frontend),
     * attaches it to the Stripe Customer, optionally sets it as default,
     * and returns its details.
     *
     * @param requestDto Contains userId, stripePaymentMethodId, and optional stripeCustomerId.
     * @return DTO containing confirmed Stripe Customer ID, PaymentMethod ID, and card details.
     */
    StripePaymentMethodDetailsDto confirmAndSavePaymentMethod(ConfirmStripePaymentMethodRequestDto requestDto) throws StripeException;
}