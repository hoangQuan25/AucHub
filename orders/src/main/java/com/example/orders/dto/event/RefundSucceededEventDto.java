// Create this file in: com.example.orders.dto.event.RefundSucceededEventDto.java
package com.example.orders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundSucceededEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String buyerId;
    private String paymentIntentId;
    private String refundId; // Stripe's refund ID, e.g., "re_..."
    private Long amountRefunded; // Amount in smallest currency unit (e.g., cents, or base for VND)
    private String currency;
    private String status; // e.g., "succeeded"
    private LocalDateTime refundedAt;
}