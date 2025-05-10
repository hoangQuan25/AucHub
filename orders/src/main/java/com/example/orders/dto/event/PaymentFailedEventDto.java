package com.example.orders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId;
    private String userId;
    private String paymentIntentId; // Stripe's Payment Intent ID
    private String failureCode;     // Stripe's error code (e.g., "card_declined")
    private String failureMessage;  // Stripe's error message
    private LocalDateTime failedAt;      // Timestamp of failure confirmation
}
