package com.example.notifications.service;

import com.example.notifications.dto.NotificationDto;
import com.example.notifications.event.NotificationEvents.*; // Import event types
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

}