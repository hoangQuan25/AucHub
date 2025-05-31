package com.example.notifications.listener;

import com.example.notifications.config.RabbitMqConfig;
import com.example.notifications.event.DeliveryEvents;
import com.example.notifications.event.NotificationEvents.*; // Import event classes
import com.example.notifications.service.NotificationService; // Create this service next
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    // Inject a service to handle the notification logic
    private final NotificationService notificationService;

    private String getAuctionTypeFromRoutingKey(String routingKey) {
        if (routingKey == null) return "TIMED"; // Default fallback
        if (routingKey.contains(".live.")) return "LIVE";
        if (routingKey.contains(".timed.")) return "TIMED";
        return "TIMED"; // Default if no match
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_STARTED_QUEUE)
    public void handleAuctionStarted(
            @Payload AuctionStartedEvent event,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) { // <-- Capture routing key
        log.info("Received AuctionStartedEvent via key [{}]: {}", routingKey, event);
        try {
            String auctionType = getAuctionTypeFromRoutingKey(routingKey);
            notificationService.processAuctionStarted(event, auctionType); // <-- Pass type to service
        } catch (Exception e) {
            log.error("Error processing AuctionStartedEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_ENDED_QUEUE)
    public void handleAuctionEnded(
            @Payload AuctionEndedEvent event,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) { // <-- Capture routing key
        log.info("Received AuctionEndedEvent via key [{}]: {}", routingKey, event);
        try {
            String auctionType = getAuctionTypeFromRoutingKey(routingKey);
            notificationService.processAuctionEnded(event, auctionType); // <-- Pass type to service
        } catch (Exception e) {
            log.error("Error processing AuctionEndedEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_OUTBID_QUEUE)
    public void handleOutbid(
            @Payload OutbidEvent event,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) { // <-- Capture routing key
        log.info("Received OutbidEvent via key [{}]: {}", routingKey, event);
        try {
            // Outbid events are only from Timed Auctions as per your info
            notificationService.processOutbid(event, "TIMED");
        } catch (Exception e) {
            log.error("Error processing OutbidEvent for auction {}: {}", event.getAuctionId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.COMMENT_REPLIED_QUEUE)
    public void handleCommentReply(
            @Payload CommentReplyEvent event,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) { // <-- Capture routing key
        log.info("Received CommentReplyEvent via key [{}]: {}", routingKey, event);
        try {
            // Comment events are only from Timed Auctions as per your info
            notificationService.processCommentReply(event, "TIMED");
        } catch (Exception e) {
            log.error("Error processing CommentReplyEvent for comment {}: {}", event.getReplyCommentId(), e.getMessage(), e);
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

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_CREATED_NOTIFICATION_QUEUE)
    public void handleDeliveryCreated(@Payload DeliveryEvents.DeliveryCreatedEventDto event) {
        log.info("Received DeliveryCreatedEvent: deliveryId={}, orderId={}, buyerId={}, sellerId={}",
                event.getDeliveryId(), event.getOrderId(), event.getBuyerId(), event.getSellerId());
        try {
            notificationService.processDeliveryCreated(event); // We'll define this in NotificationService
        } catch (Exception e) {
            log.error("Error processing DeliveryCreatedEvent for deliveryId {}: {}", event.getDeliveryId(), e.getMessage(), e);
            // Consider error handling / dead-lettering
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_SHIPPED_NOTIFICATION_QUEUE)
    public void handleDeliveryShipped(@Payload DeliveryEvents.DeliveryShippedEventDto event) {
        log.info("Received DeliveryShippedEvent: deliveryId={}, orderId={}, trackingNumber={}",
                event.getDeliveryId(), event.getOrderId(), event.getTrackingNumber());
        try {
            notificationService.processDeliveryShipped(event); // We'll define this in NotificationService
        } catch (Exception e) {
            log.error("Error processing DeliveryShippedEvent for deliveryId {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_DELIVERED_NOTIFICATION_QUEUE)
    public void handleDeliveryDelivered(@Payload DeliveryEvents.DeliveryDeliveredEventDto event) {
        log.info("Received DeliveryDeliveredEvent: deliveryId={}, orderId={}, deliveredAt={}",
                event.getDeliveryId(), event.getOrderId(), event.getDeliveredAt());
        try {
            notificationService.processDeliveryDelivered(event); // We'll define this in NotificationService
        } catch (Exception e) {
            log.error("Error processing DeliveryDeliveredEvent for deliveryId {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_AWAITING_BUYER_CONFIRMATION_NOTIFICATION_QUEUE)
    public void handleDeliveryAwaitingBuyerConfirmation(@Payload DeliveryEvents.DeliveryAwaitingBuyerConfirmationEventDto event) {
        log.info("Received DeliveryAwaitingBuyerConfirmationEvent: deliveryId={}, orderId={}, buyerId={}",
                event.getDeliveryId(), event.getOrderId(), event.getBuyerId());
        try {
            notificationService.processDeliveryAwaitingBuyerConfirmation(event);
        } catch (Exception e) {
            log.error("Error processing DeliveryAwaitingBuyerConfirmationEvent for deliveryId {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_ISSUE_REPORTED_NOTIFICATION_QUEUE)
    public void handleDeliveryIssueReported(@Payload DeliveryEvents.DeliveryIssueReportedEventDto event) {
        log.info("Received DeliveryIssueReportedEvent: deliveryId={}, orderId={}, issue='{}'",
                event.getDeliveryId(), event.getOrderId(), event.getIssueNotes());
        try {
            notificationService.processDeliveryIssueReported(event); // We'll define this in NotificationService
        } catch (Exception e) {
            log.error("Error processing DeliveryIssueReportedEvent for deliveryId {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.USER_BANNED_NOTIFICATION_QUEUE)
    public void handleUserBanned(@Payload UserBannedEvent event) { // Use the new DTO
        log.info("Received UserBannedEvent: userId={}, banEndsAt={}, level={}",
                event.getUserId(), event.getBanEndsAt(), event.getBanLevel());
        try {
            notificationService.processUserBanned(event); // Create this method in NotificationService
        } catch (Exception e) {
            log.error("Error processing UserBannedEvent for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_RETURN_REQUESTED_QUEUE)
    public void handleDeliveryReturnRequested(@Payload DeliveryEvents.DeliveryReturnRequestedEventDto event) {
        log.info("Received DeliveryReturnRequestedEvent: deliveryId={}, orderId={}, buyerId={}",
                event.getDeliveryId(), event.getOrderId(), event.getBuyerId());
        try {
            notificationService.processDeliveryReturnRequested(event);
        } catch (Exception e) {
            log.error("Error processing DeliveryReturnRequestedEvent for delivery {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_RETURN_APPROVED_QUEUE)
    public void handleDeliveryReturnApproved(@Payload DeliveryEvents.DeliveryReturnApprovedEventDto event) {
        log.info("Received DeliveryReturnApprovedEvent: deliveryId={}, orderId={}, sellerId={}",
                event.getDeliveryId(), event.getOrderId(), event.getSellerId());
        try {
            // This event notifies the buyer that their return request was approved by the seller.
            notificationService.processDeliveryReturnApproved(event);
        } catch (Exception e) {
            log.error("Error processing DeliveryReturnApprovedEvent for delivery {}: {}", event.getDeliveryId(), e.getMessage(), e);
        }
    }
}