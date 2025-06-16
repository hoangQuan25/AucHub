package com.example.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentMethodConfirmationResultDto {
    private String message;
    private String stripeCustomerId;
    private String defaultPaymentMethodId;
}
