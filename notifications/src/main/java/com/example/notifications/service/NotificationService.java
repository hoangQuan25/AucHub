package com.example.notifications.service;

import com.example.notifications.dto.FollowingAuctionSummaryDto;
import com.example.notifications.dto.NotificationDto;
import com.example.notifications.entity.AuctionStatus;
import com.example.notifications.event.NotificationEvents.*; // Import event types
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationService {

    /**
     * Processes an auction ended event, determines recipients,
     * persists notifications, and sends real-time alerts.
     * @param event The AuctionEndedEvent received from RabbitMQ.
     */
    void processAuctionEnded(AuctionEndedEvent event);

    /**
     * Processes an outbid event, notifies the outbid user,
     * persists the notification, and sends a real-time alert.
     * @param event The OutbidEvent received from RabbitMQ.
     */
    void processOutbid(OutbidEvent event);

    /**
     * Processes a comment reply event, notifies the original commenter,
     * persists the notification, and sends a real-time alert.
     * @param event The CommentReplyEvent received from RabbitMQ.
     */
    void processCommentReply(CommentReplyEvent event);

    void processOrderCreated(OrderCreatedEvent event);
    void processOrderPaymentDue(OrderCreatedEvent event);
    void processUserPaymentDefaulted(UserPaymentDefaultedEvent event);
    void processSellerDecisionRequired(SellerDecisionRequiredEvent event);
    void processOrderReadyForShipping(OrderReadyForShippingEvent event);
    void processOrderCancelled(OrderCancelledEvent event);

    /**
     * Retrieves a paginated list of notifications for a specific user.
     * @param userId The ID of the user whose notifications are being fetched.
     * @param pageable Pagination and sorting information.
     * @return A Page containing NotificationDto objects.
     */
    Page<NotificationDto> getUserNotifications(String userId, Pageable pageable);

    /**
     * Gets the count of unread notifications for a user.
     * @param userId The ID of the user.
     * @return The number of unread notifications.
     */
    long getUnreadNotificationCount(String userId);

    /**
     * Marks a list of specific notifications as read for a user.
     * @param userId The ID of the user owning the notifications.
     * @param notificationIds A list of notification IDs to mark as read.
     * @return The number of notifications successfully marked as read.
     */
    int markNotificationsAsRead(String userId, List<Long> notificationIds);

    /**
     * Marks all unread notifications as read for a user.
     * @param userId The ID of the user.
     * @return The number of notifications successfully marked as read.
     */
    int markAllNotificationsAsRead(String userId);

    /** Adds a follow relationship */
    void followAuction(String userId, UUID auctionId, String auctionType);

    /** Removes a follow relationship */
    void unfollowAuction(String userId, UUID auctionId);

    /** Gets the set of auction IDs followed by a user */
    Set<UUID> getFollowedAuctionIds(String userId);

    /** Gets user IDs following a specific auction (used internally) */
    List<String> getFollowersForAuction(UUID auctionId);

    /** Processes an auction started event */
    void processAuctionStarted(AuctionStartedEvent event);

    /**
     * Retrieves details for auctions followed by a user, supporting filtering and pagination.
     * This orchestrates calls to auction services.
     */
    Page<FollowingAuctionSummaryDto> getFollowingAuctions(
            String userId,
            AuctionStatus status, // Can be null
            Boolean ended,      // Can be null
            Set<Long> categoryIds, // Can be null or empty
            LocalDateTime from, // Can be null
            Pageable pageable
    );

}