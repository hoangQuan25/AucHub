package com.example.users.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePaymentMethodDetailsClientDto {
    private String stripeCustomerId;
    private String stripePaymentMethodId;
    private String cardBrand;
    private String last4;
    private String expiryMonth;
    private String expiryYear;
    private boolean isDefaultSource;
}