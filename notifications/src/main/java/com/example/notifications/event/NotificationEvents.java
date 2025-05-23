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

    @Value @Builder
    public static class AuctionStartedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull String auctionType; // "LIVE" or "TIMED"
        @NotNull String sellerId;
        LocalDateTime startTime;
        LocalDateTime endTime;
    }

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
    }

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

    @Value
    @Builder
    public static class OrderCreatedEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        UUID auctionId;
        Long productId; // Corrected to Long
        String productTitleSnapshot;
        String sellerId;
        String currentBidderId;
        String currentBidderUsernameSnapshot; // Added field
        BigDecimal amountDue;
        String currency;
        LocalDateTime paymentDeadline;
        String initialOrderStatus; // As String
        String auctionType;
    }

    @Value
    @Builder
    public static class UserPaymentDefaultedEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        String defaultedUserId;
        UUID orderId;
        UUID auctionId;
        BigDecimal amountDefaulted;
        String currency;
        int paymentOfferAttempt;
    }

    @Value
    @Builder
    public static class SellerDecisionRequiredEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        UUID auctionId;
        Long productId; // Corrected to Long
        String productTitleSnapshot;
        String sellerId;
        String defaultedBidderId;
        BigDecimal defaultedBidAmount;
        boolean canOfferToSecondBidder;
        String eligibleSecondBidderId;
        BigDecimal eligibleSecondBidAmount;
        int paymentOfferAttemptOfDefaultingBidder;
    }

    @Value
    @Builder
    public static class OrderReadyForShippingEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        UUID auctionId;
        Long productId; // Corrected to Long
        String productTitleSnapshot;
        String sellerId;
        String buyerId;
        Long amountPaid; // Corrected to Long (smallest currency unit)
        String currency;
        String paymentTransactionRef;
    }

    @Value
    @Builder
    public static class OrderCancelledEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        UUID auctionId;
        Long productId; // Corrected to Long
        String sellerId;
        String currentBidderIdAtCancellation;
        String finalOrderStatus; // As String, from OrderStatus.toString() or similar
        String cancellationReason;
    }

    @Value
    @Builder
    public static class RefundSucceededEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        String buyerId; // Crucial for targeting the notification
        String paymentIntentId;
        String refundId; // Stripe's Refund ID
        Long amountRefunded; // Amount in smallest currency unit
        String currency;
        String status; // e.g., "succeeded"
        LocalDateTime refundedAt;
    }

    @Value
    @Builder
    public static class RefundFailedEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        String buyerId; // Crucial for targeting the notification
        String paymentIntentId;
        String failureReason;
        String failureCode; // Optional
    }

    @Value
    @Builder
    public static class OrderAwaitingFulfillmentConfirmationEvent {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID orderId;
        String sellerId;
        String buyerId; // Or buyerUsernameSnapshot if available and preferred
        String productTitleSnapshot;
    }
}