package com.example.payments.controller; // In your Payment Service

import com.example.payments.service.PaymentService; // Your PaymentService interface
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks") // A common base path for webhooks
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/stripe") // This is the endpoint URL you'll register with Stripe
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload, // Raw request body as string
            @RequestHeader("Stripe-Signature") String sigHeader) { // Stripe signature header

        log.info("Received Stripe webhook event.");

        try {
            // Pass to service layer for verification and processing
            paymentService.handleStripeWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook received and processed successfully.");
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed.");
        } catch (StripeException e) {
            log.error("Stripe error while processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing Stripe event.");
        } catch (Exception e) {
            log.error("Unexpected error while processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error processing webhook.");
        }
    }
}