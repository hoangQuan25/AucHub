package com.example.payments.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email; // For email validation
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStripeSetupIntentRequestDto {
    @NotBlank(message = "User ID is required.")
    private String userId;

    // User details for creating/updating Stripe Customer
    @Email(message = "User email must be a valid email address.") // Optional if customer already exists and email is not being updated
    private String userEmail;

    private String userName; // Optional: e.g., "FirstName LastName"

    private String stripeCustomerId; // Optional: if already known
}