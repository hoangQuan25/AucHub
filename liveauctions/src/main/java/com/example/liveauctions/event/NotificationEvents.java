package com.example.liveauctions.event; // Or shared package

import com.example.liveauctions.entity.AuctionStatus; // Use LiveAuction's status enum
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set; // Make sure Set is imported
import java.util.UUID;

public final class NotificationEvents {

    @Value @Builder
    public static class AuctionStartedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull String auctionType; // "LIVE" or "TIMED"
        @NotNull String sellerId;
        LocalDateTime startTime;
        LocalDateTime endTime;
    }

    @Value @Builder
    public static class AuctionEndedEvent {
        @NotNull UUID eventId;
        @NotNull LocalDateTime eventTimestamp;

        @NotNull UUID auctionId;
        @NotNull Long productId;
        @NotNull String productTitleSnapshot;
        String productImageUrlSnapshot;

        @NotNull String auctionType; // "LIVE" or "TIMED"

        @NotNull String sellerId;
        String sellerUsernameSnapshot;

        @NotNull AuctionStatus finalStatus;
        LocalDateTime actualEndTime;

        // --- Winner & Bid Info (populated if status is SOLD) ---
        String winnerId;
        String winnerUsernameSnapshot; // RE-ADDED
        BigDecimal winningBid;

        // --- Reserve Price & Next Bidder Info (populated if status is SOLD and applicable) ---
        BigDecimal reservePrice;

        String secondHighestBidderId;
        String secondHighestBidderUsernameSnapshot; // RE-ADDED
        BigDecimal secondHighestBidAmount;

        String thirdHighestBidderId;
        String thirdHighestBidderUsernameSnapshot; // RE-ADDED
        BigDecimal thirdHighestBidAmount;
    }


}