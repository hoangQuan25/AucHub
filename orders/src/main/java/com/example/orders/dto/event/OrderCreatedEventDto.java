package com.example.orders.dto.event; // Suggested package

import com.example.orders.entity.OrderStatus; // Assuming OrderStatus is in this path
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
public class OrderCreatedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId;
    private UUID auctionId;
    private Long productId;
    private String productTitleSnapshot;

    private String sellerId;
    private String currentBidderId; // Initially the winner
    private String currentBidderUsernameSnapshot;

    private BigDecimal amountDue;
    private String currency;
    private LocalDateTime paymentDeadline;

    private OrderStatus initialOrderStatus; // e.g., AWAITING_WINNER_PAYMENT
    private String auctionType; // e.g., LIVE, TIMED
}