package com.example.orders.dto.event;

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
public class RefundRequestedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String buyerId;
    private String paymentTransactionRef; // e.g., Stripe PaymentIntent ID from the original payment
    private BigDecimal amountToRefund;
    private String currency;
    private String reason; // Reason for the refund
}