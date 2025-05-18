package com.example.payments.controller; // In your Payment Service

import com.example.payments.dto.request.ConfirmStripePaymentMethodRequestDto;
import com.example.payments.dto.request.CreatePaymentIntentRequestDto;
import com.example.payments.dto.request.CreateStripeSetupIntentRequestDto;
import com.example.payments.dto.response.CreatePaymentIntentResponseDto;
import com.example.payments.dto.response.CreateStripeSetupIntentResponseDto;
import com.example.payments.dto.response.StripePaymentMethodDetailsDto;
import com.example.payments.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(@Valid @RequestBody CreatePaymentIntentRequestDto requestDto) {
        log.info("Received request to create payment intent for order ID: {}", requestDto.getOrderId());
        try {
            CreatePaymentIntentResponseDto response = paymentService.createPaymentIntent(requestDto);
            log.info("PaymentIntent {} created successfully for order ID: {}", response.getPaymentIntentId(), requestDto.getOrderId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while creating PaymentIntent for order {}: {}", requestDto.getOrderId(), e.getMessage());
            // Provide a user-friendly error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating payment intent: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating PaymentIntent for order {}: {}", requestDto.getOrderId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during payment intent creation.");
        }
    }

    @PostMapping("/setup-intent")
    public ResponseEntity<?> createStripeSetupIntent(@Valid @RequestBody CreateStripeSetupIntentRequestDto requestDto) {
        log.info("Received request to create Stripe SetupIntent for user ID: {}", requestDto.getUserId());
        try {
            CreateStripeSetupIntentResponseDto response = paymentService.createStripeSetupIntent(requestDto);
            log.info("Stripe SetupIntent {} created successfully for user ID: {}, Stripe Customer ID: {}",
                    response.getSetupIntentId(), requestDto.getUserId(), response.getStripeCustomerId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while creating SetupIntent for user {}: {}", requestDto.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating Stripe SetupIntent: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating SetupIntent for user {}: {}", requestDto.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during SetupIntent creation.");
        }
    }

    // --- NEW ENDPOINT for confirming a PaymentMethod after SetupIntent completion ---
    @PostMapping("/confirm-payment-method")
    public ResponseEntity<?> confirmAndSaveStripePaymentMethod(@Valid @RequestBody ConfirmStripePaymentMethodRequestDto requestDto) {
        log.info("Received request to confirm Stripe PaymentMethod ID: {} for user ID: {}",
                requestDto.getStripePaymentMethodId(), requestDto.getUserId());
        try {
            StripePaymentMethodDetailsDto response = paymentService.confirmAndSavePaymentMethod(requestDto);
            log.info("Successfully confirmed and processed PaymentMethod {} for Stripe Customer ID: {} (User ID: {})",
                    response.getStripePaymentMethodId(), response.getStripeCustomerId(), requestDto.getUserId());
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error while confirming PaymentMethod {} for user {}: {}",
                    requestDto.getStripePaymentMethodId(), requestDto.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming Stripe PaymentMethod: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while confirming PaymentMethod for user {}: {}", requestDto.getUserId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during PaymentMethod confirmation.");
        }
    }
}