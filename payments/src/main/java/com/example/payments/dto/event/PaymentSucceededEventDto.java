package com.example.payments.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSucceededEventDto {
    private UUID eventId; // Unique ID for this event
    private LocalDateTime eventTimestamp;

    private UUID orderId;     // Your internal order ID
    private String userId;      // Your internal user ID who paid
    private String paymentIntentId; // Stripe's Payment Intent ID
    private String chargeId;        // Stripe's Charge ID (if available, from the charge object on the PaymentIntent)
    private Long amountPaid;      // Amount in smallest currency unit
    private String currency;      // e.g., "vnd"
    private String paymentMethodType; // e.g., "card"
    private LocalDateTime paidAt;        // Timestamp of successful payment confirmation from Stripe
}
