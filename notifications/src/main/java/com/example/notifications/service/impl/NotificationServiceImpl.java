package com.example.notifications.service.impl;

import com.example.notifications.client.UserServiceClient;
import com.example.notifications.client.dto.UserBasicInfoDto; // Assuming this DTO is available
import com.example.notifications.dto.NotificationDto; // DTO for WebSocket payload
import com.example.notifications.entity.Notification; // DB Entity
import com.example.notifications.entity.AuctionStatus; // Enum for status check
import com.example.notifications.event.NotificationEvents.*; // Event types
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.repository.NotificationRepository; // DB Repo
import com.example.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate; // For WebSocket messages
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import if needed
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate; // For sending WebSocket messages
    private final UserServiceClient userServiceClient; // To get user info (like email for future use)
    private final NotificationMapper notificationMapper;
    // Optional: Inject Mail Sender if implementing email
    // private final JavaMailSender mailSender;

    // Define constants for notification types (match event names or use custom)
    private static final String TYPE_AUCTION_ENDED = "AUCTION_ENDED";
    private static final String TYPE_OUTBID = "AUCTION_OUTBID";
    private static final String TYPE_COMMENT_REPLY = "COMMENT_REPLY";


    @Override
    @Transactional // Persist notification atomically
    public void processAuctionEnded(AuctionEndedEvent event) {
        log.debug("Processing AuctionEndedEvent for auction {}", event.getAuctionId());

        Set<String> notifiedUserIds = new HashSet<>();
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        List<String> recipientIds = new ArrayList<>();
        String baseMessage = String.format("Auction for '%s' has ended with status: %s.",
                truncate(event.getProductTitleSnapshot(), 50), event.getFinalStatus());
        String message;

        // 1. Notify Seller
        if (event.getSellerId() != null) {
            recipientIds.add(event.getSellerId());
            message = baseMessage; // Generic message for seller
            if (event.getFinalStatus() == AuctionStatus.SOLD) {
                message += String.format(" Sold for %s VNĐ to %s.",
                        event.getWinningBid().toPlainString(), event.getWinnerUsernameSnapshot());
            } else if (event.getFinalStatus() == AuctionStatus.RESERVE_NOT_MET) {
                message += " The reserve price was not met.";
            }
            saveAndSendNotification(event.getSellerId(), TYPE_AUCTION_ENDED, message, event.getAuctionId(), null);
        }

        // 2. Notify Winner (if applicable and different from seller)
        if (event.getFinalStatus() == AuctionStatus.SOLD && event.getWinnerId() != null && !event.getWinnerId().equals(event.getSellerId())) {
            recipientIds.add(event.getWinnerId()); // Add winner only once
            message = String.format("Congratulations! You won the auction for '%s' with a bid of %s VNĐ.",
                    truncate(event.getProductTitleSnapshot(), 50), event.getWinningBid().toPlainString());
            saveAndSendNotification(event.getWinnerId(), TYPE_AUCTION_ENDED, message, event.getAuctionId(), null);
        }

        // 3. TODO: Notify other Bidders/Followers?
        // --- NEW: Notify Other Bidders ---
        if (event.getAllBidderIds() != null) {
            for (String bidderId : event.getAllBidderIds()) {
                // Send only if not already notified (i.e., not the seller or the winner)
                if (notifiedUserIds.add(bidderId)) {
                    // Format a generic message for losing bidders / participants
                    if (event.getFinalStatus() == AuctionStatus.SOLD) {
                        message = String.format("Auction for '%s' has ended. It was sold to %s for %s VNĐ.",
                                productTitle, event.getWinnerUsernameSnapshot(), event.getWinningBid().toPlainString());
                    } else if (event.getFinalStatus() == AuctionStatus.RESERVE_NOT_MET) {
                        message = String.format("Auction for '%s' has ended. The reserve price was not met.", productTitle);
                    } else { // CANCELLED
                        message = String.format("Auction for '%s' was cancelled.", productTitle);
                    }
                    saveAndSendNotification(bidderId, TYPE_AUCTION_ENDED, message, event.getAuctionId(), null);
                }
            }
        }
        // --- End Notify Other Bidders ---

        log.info("Processed AuctionEndedEvent for auction {}, notified {} users.", event.getAuctionId(), recipientIds.size());
    }


    @Override
    @Transactional
    public void processOutbid(OutbidEvent event) {
        log.debug("Processing OutbidEvent for auction {}, user {}", event.getAuctionId(), event.getOutbidUserId());

        String message = String.format("You have been outbid on '%s'! The current bid is now %s VNĐ by %s.",
                truncate(event.getProductTitleSnapshot(), 50),
                event.getNewCurrentBid().toPlainString(),
                event.getNewHighestBidderUsernameSnapshot());

        saveAndSendNotification(event.getOutbidUserId(), TYPE_OUTBID, message, event.getAuctionId(), null);
        log.info("Processed OutbidEvent for auction {}, notified user {}.", event.getAuctionId(), event.getOutbidUserId());
    }


    @Override
    @Transactional
    public void processCommentReply(CommentReplyEvent event) {
        log.debug("Processing CommentReplyEvent for auction {}, original commenter {}", event.getAuctionId(), event.getOriginalCommenterId());

        // Avoid notifying if user replies to themselves (already checked in publisher, but double check)
        if (event.getReplierUserId().equals(event.getOriginalCommenterId())) {
            log.debug("User replied to their own comment, skipping notification.");
            return;
        }

        String message = String.format("%s replied to your comment on '%s': '%s'",
                event.getReplierUsernameSnapshot(),
                truncate(event.getProductTitleSnapshot(), 40),
                event.getReplyCommentTextSample()); // Snippet from event

        saveAndSendNotification(event.getOriginalCommenterId(), TYPE_COMMENT_REPLY, message, event.getAuctionId(), event.getReplyCommentId());
        log.info("Processed CommentReplyEvent for auction {}, notified user {}.", event.getAuctionId(), event.getOriginalCommenterId());
    }

    /**
     * Retrieves a paginated list of notifications for a specific user.
     *
     * @param userId   The ID of the user whose notifications are being fetched.
     * @param pageable Pagination and sorting information.
     * @return A Page containing NotificationDto objects.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction
    public Page<NotificationDto> getUserNotifications(String userId, Pageable pageable) {
        log.debug("Service fetching notifications for user {} page: {}", userId, pageable);

        // Fetch the paginated list of Notification entities from the repository
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Map the Page<Notification> to Page<NotificationDto>
        // The Page.map function takes a lambda (Function) to convert each element
        return notificationPage.map(notificationMapper::mapEntityToDto); // Use a helper mapping function
    }

    /**
     * Gets the count of unread notifications for a user.
     *
     * @param userId The ID of the user.
     * @return The number of unread notifications.
     */
    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(String userId) {
        log.debug("Getting unread notification count for user {}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
        // Note: Could potentially optimize with a smaller query or caching if needed frequently
    }

    /**
     * Marks a list of specific notifications as read for a user.
     *
     * @param userId          The ID of the user owning the notifications.
     * @param notificationIds A list of notification IDs to mark as read.
     * @return The number of notifications successfully marked as read.
     */
    @Override
    @Transactional // Needs transaction as it modifies data
    public int markNotificationsAsRead(String userId, List<Long> notificationIds) {
        if (CollectionUtils.isEmpty(notificationIds)) {
            log.warn("Received markNotificationsAsRead request for user {} with empty ID list.", userId);
            return 0; // Nothing to mark
        }
        log.info("Marking notifications as read for user {}. IDs: {}", userId, notificationIds);
        int updatedCount = notificationRepository.markAsRead(userId, notificationIds);
        log.info("Marked {} notifications as read for user {}", updatedCount, userId);
        sendUnreadCountUpdate(userId);
        return updatedCount;
    }

    /**
     * Marks all unread notifications as read for a user.
     *
     * @param userId The ID of the user.
     * @return The number of notifications successfully marked as read.
     */
    @Override
    @Transactional // Needs transaction as it modifies data
    public int markAllNotificationsAsRead(String userId) {
        log.info("Marking all notifications as read for user {}", userId);
        int updatedCount = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for user {}", updatedCount, userId);
        // Optionally: Send a WebSocket message to update the unread count on clients?
         sendUnreadCountUpdate(userId);
        return updatedCount;
    }

    // Optional helper to push unread count updates via WebSocket
    private void sendUnreadCountUpdate(String userId) {
        try {
            long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
            String destination = "/queue/unread-count"; // Define a specific destination
            Map<String, Long> payload = Collections.singletonMap("unreadCount", count);
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
            log.info("Sent unread count update ({}) to user {}", count, userId);
        } catch (Exception e) {
            log.error("Failed to send unread count update for user {}: {}", userId, e.getMessage(), e);
        }
    }


    // --- Helper Method to Save and Send ---
    private void saveAndSendNotification(String userId, String type, String message, UUID auctionId, Long commentId) {
        try {
            // 1. Create and Save Notification Entity
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .message(message)
                    .isRead(false)
                    .relatedAuctionId(auctionId)
                    .relatedCommentId(commentId)
                    // createdAt is set automatically
                    .build();
            Notification savedNotification = notificationRepository.save(notification);
            log.debug("Saved notification ID {} for user {}", savedNotification.getId(), userId);

            // 2. Create DTO for WebSocket Payload
            NotificationDto notificationDto = NotificationDto.builder()
                    // .id(savedNotification.getId().toString()) // Include ID if client needs it
                    .type(savedNotification.getType())
                    .message(savedNotification.getMessage())
                    .timestamp(savedNotification.getCreatedAt())
                    .relatedAuctionId(savedNotification.getRelatedAuctionId())
                    // .relatedCommentId(savedNotification.getRelatedCommentId())
                    .isRead(savedNotification.isRead())
                    .details(null) // Add extra details if needed
                    .build();

            // 3. Send via WebSocket to specific user destination
            // The prefix '/user/' targets a specific user session managed by Spring WebSocket
            String destination = "/queue/notifications"; // User-specific destination suffix
            messagingTemplate.convertAndSendToUser(
                    userId,         // User ID (Spring resolves this to the correct session/topic)
                    destination,    // The destination suffix
                    notificationDto // The payload DTO
            );
            log.info("Sent WebSocket notification type '{}' to user {}, destination '/user/{}/{}'", type, userId, userId, destination);

            // 4. (Optional) Trigger Email Notification
            sendEmailNotification(userId, message); // Implement this method if needed


        } catch (Exception e) {
            // Log error but generally don't let notification failure stop event processing
            log.error("Failed to save or send notification type '{}' for user {}: {}", type, userId, e.getMessage(), e);
        }
    }

    // --- (Optional) Email Sending Logic ---
    private void sendEmailNotification(String userId, String message) {
        // 1. Fetch user email (handle potential errors/not found)
        try {
            Map<String, UserBasicInfoDto> userInfoMap = userServiceClient.getUsersBasicInfoByIds(Collections.singletonList(userId));
            UserBasicInfoDto userInfo = userInfoMap.get(userId);
            if (userInfo != null && userInfo.getEmail() != null && !userInfo.getEmail().isBlank()) {
                String email = userInfo.getEmail();
                log.info("Attempting to send email notification to user {} at {}", userId, email);
                // 2. Construct and Send Email using JavaMailSender
                // SimpleMailMessage mail = new SimpleMailMessage();
                // mail.setTo(email);
                // mail.setFrom("noreply@your-auction-site.com"); // Use configured sender
                // mail.setSubject("Auction Notification");
                // mail.setText(message);
                // mailSender.send(mail);
                // log.info("Email notification seemingly sent to user {}", userId);
            } else {
                log.warn("Could not send email notification to user {}: User info or email not found.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to fetch user info or send email notification for user {}: {}", userId, e.getMessage(), e);
        }
    }

    // --- Helper for truncating strings ---
    private String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "(Unknown Item)"; // Placeholder
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    // --- TODO: Implement methods for fetching/managing notifications via API ---
    // public Page<NotificationDto> getUserNotifications(String userId, Pageable pageable) { ... }
    // public long getUnreadNotificationCount(String userId) { ... }
    // public void markNotificationsAsRead(String userId, List<Long> notificationIds) { ... }
    // public void markAllNotificationsAsRead(String userId) { ... }

}