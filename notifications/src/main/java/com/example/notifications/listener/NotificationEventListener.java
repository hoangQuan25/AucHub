package com.example.notifications.listener;

import com.example.notifications.config.RabbitMqConfig;
import com.example.notifications.event.NotificationEvents.*; // Import event classes
import com.example.notifications.service.NotificationService; // Create this service next
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    // Inject a service to handle the notification logic
    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConfig.AUCTION_STARTED_QUEUE)
    public void handleAuctionStarted(AuctionStartedEvent event) {
        log.info("Received AuctionStartedEvent: {}", event);
        try {
            notificationService.processAuctionStarted(event); // Call service method
        } catch (Exception e) {
            log.error("Error processing AuctionStartedEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_ENDED_QUEUE)
    public void handleAuctionEnded(AuctionEndedEvent event) {
        log.info("Received AuctionEndedEvent: {}", event);
        try {
            notificationService.processAuctionEnded(event);
        } catch (Exception e) {
            log.error("Error processing AuctionEndedEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
            // Consider error handling / dead-lettering
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_OUTBID_QUEUE)
    public void handleOutbid(OutbidEvent event) {
        log.info("Received OutbidEvent: {}", event);
        try {
            notificationService.processOutbid(event);
        } catch (Exception e) {
            log.error("Error processing OutbidEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
            // Consider error handling / dead-lettering
        }
    }

    @RabbitListener(queues = RabbitMqConfig.COMMENT_REPLIED_QUEUE)
    public void handleCommentReply(CommentReplyEvent event) {
        log.info("Received CommentReplyEvent: {}", event);
        try {
            notificationService.processCommentReply(event);
        } catch (Exception e) {
            log.error("Error processing CommentReplyEvent for comment {}: {}", event.getReplyCommentId(), e.getMessage(), e);
            // Consider error handling / dead-lettering
        }
    }
}