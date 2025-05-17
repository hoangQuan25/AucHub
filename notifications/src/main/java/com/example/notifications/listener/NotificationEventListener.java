package com.example.notifications.listener;

import com.example.notifications.config.RabbitMqConfig;
import com.example.notifications.event.NotificationEvents.*; // Import event classes
import com.example.notifications.service.NotificationService; // Create this service next
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
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

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(@Payload OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, bidderId={}", event.getOrderId(), event.getCurrentBidderId());
        try {
            // You'll need a corresponding processOrderCreated method in NotificationService
            notificationService.processOrderCreated(event);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
            // Consider error handling / dead-lettering
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_DUE_QUEUE)
    public void handleOrderPaymentDue(@Payload OrderCreatedEvent event) { // Reusing OrderCreatedEvent as discussed
        log.info("Received OrderPaymentDueEvent (using OrderCreatedEvent structure): orderId={}, newBidderId={}", event.getOrderId(), event.getCurrentBidderId());
        try {
            // You'll need a corresponding processOrderPaymentDue method in NotificationService
            notificationService.processOrderPaymentDue(event);
        } catch (Exception e) {
            log.error("Error processing OrderPaymentDueEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.USER_PAYMENT_DEFAULTED_QUEUE)
    public void handleUserPaymentDefaulted(@Payload UserPaymentDefaultedEvent event) {
        log.info("Received UserPaymentDefaultedEvent: orderId={}, defaultedUserId={}", event.getOrderId(), event.getDefaultedUserId());
        try {
            // You'll need a corresponding processUserPaymentDefaulted method in NotificationService
            notificationService.processUserPaymentDefaulted(event);
        } catch (Exception e) {
            log.error("Error processing UserPaymentDefaultedEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_SELLER_DECISION_REQUIRED_QUEUE)
    public void handleSellerDecisionRequired(@Payload SellerDecisionRequiredEvent event) {
        log.info("Received SellerDecisionRequiredEvent: orderId={}, sellerId={}", event.getOrderId(), event.getSellerId());
        try {
            // You'll need a corresponding processSellerDecisionRequired method in NotificationService
            notificationService.processSellerDecisionRequired(event);
        } catch (Exception e) {
            log.error("Error processing SellerDecisionRequiredEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_READY_FOR_SHIPPING_QUEUE)
    public void handleOrderReadyForShipping(@Payload OrderReadyForShippingEvent event) {
        log.info("Received OrderReadyForShippingEvent: orderId={}, buyerId={}", event.getOrderId(), event.getBuyerId());
        try {
            // You'll need a corresponding processOrderReadyForShipping method in NotificationService
            notificationService.processOrderReadyForShipping(event);
        } catch (Exception e) {
            log.error("Error processing OrderReadyForShippingEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(@Payload OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, reason={}", event.getOrderId(), event.getCancellationReason());
        try {
            // You'll need a corresponding processOrderCancelled method in NotificationService
            notificationService.processOrderCancelled(event);
        } catch (Exception e) {
            log.error("Error processing OrderCancelledEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.REFUND_SUCCEEDED_NOTIFICATION_QUEUE)
    public void handleRefundSucceeded(@Payload RefundSucceededEvent event) {
        log.info("Received RefundSucceededEvent for orderId: {}, buyerId: {}", event.getOrderId(), event.getBuyerId());
        try {
            notificationService.processRefundSucceeded(event);
        } catch (Exception e) {
            log.error("Error processing RefundSucceededEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.REFUND_FAILED_NOTIFICATION_QUEUE)
    public void handleRefundFailed(@Payload RefundFailedEvent event) {
        log.info("Received RefundFailedEvent for orderId: {}, buyerId: {}", event.getOrderId(), event.getBuyerId());
        try {
            notificationService.processRefundFailed(event);
        } catch (Exception e) {
            log.error("Error processing RefundFailedEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }

    // In NotificationEventListener.java
    @RabbitListener(queues = RabbitMqConfig.ORDER_AWAITING_FULFILLMENT_CONFIRMATION_QUEUE)
    public void handleOrderAwaitingFulfillmentConfirmation(@Payload OrderAwaitingFulfillmentConfirmationEvent event) {
        log.info("Received OrderAwaitingFulfillmentConfirmationEvent: orderId={}, sellerId={}", event.getOrderId(), event.getSellerId());
        try {
            notificationService.processOrderAwaitingFulfillmentConfirmation(event);
        } catch (Exception e) {
            log.error("Error processing OrderAwaitingFulfillmentConfirmationEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}