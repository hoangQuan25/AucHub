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

    /** Event published when a timed auction reaches a final state. */
    @Value // Makes immutable with constructor, getters, equals/hashCode/toString
    @Builder
    public static class AuctionEndedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot; // For context in notification
        @NotNull AuctionStatus finalStatus;    // SOLD, RESERVE_NOT_MET, CANCELLED
        LocalDateTime actualEndTime;
        @NotNull String sellerId;
        // Winner info (only populated if status is SOLD)
        String winnerId;
        String winnerUsernameSnapshot;
        BigDecimal winningBid;
        Set<String> allBidderIds;
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