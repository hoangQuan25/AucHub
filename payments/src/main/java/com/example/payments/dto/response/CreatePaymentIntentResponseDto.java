package com.example.payments.dto.response;// package com.example.payments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentIntentResponseDto {
    private String paymentIntentId; // Stripe's Payment Intent ID (pi_...)
    private String clientSecret;    // The secret needed by Stripe.js on the frontend
    private String status;          // The initial status of the PaymentIntent (e.g., "requires_payment_method")
}