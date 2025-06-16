package com.example.notifications.service.impl;

import com.example.notifications.client.LiveAuctionServiceClient;
import com.example.notifications.client.TimedAuctionServiceClient;
import com.example.notifications.client.UserServiceClient;
import com.example.notifications.client.dto.LiveAuctionSummaryDto;
import com.example.notifications.client.dto.TimedAuctionSummaryDto;
import com.example.notifications.client.dto.UserBasicInfoDto; // Assuming this DTO is available
import com.example.notifications.dto.FollowingAuctionSummaryDto;
import com.example.notifications.dto.NotificationDto; // DTO for WebSocket payload
import com.example.notifications.entity.AuctionFollower;
import com.example.notifications.entity.Notification; // DB Entity
import com.example.notifications.entity.AuctionStatus; // Enum for status check
import com.example.notifications.event.DeliveryEvents;
import com.example.notifications.event.NotificationEvents;
import com.example.notifications.event.NotificationEvents.*; // Event types
import com.example.notifications.mapper.NotificationMapper;
import com.example.notifications.repository.AuctionFollowerRepository;
import com.example.notifications.repository.NotificationRepository; // DB Repo
import com.example.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate; // For WebSocket messages
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import if needed
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserServiceClient userServiceClient;
    private final NotificationMapper notificationMapper;
    private final AuctionFollowerRepository auctionFollowerRepository;
    private final TimedAuctionServiceClient timedAuctionServiceClient;
    private final LiveAuctionServiceClient liveAuctionServiceClient;

    private static final String TYPE_AUCTION_STARTED = "AUCTION_STARTED"; // Added for consistency
    private static final String TYPE_AUCTION_ENDED = "AUCTION_ENDED";
    private static final String TYPE_OUTBID = "AUCTION_OUTBID";
    private static final String TYPE_COMMENT_REPLY = "COMMENT_REPLY";

    // --- New Notification Types for Order Events ---
    private static final String TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String TYPE_ORDER_PAYMENT_DUE = "ORDER_PAYMENT_DUE";
    private static final String TYPE_SELLER_DECISION_REQUIRED = "SELLER_DECISION_REQUIRED";
    private static final String TYPE_ORDER_READY_FOR_SHIPPING = "ORDER_READY_FOR_SHIPPING";
    private static final String TYPE_ORDER_CANCELLED = "ORDER_CANCELLED";
    private static final String TYPE_USER_PAYMENT_DEFAULTED = "USER_PAYMENT_DEFAULTED";
    private static final String TYPE_REFUND_SUCCEEDED = "REFUND_SUCCEEDED";
    private static final String TYPE_REFUND_FAILED = "REFUND_FAILED";
    private static final String TYPE_ORDER_AWAITING_FULFILLMENT_CONFIRMATION = "ORDER_AWAITING_FULFILLMENT_CONFIRMATION";

    private static final String TYPE_DELIVERY_CREATED = "DELIVERY_CREATED";
    private static final String TYPE_DELIVERY_SHIPPED = "DELIVERY_SHIPPED";
    private static final String TYPE_DELIVERY_DELIVERED = "DELIVERY_DELIVERED";
    private static final String TYPE_DELIVERY_AWAITING_BUYER_CONFIRMATION = "DELIVERY_AWAITING_BUYER_CONFIRMATION";
    private static final String TYPE_DELIVERY_ISSUE_REPORTED = "DELIVERY_ISSUE_REPORTED";

    private static final String TYPE_USER_BANNED = "USER_BANNED";

    private static final String TYPE_DELIVERY_RETURN_REQUESTED = "DELIVERY_RETURN_REQUESTED";
    private static final String TYPE_DELIVERY_RETURN_APPROVED = "DELIVERY_RETURN_APPROVED";


    private static final DateTimeFormatter SHORT_DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

    @Override
    @Transactional
    public void processAuctionStarted(AuctionStartedEvent event, String auctionType) {
        log.debug("Processing AuctionStartedEvent for {} auction {}", auctionType, event.getAuctionId());
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        String message;
        Set<String> notifiedUserIds = new HashSet<>();

        // Notify Seller
        if (event.getSellerId() != null) {
            notifiedUserIds.add(event.getSellerId());
            // : Use the auctionType parameter
            message = String.format("Your %s auction for '%s' has started.",
                    auctionType.toLowerCase(), productTitle);
            saveAndSendNotification(event.getSellerId(), TYPE_AUCTION_STARTED, message, event.getAuctionId(), auctionType, null, null);
        }

        // Notify Followers
        List<String> followerIds = getFollowersForAuction(event.getAuctionId());
        if (!followerIds.isEmpty()) {
            // : Use the auctionType parameter
            message = String.format("The %s auction you follow for '%s' has started!",
                    auctionType.toLowerCase(), productTitle);
            String finalMessage = message;
            followerIds.stream()
                    .filter(followerId -> !notifiedUserIds.contains(followerId))
                    .forEach(followerId -> saveAndSendNotification(followerId, TYPE_AUCTION_STARTED, finalMessage, event.getAuctionId(), auctionType, null, null));
        }
        log.info("Processed AuctionStartedEvent for auction {}, notified {} users.", event.getAuctionId(), notifiedUserIds.size() + followerIds.size());
    }

    @Override
    @Transactional
    public void processAuctionEnded(AuctionEndedEvent event, String auctionType) {
        log.debug("Processing AuctionEndedEvent for {} auction {}", auctionType, event.getAuctionId());
        Set<String> notifiedUserIds = new HashSet<>();
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);

        // Notify Seller
        if (event.getSellerId() != null) {
            notifiedUserIds.add(event.getSellerId());
            // : Create a more specific base message
            String message = String.format("The %s auction for '%s' has ended.", auctionType.toLowerCase(), productTitle);
            if (event.getFinalStatus() == AuctionStatus.SOLD) {
                message += String.format(" You sold it for %s VNĐ to %s.",
                        event.getWinningBid().toPlainString(), event.getWinnerUsernameSnapshot());
            } else if (event.getFinalStatus() == AuctionStatus.RESERVE_NOT_MET) {
                message += " The reserve price was not met.";
            } else { // CANCELLED
                message += " You have cancelled this auction.";
            }
            saveAndSendNotification(event.getSellerId(), TYPE_AUCTION_ENDED, message, event.getAuctionId(), auctionType, null, null);
        }

        // Notify Winner
        if (event.getFinalStatus() == AuctionStatus.SOLD && event.getWinnerId() != null && !event.getWinnerId().equals(event.getSellerId())) {
            notifiedUserIds.add(event.getWinnerId());
            String message = String.format("Congratulations! You won the %s auction for '%s' with a bid of %s VNĐ.",
                    auctionType.toLowerCase(), productTitle, event.getWinningBid().toPlainString());
            saveAndSendNotification(event.getWinnerId(), TYPE_AUCTION_ENDED, message, event.getAuctionId(), auctionType, null, null);
        }

        // Notify Followers
        String generalEndMessage;
        if (event.getFinalStatus() == AuctionStatus.SOLD) {
            generalEndMessage = String.format("The %s auction for '%s' has ended, selling to %s for %s VNĐ.",
                    auctionType.toLowerCase(), productTitle, event.getWinnerUsernameSnapshot(), event.getWinningBid().toPlainString());
        } else if (event.getFinalStatus() == AuctionStatus.RESERVE_NOT_MET) {
            generalEndMessage = String.format("The %s auction for '%s' has ended. The reserve price was not met.", auctionType.toLowerCase(), productTitle);
        } else { // CANCELLED
            generalEndMessage = String.format("The %s auction for '%s' was cancelled.", auctionType.toLowerCase(), productTitle);
        }
        getFollowersForAuction(event.getAuctionId()).stream()
                .filter(followerId -> !notifiedUserIds.contains(followerId))
                .forEach(followerId -> saveAndSendNotification(followerId, TYPE_AUCTION_ENDED, generalEndMessage, event.getAuctionId(), auctionType, null, null));
    }

    @Override
    @Transactional
    public void processOutbid(OutbidEvent event, String auctionType) {
        log.debug("Processing OutbidEvent for {} auction {}, user {}", auctionType, event.getAuctionId(), event.getOutbidUserId());

        // : Specify it's a timed auction.
        String message = String.format("You've been outbid on the timed auction for '%s'! The new bid is %s VNĐ by %s.",
                truncate(event.getProductTitleSnapshot(), 50),
                event.getNewCurrentBid().toPlainString(),
                event.getNewHighestBidderUsernameSnapshot());

        saveAndSendNotification(event.getOutbidUserId(), TYPE_OUTBID, message, event.getAuctionId(), auctionType, null, null);
        log.info("Processed OutbidEvent for auction {}, notified user {}.", event.getAuctionId(), event.getOutbidUserId());
    }

    @Override
    @Transactional
    public void processCommentReply(CommentReplyEvent event, String auctionType) {
        log.debug("Processing CommentReplyEvent for {} auction {}", auctionType, event.getAuctionId());
        if (event.getReplierUserId().equals(event.getOriginalCommenterId())) {
            return;
        }

        // : Specify it's on a timed auction.
        String message = String.format("%s replied to your comment on the timed auction for '%s': '%s'",
                event.getReplierUsernameSnapshot(),
                truncate(event.getProductTitleSnapshot(), 40),
                truncate(event.getReplyCommentTextSample(), 80));

        saveAndSendNotification(event.getOriginalCommenterId(), TYPE_COMMENT_REPLY, message, event.getAuctionId(), auctionType, null, event.getReplyCommentId());
        log.info("Processed CommentReplyEvent for auction {}, notified user {}.", event.getAuctionId(), event.getOriginalCommenterId());
    }

    @Override
    @Transactional
    public void processOrderCreated(OrderCreatedEvent event) {
        log.debug("Processing OrderCreatedEvent for order ID: {}", event.getOrderId());
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        String deadlineStr = event.getPaymentDeadline().format(SHORT_DATE_TIME_FORMATTER);

        Map<String, String> userNames = getUsernamesFromIds(Arrays.asList(event.getSellerId(), event.getCurrentBidderId()));
        String buyerUsername = userNames.getOrDefault(event.getCurrentBidderId(), "a buyer");


        // Notify Buyer
        String buyerMessage = String.format("Your order for '%s' is created! Please pay %s %s by %s.",
                productTitle, event.getAmountDue().toPlainString(), event.getCurrency(), deadlineStr);
        saveAndSendNotification(event.getCurrentBidderId(), TYPE_ORDER_CREATED, buyerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        // Notify Seller
        String sellerMessage = String.format("Order created for your item '%s'. Awaiting payment of %s %s from buyer %s.",
                productTitle, event.getAmountDue().toPlainString(), event.getCurrency(), buyerUsername);
        saveAndSendNotification(event.getSellerId(), TYPE_ORDER_CREATED, sellerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        log.info("Processed OrderCreatedEvent for order {}, notified buyer {} and seller {}.",
                event.getOrderId(), event.getCurrentBidderId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processOrderPaymentDue(OrderCreatedEvent event) {
        log.debug("Processing OrderPaymentDueEvent for order ID: {}, new current bidder: {}", event.getOrderId(), event.getCurrentBidderId());
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        String deadlineStr = event.getPaymentDeadline().format(SHORT_DATE_TIME_FORMATTER);

        Map<String, String> userNames = getUsernamesFromIds(Collections.singletonList(event.getCurrentBidderId()));
        String newBidderUsername = userNames.getOrDefault(event.getCurrentBidderId(), "the next bidder");

        // Notify the new current bidder
        String bidderMessage = String.format("Good news! The item '%s' is now offered to you. Please pay %s %s by %s.",
                productTitle, event.getAmountDue().toPlainString(), event.getCurrency(), deadlineStr);
        saveAndSendNotification(event.getCurrentBidderId(), TYPE_ORDER_PAYMENT_DUE, bidderMessage, event.getAuctionId(), null, event.getOrderId(), null);

        // Notify the seller
        String sellerMessage = String.format("The offer for item '%s' (Order: %s) has been extended to %s.",
                productTitle, event.getOrderId().toString().substring(0, 8), newBidderUsername);
        saveAndSendNotification(event.getSellerId(), TYPE_ORDER_PAYMENT_DUE, sellerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        log.info("Processed OrderPaymentDueEvent for order {}, notified new bidder {} and seller {}.",
                event.getOrderId(), event.getCurrentBidderId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processUserPaymentDefaulted(UserPaymentDefaultedEvent event) {
        log.debug("Processing UserPaymentDefaultedEvent for order ID: {}, user: {}", event.getOrderId(), event.getDefaultedUserId());
        // Business decision: Do we send a direct notification to the user who defaulted?
        // It might be perceived negatively. Often, the consequence (e.g., seller needs to decide)
        // is notified to other parties.
        // For now, let's log it. If a direct notification is desired:
        /*
        String productTitle = "(Product related to order " + event.getOrderId().toString().substring(0,8) + ")";
        // Potentially fetch product title if crucial and not in event, but avoid if possible for perf.
        String userMessage = String.format("Payment for order concerning auction '%s' was not completed by the deadline. Order ID: %s.",
                                           event.getAuctionId().toString().substring(0,8), event.getOrderId().toString().substring(0,8));
        saveAndSendNotification(event.getDefaultedUserId(), TYPE_USER_PAYMENT_DEFAULTED, userMessage, event.getAuctionId(), null);
        log.info("Sent UserPaymentDefaulted notification to user {}", event.getDefaultedUserId());
        */
        log.info("UserPaymentDefaultedEvent processed for user {}. No direct user notification sent by default.", event.getDefaultedUserId());
        // The impact of this default is usually covered by notifications like SELLER_DECISION_REQUIRED or ORDER_PAYMENT_DUE to next bidder.
    }

    @Override
    @Transactional
    public void processSellerDecisionRequired(SellerDecisionRequiredEvent event) {
        log.debug("Processing SellerDecisionRequiredEvent for order ID: {}", event.getOrderId());
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);

        Map<String, String> userNames = getUsernamesFromIds(Collections.singletonList(event.getDefaultedBidderId()));
        String defaultedBidderUsername = userNames.getOrDefault(event.getDefaultedBidderId(), "the previous bidder");

        String sellerMessage = String.format("Action required for '%s'. Payment wasn't completed by %s. Please decide the next step for Order #%s.",
                productTitle, defaultedBidderUsername, event.getOrderId().toString().substring(0, 8));
        saveAndSendNotification(event.getSellerId(), TYPE_SELLER_DECISION_REQUIRED, sellerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        log.info("Processed SellerDecisionRequiredEvent for order {}, notified seller {}.", event.getOrderId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processOrderReadyForShipping(OrderReadyForShippingEvent event) {
        log.debug("Processing OrderReadyForShippingEvent for order ID: {}", event.getOrderId());
        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);

        Map<String, String> userNames = getUsernamesFromIds(Arrays.asList(event.getSellerId(), event.getBuyerId()));
        String sellerUsername = userNames.getOrDefault(event.getSellerId(), "The seller");
        String buyerUsername = userNames.getOrDefault(event.getBuyerId(), "the buyer");

        // Notify Buyer
        String buyerMessage = String.format("Payment confirmed for '%s'! Seller %s is preparing it for shipping. Order: #%s",
                productTitle, sellerUsername, orderIdShort);
        saveAndSendNotification(event.getBuyerId(), TYPE_ORDER_READY_FOR_SHIPPING, buyerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        // Notify Seller
        String sellerMessage = String.format("Payment received for '%s' from buyer %s! Please prepare for shipping. Order: #%s",
                productTitle, buyerUsername, orderIdShort);
        saveAndSendNotification(event.getSellerId(), TYPE_ORDER_READY_FOR_SHIPPING, sellerMessage, event.getAuctionId(), null, event.getOrderId(), null);

        log.info("Processed OrderReadyForShippingEvent for order {}, notified buyer {} and seller {}.",
                event.getOrderId(), event.getBuyerId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processOrderCancelled(OrderCancelledEvent event) {
        log.debug("Processing OrderCancelledEvent for order ID: {}", event.getOrderId());
        // String productTitle = truncate(event.getProductTitleSnapshot(), 50); // Product title is not in OrderCancelledEvent DTO
        // We might need to adjust OrderCancelledEvent DTO or fetch product title if it's essential for the message.
        // For now, using auction ID and order ID.
        String baseMessage = String.format("Order %s (related to auction %s) has been cancelled. Reason: %s.",
                event.getOrderId().toString().substring(0,8),
                event.getAuctionId().toString().substring(0,8),
                truncate(event.getCancellationReason(), 100));

        // Notify Seller
        saveAndSendNotification(event.getSellerId(), TYPE_ORDER_CANCELLED, baseMessage + " (As Seller)", event.getAuctionId(), null, event.getOrderId(), null);

        // Notify the bidder involved at cancellation, if they are not the seller (and if a bidder was involved)
        if (event.getCurrentBidderIdAtCancellation() != null && !event.getCurrentBidderIdAtCancellation().equals(event.getSellerId())) {
            saveAndSendNotification(event.getCurrentBidderIdAtCancellation(), TYPE_ORDER_CANCELLED, baseMessage + " (As Buyer/Bidder)", event.getAuctionId(), null, null, null);
            log.info("Processed OrderCancelledEvent for order {}, notified seller {} and involved bidder {}.",
                    event.getOrderId(), event.getSellerId(), event.getCurrentBidderIdAtCancellation());
        } else {
            log.info("Processed OrderCancelledEvent for order {}, notified seller {}. No distinct involved bidder to notify or bidder was seller.",
                    event.getOrderId(), event.getSellerId());
        }
    }

    @Override
    @Transactional
    public void processRefundSucceeded(RefundSucceededEvent event) {
        log.debug("Processing RefundSucceededEvent for order ID: {}", event.getOrderId());

        if (event.getBuyerId() == null) {
            log.error("Cannot send refund succeeded notification: buyerId is missing in event for order {}", event.getOrderId());
            return;
        }

        // Format amount (assuming amountRefunded is in smallest unit, e.g., cents or base unit for VND)
        // This is a simplified formatting. For production, consider a robust currency formatting library or util.
        String formattedAmount;
        try {
            BigDecimal amountDecimal;
            if ("vnd".equalsIgnoreCase(event.getCurrency())) {
                amountDecimal = new BigDecimal(event.getAmountRefunded()); // VND is base unit in Stripe
            } else {
                // For currencies like USD, EUR (2 decimal places)
                amountDecimal = new BigDecimal(event.getAmountRefunded()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            // Using NumberFormat for basic currency display
            NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")); // Example for VND
            if (!"vnd".equalsIgnoreCase(event.getCurrency())) {
                // Attempt to use a generic locale or one based on event.getCurrency() if possible
                // This part might need more sophisticated currency handling if you support many.
                currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US); // Fallback or determine by currency
                currencyFormatter.setCurrency(java.util.Currency.getInstance(event.getCurrency().toUpperCase()));
            }
            formattedAmount = currencyFormatter.format(amountDecimal);


        } catch (Exception e) {
            log.warn("Could not format refund amount for notification, using raw value. Order ID: {}", event.getOrderId(), e);
            formattedAmount = event.getAmountRefunded() + " " + event.getCurrency().toUpperCase();
        }


        String message = String.format("Good news! Your refund of %s for order #%s has been processed successfully. Stripe Refund ID: %s.",
                formattedAmount,
                event.getOrderId().toString().substring(0, 8),
                event.getRefundId());

        // Assuming auctionId is not directly relevant for a refund notification,
        // but if your Order entity (fetched via OrderService if needed) has it, you could pass it.
        // For now, passing orderId as relatedAuctionId for consistency if needed by saveAndSend or null.
        saveAndSendNotification(event.getBuyerId(), TYPE_REFUND_SUCCEEDED, message, null, null, event.getOrderId(), null);

        log.info("Processed RefundSucceededEvent for order {}, notified buyer {}.", event.getOrderId(), event.getBuyerId());
    }

    @Override
    @Transactional
    public void processRefundFailed(RefundFailedEvent event) {
        log.debug("Processing RefundFailedEvent for order ID: {}", event.getOrderId());

        if (event.getBuyerId() == null) {
            log.error("Cannot send refund failed notification: buyerId is missing in event for order {}", event.getOrderId());
            return;
        }

        String message = String.format("We encountered an issue processing your refund for order #%s. Reason: %s. Please contact support if this persists. Payment Intent ID: %s",
                event.getOrderId().toString().substring(0, 8),
                truncate(event.getFailureReason(), 100),
                event.getPaymentIntentId());

        saveAndSendNotification(event.getBuyerId(), TYPE_REFUND_FAILED, message, null, null, event.getOrderId(), null);

        log.info("Processed RefundFailedEvent for order {}, notified buyer {}. Reason: {}", event.getOrderId(), event.getBuyerId(), event.getFailureReason());
        // You might also want to notify an admin/support team about refund failures.
    }

    @Override
    @Transactional
    public void processOrderAwaitingFulfillmentConfirmation(OrderAwaitingFulfillmentConfirmationEvent event) {
        log.debug("Processing OrderAwaitingFulfillmentConfirmationEvent for order ID: {}", event.getOrderId());

        if (event.getSellerId() == null) {
            log.error("Cannot send order awaiting fulfillment notification: sellerId is missing. OrderId: {}", event.getOrderId());
            return;
        }

        String productTitle = truncate(event.getProductTitleSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);

        Map<String, String> userNames = getUsernamesFromIds(Arrays.asList(event.getSellerId(), event.getBuyerId()));
        String buyerUsername = userNames.getOrDefault(event.getBuyerId(), "the buyer");
        String sellerUsername = userNames.getOrDefault(event.getSellerId(), "The seller");


        // --- Notification to Seller ---
        String sellerMessage = String.format(
                "Action Required: Order #%s for '%s' has been paid by %s. Please confirm if you can fulfill this order.",
                orderIdShort, productTitle, buyerUsername
        );
        saveAndSendNotification(event.getSellerId(), TYPE_ORDER_AWAITING_FULFILLMENT_CONFIRMATION, sellerMessage, null, null, event.getOrderId(), null);
        log.info("Notified seller {} for order {} awaiting fulfillment confirmation.", event.getSellerId(), event.getOrderId());

        // --- Notification to Buyer ---
        if (event.getBuyerId() != null) {
            String buyerMessage = String.format(
                    "Your payment for order #%s ('%s') was successful! %s is now confirming fulfillment before shipping.",
                    orderIdShort, productTitle, sellerUsername
            );
            saveAndSendNotification(event.getBuyerId(), TYPE_ORDER_AWAITING_FULFILLMENT_CONFIRMATION, buyerMessage, null, null, event.getOrderId(), null);
            log.info("Notified buyer {} for order {} that payment is confirmed, awaiting seller fulfillment.", event.getBuyerId(), event.getOrderId());
        }
    }

    @Override
    @Transactional
    public void processDeliveryCreated(DeliveryEvents.DeliveryCreatedEventDto event) {
        log.debug("Processing DeliveryCreatedEvent for deliveryId: {}", event.getDeliveryId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);
        String deliveryIdShort = event.getDeliveryId().toString().substring(0, 8);

        // Notify Buyer
        String buyerMessage = String.format("The seller is preparing your order #%s (%s) for shipment. Delivery ID: #%s.",
                orderIdShort, productInfo, deliveryIdShort);
        // For delivery notifications, using event.getDeliveryId() as the relatedId seems most direct.
        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_CREATED, buyerMessage, null, null, event.getOrderId(), null);

        // Notify Seller
        String sellerMessage = String.format("Please prepare order #%s (%s) for shipment. Delivery ID: #%s.",
                orderIdShort, productInfo, deliveryIdShort);
        saveAndSendNotification(event.getSellerId(), TYPE_DELIVERY_CREATED, sellerMessage, null, null, event.getOrderId(), null);

        log.info("Processed DeliveryCreatedEvent for delivery {}, notified buyer {} and seller {}.",
                event.getDeliveryId(), event.getBuyerId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processDeliveryShipped(DeliveryEvents.DeliveryShippedEventDto event) {
        log.debug("Processing DeliveryShippedEvent for deliveryId: {}", event.getDeliveryId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);

        // Notify Buyer
        String buyerMessage = String.format("Shipped! Your order #%s (%s) is on its way via %s. Tracking: %s.",
                orderIdShort,
                productInfo,
                event.getCourierName(),
                event.getTrackingNumber());
        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_SHIPPED, buyerMessage, null, null, event.getOrderId(), null);

        log.info("Processed DeliveryShippedEvent for delivery {}, notified buyer {}.",
                event.getDeliveryId(), event.getBuyerId());
    }

    @Override
    @Transactional
    public void processDeliveryDelivered(DeliveryEvents.DeliveryDeliveredEventDto event) {
        log.debug("Processing DeliveryDeliveredEvent for deliveryId: {}", event.getDeliveryId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);
        String deliveredAtFormatted = event.getDeliveredAt() != null ? event.getDeliveredAt().format(SHORT_DATE_TIME_FORMATTER) : "recently";

        Map<String, String> userNames = getUsernamesFromIds(Collections.singletonList(event.getBuyerId()));
        String buyerUsername = userNames.getOrDefault(event.getBuyerId(), "the buyer");


        // Notify Buyer
        String buyerMessage = String.format("Delivered! Your order #%s (%s) was marked as delivered on %s.",
                orderIdShort, productInfo, deliveredAtFormatted);
        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_DELIVERED, buyerMessage, null, null, event.getOrderId(), null);

        // Notify Seller
        String sellerMessage = String.format("Order #%s (%s) for buyer %s has been marked as delivered on %s.",
                orderIdShort, productInfo, buyerUsername, deliveredAtFormatted);
        saveAndSendNotification(event.getSellerId(), TYPE_DELIVERY_DELIVERED, sellerMessage, null, null, event.getOrderId(), null);

        log.info("Processed DeliveryDeliveredEvent for delivery {}, notified buyer {} and seller {}.",
                event.getDeliveryId(), event.getBuyerId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processDeliveryAwaitingBuyerConfirmation(DeliveryEvents.DeliveryAwaitingBuyerConfirmationEventDto event) {
        log.debug("Processing DeliveryAwaitingBuyerConfirmationEvent for delivery ID: {}", event.getDeliveryId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 40);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);
        String deliveredAtStr = event.getDeliveredAt() != null ? event.getDeliveredAt().format(SHORT_DATE_TIME_FORMATTER) : "recently";

        // Notify Buyer
        String buyerMessage = String.format(
                "Item Delivered! Your order #%s (%s) was marked as delivered around %s. Please confirm receipt within 3 days via the order details page.",
                orderIdShort, productInfo, deliveredAtStr
        );
        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_AWAITING_BUYER_CONFIRMATION, buyerMessage, null, null,  event.getOrderId(), null);

        // Notify Seller
        String sellerMessage = String.format(
                "Item Delivered for order #%s (%s). Awaiting buyer's confirmation of receipt.",
                orderIdShort, productInfo
        );
        saveAndSendNotification(event.getSellerId(), TYPE_DELIVERY_AWAITING_BUYER_CONFIRMATION, sellerMessage, null, null, event.getOrderId(), null);

        log.info("Processed DeliveryAwaitingBuyerConfirmationEvent for delivery {}, notified buyer {} and seller {}.",
                event.getDeliveryId(), event.getBuyerId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processDeliveryIssueReported(DeliveryEvents.DeliveryIssueReportedEventDto event) {
        log.debug("Processing DeliveryIssueReportedEvent for deliveryId: {}", event.getDeliveryId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);
        String issue = truncate(event.getIssueNotes(), 70);

        Map<String, String> userNames = getUsernamesFromIds(Collections.singletonList(event.getReporterId()));
        String reporterUsername = userNames.getOrDefault(event.getReporterId(), "A user");

        // Notify Buyer
        String buyerMessage = String.format("Delivery Update for order #%s (%s): An issue was reported - '%s'.",
                orderIdShort, productInfo, issue);
        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_ISSUE_REPORTED, buyerMessage, null, null, event.getOrderId(), null);


        // Notify Seller only if they were not the one who reported it
        if (!Objects.equals(event.getSellerId(), event.getReporterId())) {
            String sellerMessage = String.format("Issue reported for delivery #%s (order #%s): '%s'. Reported by: %s.",
                    event.getDeliveryId().toString().substring(0,8), orderIdShort, issue, reporterUsername);
            saveAndSendNotification(event.getSellerId(), TYPE_DELIVERY_ISSUE_REPORTED, sellerMessage, null, null, event.getOrderId(), null);
        } else {
            log.info("Seller {} reported the issue for delivery {}. No separate notification to seller.", event.getSellerId(), event.getDeliveryId());
        }

        log.info("Processed DeliveryIssueReportedEvent for delivery {}, issue: '{}'. Notified relevant parties.",
                event.getDeliveryId(), event.getIssueNotes());
    }

    @Override
    @Transactional
    public void processUserBanned(NotificationEvents.UserBannedEvent event) {
        log.debug("Processing UserBannedEvent for user ID: {}", event.getUserId());

        String banDurationDescription;
        if (event.getBanLevel() == 1) {
            banDurationDescription = "1 week";
        } else if (event.getBanLevel() >= 2) {
            banDurationDescription = "1 month";
        } else {
            banDurationDescription = "a specified period"; // Fallback
        }

        String formattedBanEndsAt = event.getBanEndsAt() != null ?
                event.getBanEndsAt().format(SHORT_DATE_TIME_FORMATTER) : "N/A";

        String message = String.format(
                "Account Notice: Due to %d payment defaults where you were the winning bidder, " +
                        "your ability to bid has been temporarily restricted for %s, until %s. " +
                        "Further defaults may lead to longer restrictions.",
                event.getTotalDefaults(),
                banDurationDescription,
                formattedBanEndsAt
        );

        // No auctionId, orderId, or commentId directly related to a ban notification itself
        saveAndSendNotification(event.getUserId(), TYPE_USER_BANNED, message, null, null, null, null);

        log.info("Processed UserBannedEvent for user {}, notified about ban until {}.",
                event.getUserId(), formattedBanEndsAt);
    }

    @Override
    @Transactional
    public void processDeliveryReturnRequested(DeliveryEvents.DeliveryReturnRequestedEventDto event) {
        log.debug("Processing DeliveryReturnRequestedEvent for order ID: {}", event.getOrderId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);

        // Fetch buyer's username for a friendlier message
        Map<String, String> userNames = getUsernamesFromIds(Collections.singletonList(event.getBuyerId()));
        String buyerUsername = userNames.getOrDefault(event.getBuyerId(), "A buyer");

        // Notify the SELLER that a return has been requested.
        String sellerMessage = String.format(
                "Return Request for order #%s: Buyer %s has requested to return '%s'. Reason: %s. Please go to the order page to manage this return.",
                orderIdShort,
                buyerUsername,
                productInfo,
                truncate(event.getReason(), 60)
        );

        saveAndSendNotification(event.getSellerId(), TYPE_DELIVERY_RETURN_REQUESTED, sellerMessage, null, null, event.getOrderId(), null);
        log.info("Processed DeliveryReturnRequestedEvent for order {}, notified seller {}.", event.getOrderId(), event.getSellerId());
    }

    @Override
    @Transactional
    public void processDeliveryReturnApproved(DeliveryEvents.DeliveryReturnApprovedEventDto event) {
        log.debug("Processing DeliveryReturnApprovedEvent for order ID: {}", event.getOrderId());
        String productInfo = truncate(event.getProductInfoSnapshot(), 50);
        String orderIdShort = event.getOrderId().toString().substring(0, 8);

        // Notify the BUYER that their return has been approved.
        String buyerMessage = String.format(
                "Your return request for order #%s ('%s') has been approved by the seller. They are now awaiting the item's arrival to process your refund.",
                orderIdShort,
                productInfo
        );

        saveAndSendNotification(event.getBuyerId(), TYPE_DELIVERY_RETURN_APPROVED, buyerMessage, null, null, event.getOrderId(), null);
        log.info("Processed DeliveryReturnApprovedEvent for order {}, notified buyer {}.", event.getOrderId(), event.getBuyerId());
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
         sendUnreadCountUpdate(userId);
        return updatedCount;
    }

    /**
     * Adds a follow relationship
     *
     * @param userId
     * @param auctionId
     * @param auctionType
     */
    @Override
    @Transactional
    public void followAuction(String userId, UUID auctionId, String auctionType) {
        log.info("User {} attempting to follow auction {} (type: {})", userId, auctionId, auctionType);
        // Use exists check for idempotency (don't create duplicates)
        if (!auctionFollowerRepository.existsByUserIdAndAuctionId(userId, auctionId)) {
            AuctionFollower follower = AuctionFollower.builder()
                    .userId(userId)
                    .auctionId(auctionId)
                    .auctionType(auctionType) // Store the type
                    .build();
            auctionFollowerRepository.save(follower);
            log.info("User {} successfully followed auction {}", userId, auctionId);
        } else {
            log.debug("User {} already following auction {}", userId, auctionId);
        }
    }


    /**
     * Removes a follow relationship
     *
     * @param userId
     * @param auctionId
     */
    @Override
    @Transactional
    public void unfollowAuction(String userId, UUID auctionId) {
        log.info("User {} attempting to unfollow auction {}", userId, auctionId);
        // Deleting by composite key is efficient
        long deletedCount = auctionFollowerRepository.deleteByUserIdAndAuctionId(userId, auctionId);
        if (deletedCount > 0) {
            log.info("User {} successfully unfollowed auction {}", userId, auctionId);
        } else {
            log.debug("User {} was not following auction {} or already unfollowed.", userId, auctionId);
        }
    }

    /**
     * Gets the set of auction IDs followed by a user
     *
     * @param userId
     */
    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getFollowedAuctionIds(String userId) {
        log.debug("Fetching followed auction IDs for user {}", userId);
        return auctionFollowerRepository.findAuctionIdsByUserId(userId);
    }

    /**
     * Gets user IDs following a specific auction (used internally)
     *
     * @param auctionId
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getFollowersForAuction(UUID auctionId) {
        log.debug("Fetching followers for auction {}", auctionId);
        return auctionFollowerRepository.findUserIdsByAuctionId(auctionId);
    }



    /**
     * Retrieves details for auctions followed by a user, supporting filtering and pagination.
     * This orchestrates calls to auction services.
     *
     * @param userId
     * @param status
     * @param ended
     * @param categoryIds
     * @param from
     * @param pageable
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FollowingAuctionSummaryDto> getFollowingAuctions(
            String userId,
            AuctionStatus status,
            Boolean ended,
            Set<Long> categoryIds,
            LocalDateTime from,
            Pageable pageable
    ) {
        log.info("Service fetching following auctions for user {}: status={}, ended={}, cats={}, from={}, page={}",
                userId, status, ended, categoryIds, from, pageable);

        // 1. Get ALL followed auction IDs and Types for the user
        List<AuctionFollower> followed = auctionFollowerRepository.findByUserId(userId); // Fetch full follower objects
        if (followed.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. Separate IDs by type
        Set<UUID> liveAuctionIds = followed.stream()
                .filter(f -> "LIVE".equalsIgnoreCase(f.getAuctionType()))
                .map(AuctionFollower::getAuctionId)
                .collect(Collectors.toSet());

        Set<UUID> timedAuctionIds = followed.stream()
                .filter(f -> "TIMED".equalsIgnoreCase(f.getAuctionType()))
                .map(AuctionFollower::getAuctionId)
                .collect(Collectors.toSet());

        // 3. Fetch Summaries via Feign Clients (in parallel eventually?)
        List<FollowingAuctionSummaryDto> combinedSummaries = new ArrayList<>();

        // Fetch Live Auction Summaries
        if (!liveAuctionIds.isEmpty()) {
            try {
                List<LiveAuctionSummaryDto> liveSummaries = liveAuctionServiceClient.getAuctionSummariesByIds(liveAuctionIds);
                liveSummaries.forEach(dto -> combinedSummaries.add(notificationMapper.mapToCommonSummary(dto, "LIVE")));
                log.debug("Fetched {} live auction summaries", liveSummaries.size());
            } catch (Exception e) {
                log.error("Failed to fetch live auction summaries for user {}: {}", userId, e.getMessage());
                // Decide how to handle partial failure - continue or throw? Continue for now.
            }
        }

        // Fetch Timed Auction Summaries
        if (!timedAuctionIds.isEmpty()) {
            try {
                List<TimedAuctionSummaryDto> timedSummaries = timedAuctionServiceClient.getAuctionSummariesByIds(timedAuctionIds);
                timedSummaries.forEach(dto -> combinedSummaries.add(notificationMapper.mapToCommonSummary(dto, "TIMED")));
                log.debug("Fetched {} timed auction summaries", timedSummaries.size());
            } catch (Exception e) {
                log.error("Failed to fetch timed auction summaries for user {}: {}", userId, e.getMessage());
            }
        }

        // 4. Apply Filtering (in memory)
        Stream<FollowingAuctionSummaryDto> filteredStream = combinedSummaries.stream();

        // Status/Ended Filter
        final Set<AuctionStatus> endedStatuses = Set.of(AuctionStatus.SOLD, AuctionStatus.RESERVE_NOT_MET, AuctionStatus.CANCELLED);
        if (Boolean.TRUE.equals(ended)) {
            filteredStream = filteredStream.filter(a -> endedStatuses.contains(a.getStatus()));
        } else if (status != null) {
            filteredStream = filteredStream.filter(a -> a.getStatus() == status);
        }

        // 'From' Time Filter (based on endTime for relevance?)
        if (from != null) {
            // Assuming 'from' means auctions ending *after* this time, or starting after? Let's filter by endTime > from
            filteredStream = filteredStream.filter(a -> a.getEndTime() != null && a.getEndTime().isAfter(from));
        }

        // Category Filter - OMITTED for now as Summary DTOs don't contain categories.
        // Would require fetching categories or adding them to summary DTOs + batch endpoints.
        if (categoryIds != null && !categoryIds.isEmpty()) {
            log.debug("Applying category filter with IDs: {} for user {}", categoryIds, userId);
            filteredStream = filteredStream.filter(summary -> {
                Set<Long> summaryCategoryIds = summary.getCategoryIds();
                if (summaryCategoryIds == null || summaryCategoryIds.isEmpty()) {
                    return false; // Auction has no categories listed, so it can't match the filter
                }
                // Check if any of the auction's categories are in the selected categoryIds from the request
                return summaryCategoryIds.stream().anyMatch(categoryIds::contains);
            });
        } else {
            log.debug("No category filter applied for user {}", userId);
        }


        List<FollowingAuctionSummaryDto> filteredList = filteredStream.collect(Collectors.toList());

        // 5. Apply Sorting (in memory - less efficient than DB sort)
        if (pageable.getSort().isSorted()) {
            Comparator<FollowingAuctionSummaryDto> comparator = Comparator.comparing(FollowingAuctionSummaryDto::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())); // Default sort
            // Basic sort handling - can be expanded
            Optional<Sort.Order> orderOpt = pageable.getSort().stream().findFirst();
            if(orderOpt.isPresent()) {
                Sort.Order order = orderOpt.get();
                boolean ascending = order.isAscending();
                // Add more sortable properties if needed
                if ("endTime".equalsIgnoreCase(order.getProperty())) {
                    comparator = Comparator.comparing(FollowingAuctionSummaryDto::getEndTime, Comparator.nullsLast(Comparator.naturalOrder()));
                } else if ("currentBid".equalsIgnoreCase(order.getProperty())) {
                    comparator = Comparator.comparing(FollowingAuctionSummaryDto::getCurrentBid, Comparator.nullsLast(Comparator.naturalOrder()));
                } // Add others
                if (!ascending) {
                    comparator = comparator.reversed();
                }
                filteredList.sort(comparator);
            }
        } else {
            // Default sort if none provided by Pageable (e.g., endTime descending)
            filteredList.sort(Comparator.comparing(FollowingAuctionSummaryDto::getEndTime, Comparator.nullsLast(Comparator.reverseOrder())));
        }


        // 6. Apply Pagination (in memory)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());

        List<FollowingAuctionSummaryDto> pageContent = (start > filteredList.size()) ? Collections.emptyList() : filteredList.subList(start, end);

        // 7. Create and Return Page object
        return new PageImpl<>(pageContent, pageable, filteredList.size());
    }

    private Map<String, String> getUsernamesFromIds(List<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        try {
            // Remove duplicates and nulls before making the client call
            List<String> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if(distinctIds.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, UserBasicInfoDto> userInfoMap = userServiceClient.getUsersBasicInfoByIds(distinctIds);

            return userInfoMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().getUsername()
                    ));
        } catch (Exception e) {
            log.error("Failed to fetch usernames for IDs: {}. Error: {}", userIds, e.getMessage());
            return Collections.emptyMap();
        }
    }


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
    private void saveAndSendNotification(String userId, String type, String message, UUID auctionId, String auctionType, UUID orderId, Long commentId) {
        try {
            // 1. Create and Save Notification Entity
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .message(message)
                    .isRead(false)
                    .relatedAuctionId(auctionId)
                    .relatedAuctionType(auctionType)
                    .relatedOrderId(orderId)
                    .relatedCommentId(commentId)
                    .build();
            Notification savedNotification = notificationRepository.save(notification);
            log.debug("Saved notification ID {} for user {}", savedNotification.getId(), userId);

            // 2. Create DTO for WebSocket Payload
            NotificationDto notificationDto = NotificationDto.builder()
                    .type(savedNotification.getType())
                    .message(savedNotification.getMessage())
                    .timestamp(savedNotification.getCreatedAt())
                    .relatedAuctionId(savedNotification.getRelatedAuctionId())
                    .relatedAuctionType(savedNotification.getRelatedAuctionType())
                    .relatedOrderId(savedNotification.getRelatedOrderId())
                    .isRead(savedNotification.isRead())
                    .details(null)
                    .build();

            // 3. Send via WebSocket to specific user destination
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(
                    userId,
                    destination,
                    notificationDto
            );
            log.info("Sent WebSocket notification type '{}' to user {}, destination '/user/{}/{}'", type, userId, userId, destination);

            // 4.  Trigger Email Notification
            sendEmailNotification(userId, message);

        } catch (Exception e) {
            log.error("Failed to save or send notification type '{}' for user {}: {}", type, userId, e.getMessage(), e);
        }
    }

    //  Email Sending Logic ---
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
}