package com.example.users.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStripeSetupIntentRequestClientDto {
    private String userId;
    private String userEmail; // <<< ADD
    private String userName;  // <<< ADD
    private String stripeCustomerId;
}