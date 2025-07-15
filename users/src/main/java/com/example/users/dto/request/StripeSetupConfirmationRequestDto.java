package com.example.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StripeSetupConfirmationRequestDto {
    @NotBlank(message = "Stripe PaymentMethod ID is required.")
    private String stripePaymentMethodId;
}