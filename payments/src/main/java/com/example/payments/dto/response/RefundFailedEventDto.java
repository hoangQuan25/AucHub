package com.example.payments.dto.response;

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
public class RefundFailedEventDto {
    private UUID eventId; // New event ID
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String buyerId; // The ID of the user who requested the refund
    private String paymentIntentId; // Original Stripe PaymentIntent ID
    private String failureReason;
    private String failureCode; // Optional: if Stripe provides a specific code
}
