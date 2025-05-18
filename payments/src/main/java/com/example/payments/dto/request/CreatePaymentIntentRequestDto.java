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

    private String description;


    // Optional fields you might want to pass to Stripe
    private String stripeCustomerId;           // cus_xxx (if you want to charge default or a specific PM of this customer)
    private String stripePaymentMethodId;      // pm_xxx (if you want to charge this specific saved payment method)
    private Boolean confirmImmediately;        // Default to false. If true, backend attempts to confirm PI immediately.
    // If true & off_session=true, useful for subscriptions/auto-charges.
    // If true & on_session, Stripe might still require frontend action for SCA.
    private Boolean offSession;

    private String returnUrl; // <<< NEW FIELD: URL to redirect to after off-site authentication
}