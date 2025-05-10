package com.example.timedauctions.event;

import com.example.timedauctions.entity.AuctionStatus;
import jakarta.validation.constraints.NotNull; // Use jakarta validation
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

// Container class or individual records/classes
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

    @Value
    @Builder
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

    /** Event published when a user's max bid is surpassed by another user. */
    @Value
    @Builder
    public static class OutbidEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull String outbidUserId; // The user who was just outbid
        @NotNull BigDecimal newCurrentBid; // The new visible bid amount
        @NotNull String newHighestBidderId;
        @NotNull String newHighestBidderUsernameSnapshot;
    }

    /** Event published when a comment receives a reply. */
    @Value
    @Builder
    public static class CommentReplyEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull Long parentCommentId;
        @NotNull String originalCommenterId; // User ID of the parent comment's author
        @NotNull Long replyCommentId;
        @NotNull String replierUserId;
        @NotNull String replierUsernameSnapshot;
        @NotNull String replyCommentTextSample; // A snippet of the reply text
    }
}