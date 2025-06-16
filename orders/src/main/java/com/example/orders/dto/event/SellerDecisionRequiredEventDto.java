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
public class SellerDecisionRequiredEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId;
    private UUID auctionId;
    private Long productId;
    private String productTitleSnapshot;
    private String sellerId; // The seller who needs to make a decision

    private String defaultedBidderId; // The winner who timed out
    private BigDecimal defaultedBidAmount; // The amount the winner was supposed to pay

    // Information about potential next steps
    private boolean canOfferToSecondBidder;
    private String eligibleSecondBidderId; // Populated if canOfferToSecondBidder is true
    private BigDecimal eligibleSecondBidAmount; // Populated if canOfferToSecondBidder is true

    private int paymentOfferAttemptOfDefaultingBidder; // Should be 1 in this scenario
}