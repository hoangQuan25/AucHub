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
public class UserPaymentDefaultedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private String defaultedUserId; // The ID of the user who failed to pay
    private UUID orderId;
    private UUID auctionId; // For context
    private BigDecimal amountDefaulted; // The amount they were supposed to pay
    private String currency; // e.g., "VND"
    private int paymentOfferAttempt; // Which attempt this was for the order (1, 2, or 3)
}