package com.example.users.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPaymentDefaultedEventDto { // Mirror the DTO from Orders service
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private String defaultedUserId;
    private UUID orderId;
    private UUID auctionId;
    private BigDecimal amountDefaulted;
    private String currency;
    private int paymentOfferAttempt;
}