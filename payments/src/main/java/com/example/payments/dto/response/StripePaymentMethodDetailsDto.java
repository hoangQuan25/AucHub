package com.example.payments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePaymentMethodDetailsDto {
    private String stripeCustomerId;
    private String stripePaymentMethodId; // The confirmed and attached PaymentMethod ID
    private String cardBrand;
    private String last4;
    private String expiryMonth; // MM
    private String expiryYear;  // YYYY
    private boolean isDefaultSource; // Was this set as default in Stripe for the customer
}