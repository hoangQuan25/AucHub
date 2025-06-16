package com.example.notifications.service;

import com.example.notifications.dto.FollowingAuctionSummaryDto;
import com.example.notifications.dto.NotificationDto;
import com.example.notifications.entity.AuctionStatus;
import com.example.notifications.event.NotificationEvents;
import com.example.notifications.event.NotificationEvents.*; // Import event types
import com.example.notifications.event.DeliveryEvents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationService {

    void processAuctionStarted(AuctionStartedEvent event, String auctionType);

    void processAuctionEnded(AuctionEndedEvent event, String auctionType);


    void processOutbid(OutbidEvent event, String auctionType);


    void processCommentReply(CommentReplyEvent event, String auctionType);

    void processOrderCreated(OrderCreatedEvent event);
    void processOrderPaymentDue(OrderCreatedEvent event);
    void processUserPaymentDefaulted(UserPaymentDefaultedEvent event);
    void processSellerDecisionRequired(SellerDecisionRequiredEvent event);
    void processOrderReadyForShipping(OrderReadyForShippingEvent event);
    void processOrderCancelled(OrderCancelledEvent event);

    void processRefundSucceeded(RefundSucceededEvent event);
    void processRefundFailed(RefundFailedEvent event);
    void processOrderAwaitingFulfillmentConfirmation(OrderAwaitingFulfillmentConfirmationEvent event);

    void processDeliveryCreated(DeliveryEvents.DeliveryCreatedEventDto event);
    void processDeliveryShipped(DeliveryEvents.DeliveryShippedEventDto event);
    void processDeliveryDelivered(DeliveryEvents.DeliveryDeliveredEventDto event);
    void processDeliveryAwaitingBuyerConfirmation(DeliveryEvents.DeliveryAwaitingBuyerConfirmationEventDto event);
    void processDeliveryIssueReported(DeliveryEvents.DeliveryIssueReportedEventDto event);

    void processUserBanned(NotificationEvents.UserBannedEvent event);

    void processDeliveryReturnRequested(DeliveryEvents.DeliveryReturnRequestedEventDto event);

    void processDeliveryReturnApproved(DeliveryEvents.DeliveryReturnApprovedEventDto event);


    Page<NotificationDto> getUserNotifications(String userId, Pageable pageable);


    long getUnreadNotificationCount(String userId);


    int markNotificationsAsRead(String userId, List<Long> notificationIds);

    int markAllNotificationsAsRead(String userId);

    void followAuction(String userId, UUID auctionId, String auctionType);

    void unfollowAuction(String userId, UUID auctionId);

    Set<UUID> getFollowedAuctionIds(String userId);

    List<String> getFollowersForAuction(UUID auctionId);

    Page<FollowingAuctionSummaryDto> getFollowingAuctions(
            String userId,
            AuctionStatus status, // Can be null
            Boolean ended,      // Can be null
            Set<Long> categoryIds, // Can be null or empty
            LocalDateTime from, // Can be null
            Pageable pageable
    );

}