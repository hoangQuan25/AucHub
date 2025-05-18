package com.example.payments.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmStripePaymentMethodRequestDto {
    @NotBlank(message = "User ID is required.")
    private String userId;

    @NotBlank(message = "Stripe PaymentMethod ID is required.")
    private String stripePaymentMethodId; // The pm_xxx ID from Stripe.js after setup

    private String stripeCustomerId; // Optional: if known and should be used/verified
}