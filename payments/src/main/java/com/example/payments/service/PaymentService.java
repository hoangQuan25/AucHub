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

    void handleStripeWebhookEvent(String payload, String sigHeader) throws StripeException;

    CreatePaymentIntentResponseDto createPaymentIntent(CreatePaymentIntentRequestDto requestDto) throws StripeException;

    void processRefundRequest(RefundRequestedEventDto event) throws StripeException;

    CreateStripeSetupIntentResponseDto createStripeSetupIntent(CreateStripeSetupIntentRequestDto requestDto) throws StripeException;

    StripePaymentMethodDetailsDto confirmAndSavePaymentMethod(ConfirmStripePaymentMethodRequestDto requestDto) throws StripeException;
}