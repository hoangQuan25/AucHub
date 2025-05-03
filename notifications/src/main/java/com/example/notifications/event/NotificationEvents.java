package com.example.notifications.event;

// Assuming AuctionStatus enum is also available (copy or use shared module)
import com.example.notifications.entity.AuctionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value; // Or use records if preferred and Java version allows easily

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public final class NotificationEvents {

    /** Event published when a timed auction reaches a final state. */
    @Value // Immutable DTO
    @Builder
    public static class AuctionEndedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull AuctionStatus finalStatus; // SOLD, RESERVE_NOT_MET, CANCELLED
        LocalDateTime actualEndTime;
        @NotNull String sellerId;
        // Winner info (nullable)
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
        @NotNull BigDecimal newCurrentBid;
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
        @NotNull String originalCommenterId; // User ID of the parent comment author
        @NotNull Long replyCommentId;
        @NotNull String replierUserId;
        @NotNull String replierUsernameSnapshot;
        @NotNull String replyCommentTextSample; // Snippet of the reply text
    }
}