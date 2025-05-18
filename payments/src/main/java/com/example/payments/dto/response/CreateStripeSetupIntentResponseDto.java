package com.example.payments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStripeSetupIntentResponseDto {
    private String clientSecret;        // The client secret for the Stripe SetupIntent
    private String stripeCustomerId;    // The Stripe Customer ID (either existing or newly created)
    private String setupIntentId;       // The ID of the SetupIntent itself
}