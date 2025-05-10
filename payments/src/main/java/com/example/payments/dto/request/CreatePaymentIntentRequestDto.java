package com.example.payments.dto.request;// package com.example.payments.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentIntentRequestDto {

    @NotNull(message = "Order ID cannot be null")
    private UUID orderId;

    @NotNull(message = "User ID cannot be null")
    @NotEmpty(message = "User ID cannot be empty")
    private String userId; // The ID of the user making the payment

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "1", message = "Amount must be positive") // Smallest unit (e.g., 1 Dong)
    private Long amount; // Amount in smallest currency unit (e.g., Dong for VND)

    @NotNull(message = "Currency cannot be null")
    @NotEmpty(message = "Currency cannot be empty")
    private String currency; // e.g., "vnd" (lowercase for Stripe)

    // Optional fields you might want to pass to Stripe
    private String description; // e.g., "Payment for Order #123 - Auction Item XYZ"
    private String customerEmail; // Useful for guest checkouts or Stripe receipts
}