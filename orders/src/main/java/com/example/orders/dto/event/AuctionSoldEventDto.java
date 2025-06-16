package com.example.orders.dto.event; // In your Orders service

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
public class AuctionSoldEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID auctionId;
    private Long productId;
    private String productTitleSnapshot;
    private String productImageUrlSnapshot; // Optional

    private String auctionType; // "LIVE" or "TIMED"

    private String sellerId;
    private String sellerUsernameSnapshot;

    private String finalStatus; // Expecting "SOLD" for order creation

    private LocalDateTime actualEndTime;

    // Winner info
    private String winnerId;
    private BigDecimal winningBid;
    // Currency is implicitly VND

    private BigDecimal reservePrice;

    private String secondHighestBidderId;
    private BigDecimal secondHighestBidAmount;

    private String thirdHighestBidderId;
    private BigDecimal thirdHighestBidAmount;
}