package com.example.payments.dto.request; // In your Payments Service

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
    private UUID eventId; // Event ID from the publisher (OrdersService)
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String buyerId; // The ID of the user requesting the refund
    private String paymentTransactionRef; // Stripe PaymentIntent ID
    private BigDecimal amountToRefund;    // The amount requested for refund
    private String currency;
    private String reason;
}