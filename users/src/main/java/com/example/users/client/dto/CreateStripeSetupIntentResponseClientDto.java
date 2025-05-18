package com.example.users.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStripeSetupIntentResponseClientDto {
    private String clientSecret;
    private String stripeCustomerId;
    private String setupIntentId;
}