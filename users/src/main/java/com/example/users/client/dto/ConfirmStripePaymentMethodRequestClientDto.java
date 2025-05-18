package com.example.users.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmStripePaymentMethodRequestClientDto {
    private String userId;
    private String stripePaymentMethodId; // The pm_xxx ID from Stripe.js
    private String stripeCustomerId; // Optional: if UsersService has one and wants PaymentsService to use/verify
}