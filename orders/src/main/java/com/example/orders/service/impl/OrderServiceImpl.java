package com.example.orders.service.impl;

import com.example.orders.commands.OrderWorkflowCommands.CheckPaymentTimeoutCommand;
import com.example.orders.config.OrderPaymentProperties;
import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.*;
import com.example.orders.dto.response.OrderDetailDto;
import com.example.orders.dto.response.OrderSummaryDto;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.entity.SellerDecisionType; // Make sure this is imported if used directly
import com.example.orders.mapper.OrderMapper;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.OrderService;
import com.example.orders.dto.request.SellerDecisionDto; // Import for processSellerDecision
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderPaymentProperties paymentProperties;
    private final RabbitTemplate rabbitTemplate;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public void processAuctionSoldEvent(AuctionSoldEventDto eventDto) {
        log.info("Processing AuctionSoldEvent for auction ID: {}", eventDto.getAuctionId());

        if (orderRepository.findByAuctionId(eventDto.getAuctionId()).isPresent()) {
            log.warn("Order for auction ID {} already exists. Skipping event processing. Event ID: {}",
                    eventDto.getAuctionId(), eventDto.getEventId());
            return;
        }

        Duration paymentDuration;
        if ("LIVE".equalsIgnoreCase(eventDto.getAuctionType())) {
            paymentDuration = paymentProperties.getLiveAuctionWinnerDuration();
        } else if ("TIMED".equalsIgnoreCase(eventDto.getAuctionType())) {
            paymentDuration = paymentProperties.getTimedAuctionWinnerDuration();
        } else {
            log.warn("Unknown auction type '{}' for auction ID {}. Using default timed auction duration.",
                    eventDto.getAuctionType(), eventDto.getAuctionId());
            paymentDuration = paymentProperties.getTimedAuctionWinnerDuration();
        }
        LocalDateTime paymentDeadline = LocalDateTime.now().plus(paymentDuration);
        int currentPaymentOfferAttempt = 1;

        log.info("RECEIVE ORDER!: {}", eventDto.toString());

        Order order = Order.builder()
                .auctionId(eventDto.getAuctionId())
                .productId(eventDto.getProductId())
                .productTitleSnapshot(eventDto.getProductTitleSnapshot())
                .productImageUrlSnapshot(eventDto.getProductImageUrlSnapshot())
                .sellerId(eventDto.getSellerId())
                .sellerUsernameSnapshot(eventDto.getSellerUsernameSnapshot())
                .auctionType(eventDto.getAuctionType()) // *** POPULATE auctionType HERE ***
                .initialWinnerId(eventDto.getWinnerId())
                .initialWinningBidAmount(eventDto.getWinningBid())
                .currency("VND")
                .reservePriceSnapshot(eventDto.getReservePrice())
                .eligibleSecondBidderId(eventDto.getSecondHighestBidderId())
                .eligibleSecondBidAmount(eventDto.getSecondHighestBidAmount())
                .eligibleThirdBidderId(eventDto.getThirdHighestBidderId())
                .eligibleThirdBidAmount(eventDto.getThirdHighestBidAmount())
                .currentBidderId(eventDto.getWinnerId())
                .currentAmountDue(eventDto.getWinningBid())
                .paymentDeadline(paymentDeadline)
                .orderStatus(OrderStatus.AWAITING_WINNER_PAYMENT)
                .paymentOfferAttempt(currentPaymentOfferAttempt)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Created new order ID {} for auction ID {}. Auction Type: {}",
                savedOrder.getId(), savedOrder.getAuctionId(), savedOrder.getAuctionType());

        schedulePaymentTimeoutCheck(savedOrder.getId(), paymentDeadline, currentPaymentOfferAttempt);
        // Use savedOrder.getAuctionType() for publishing the event
        publishOrderCreatedEvent(savedOrder); // Removed auctionType param, will get from order object

        log.info("Successfully processed auction sold event for order ID {}", savedOrder.getId());
    }

    @Override
    @Transactional
    public void handlePaymentTimeout(UUID orderId, int paymentOfferAttempt) {
        log.info("Handling payment timeout for order ID: {}, attempt: {}", orderId, paymentOfferAttempt);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for payment timeout check: {}", orderId);
                    return new NoSuchElementException("Order not found: " + orderId);
                });

        if (order.getOrderStatus() == OrderStatus.PAYMENT_SUCCESSFUL ||
                order.getOrderStatus() == OrderStatus.AWAITING_SHIPMENT ||
                order.getOrderStatus().name().startsWith("ORDER_CANCELLED")) {
            log.info("Payment for order {} was already processed (status: {}) or order cancelled. No action needed for timeout.",
                    orderId, order.getOrderStatus());
            return;
        }

        if (order.getPaymentOfferAttempt() != paymentOfferAttempt) {
            log.warn("Stale payment timeout check for order {}. Current attempt: {}, Timeout attempt: {}. Ignoring.",
                    orderId, order.getPaymentOfferAttempt(), paymentOfferAttempt);
            return;
        }

        log.info("Payment timed out for order {}, bidder {}, attempt {}.",
                orderId, order.getCurrentBidderId(), paymentOfferAttempt);

        publishUserPaymentDefaultedEvent(order);

        if (paymentOfferAttempt == 1) {
            order.setOrderStatus(OrderStatus.PAYMENT_WINDOW_EXPIRED_WINNER);
            order.setOrderStatus(OrderStatus.AWAITING_SELLER_DECISION);
            log.info("Order {} status set to AWAITING_SELLER_DECISION. Winner timed out.", orderId);
            publishSellerDecisionRequiredEvent(order);
        } else if (paymentOfferAttempt == 2) {
            order.setOrderStatus(OrderStatus.PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER);
            log.info("Second offered bidder timed out for order {}.", orderId);
            if (order.getEligibleThirdBidderId() != null && order.getEligibleThirdBidAmount() != null) {
                log.info("Offering order {} to third eligible bidder: {}", orderId, order.getEligibleThirdBidderId());
                order.setCurrentBidderId(order.getEligibleThirdBidderId());
                order.setCurrentAmountDue(order.getEligibleThirdBidAmount());
                order.setPaymentOfferAttempt(3);
                Duration nextBidderPaymentDuration = paymentProperties.getNextBidderDuration();
                LocalDateTime newDeadline = LocalDateTime.now().plus(nextBidderPaymentDuration);
                order.setPaymentDeadline(newDeadline);
                order.setOrderStatus(OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT);

                schedulePaymentTimeoutCheck(order.getId(), newDeadline, 3);
                publishPaymentDueEvent(order); // auctionType will be fetched from order object
            } else {
                log.info("No eligible third bidder for order {}. Setting to NO_PAYMENT_FINAL.", orderId);
                order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
            }
        } else if (paymentOfferAttempt >= 3) {
            log.info("Third (or later) offered bidder timed out for order {}. Setting to NO_PAYMENT_FINAL.", orderId);
            order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} updated. New status: {}", orderId, savedOrder.getOrderStatus());

        if (savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL ||
                savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_SYSTEM) {
            publishOrderCancelledEvent(savedOrder, "No payment received after all attempts or no eligible bidders remaining.");
        }
    }

    @Override
    @Transactional
    public void processSellerDecision(UUID orderId, String authenticatedSellerId, SellerDecisionDto decisionDto) {
        log.info("Processing seller decision for order {}, seller {}, decision: {}",
                orderId, authenticatedSellerId, decisionDto.getDecisionType());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (!order.getSellerId().equals(authenticatedSellerId)) {
            log.warn("Unauthorized attempt by user {} to make decision on order {} owned by seller {}.",
                    authenticatedSellerId, orderId, order.getSellerId());
            throw new SecurityException("User not authorized to make a decision on this order.");
        }

        if (order.getOrderStatus() != OrderStatus.AWAITING_SELLER_DECISION) {
            log.warn("Order {} is not in AWAITING_SELLER_DECISION state. Current status: {}. Cannot process seller decision.",
                    orderId, order.getOrderStatus());
            throw new IllegalStateException("Order is not awaiting seller decision.");
        }

        order.setSellerDecision(decisionDto.getDecisionType());

        switch (decisionDto.getDecisionType()) {
            case OFFER_TO_NEXT_BIDDER:
                if (order.getEligibleSecondBidderId() != null && order.getEligibleSecondBidAmount() != null) {
                    log.info("Seller chose to offer order {} to second eligible bidder: {}", orderId, order.getEligibleSecondBidderId());
                    order.setCurrentBidderId(order.getEligibleSecondBidderId());
                    order.setCurrentAmountDue(order.getEligibleSecondBidAmount());
                    order.setPaymentOfferAttempt(2);
                    Duration nextBidderPaymentDuration = paymentProperties.getNextBidderDuration();
                    LocalDateTime newDeadline = LocalDateTime.now().plus(nextBidderPaymentDuration);
                    order.setPaymentDeadline(newDeadline);
                    order.setOrderStatus(OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT);

                    schedulePaymentTimeoutCheck(order.getId(), newDeadline, 2);
                    publishPaymentDueEvent(order); // auctionType will be fetched from order object
                } else {
                    log.warn("Seller chose to offer to next bidder for order {}, but no eligible second bidder found. Defaulting to cancel.", orderId);
                    order.setOrderStatus(OrderStatus.ORDER_CANCELLED_SYSTEM);
                    publishOrderCancelledEvent(order, "No eligible second bidder to offer to, as per seller's attempt.");
                }
                break;

            case REOPEN_AUCTION:
                log.info("Seller chose to reopen auction for order {}", orderId);
                order.setOrderStatus(OrderStatus.AUCTION_REOPEN_INITIATED);
                publishAuctionReopenRequestedEvent(order);
                break;

            case CANCEL_SALE:
                log.info("Seller chose to cancel the sale for order {}", orderId);
                order.setOrderStatus(OrderStatus.ORDER_CANCELLED_BY_SELLER);
                publishOrderCancelledEvent(order, "Sale cancelled by seller decision.");
                break;

            default:
                log.error("Unknown seller decision type: {} for order {}", decisionDto.getDecisionType(), orderId);
                throw new IllegalArgumentException("Invalid seller decision type.");
        }

        orderRepository.save(order);
        log.info("Seller decision processed for order {}. New status: {}", orderId, order.getOrderStatus());
    }

    /**
     * Processes a successful payment event from the Payment Service.
     *
     * @param eventDto Details of the successful payment.
     */
    @Override
    @Transactional
    public void processPaymentSuccess(PaymentSucceededEventDto eventDto) {
        log.info("Processing successful payment for order ID: {}, paymentIntentId: {}",
                eventDto.getOrderId(), eventDto.getPaymentIntentId());

        Order order = orderRepository.findById(eventDto.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found for payment success event: {}", eventDto.getOrderId());
                    // This shouldn't happen if Payment Service got orderId from Orders service correctly
                    return new NoSuchElementException("Order not found: " + eventDto.getOrderId());
                });

        // Idempotency: If already marked as successful or further, just log.
        if (order.getOrderStatus() == OrderStatus.PAYMENT_SUCCESSFUL ||
                order.getOrderStatus() == OrderStatus.AWAITING_SHIPMENT /* Add other post-payment states */) {
            log.warn("Order {} already processed for successful payment. Current status: {}. Ignoring event.",
                    order.getId(), order.getOrderStatus());
            return;
        }

        // Validate that the payment is for the current bidder on the order
        if (!order.getCurrentBidderId().equals(eventDto.getUserId())) { // Assuming DTO has userId who paid
            log.error("Payment success event for order {} has mismatched user. Order current bidder: {}, Event user: {}. Critical error!",
                    order.getId(), order.getCurrentBidderId(), eventDto.getUserId());
            // This is a serious issue, might require manual intervention or specific error handling.
            // For now, we'll throw an exception.
            throw new IllegalStateException("Payment user mismatch for order " + order.getId());
        }

        order.setOrderStatus(OrderStatus.PAYMENT_SUCCESSFUL); // Or directly AWAITING_SHIPMENT
        order.setPaymentTransactionRef(eventDto.getPaymentIntentId()); // Store Stripe's PaymentIntent ID
        // Potentially store eventDto.getChargeId() if you add that to Order entity
        // order.setActualAmountPaid(eventDto.getAmountPaid()); // If you store this

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to PAYMENT_SUCCESSFUL. PaymentIntentId: {}",
                savedOrder.getId(), savedOrder.getPaymentTransactionRef());

        // Publish event for Delivery Service / Notifications
        publishOrderReadyForShippingEvent(savedOrder);
    }

    /**
     * Processes a failed payment event from the Payment Service.
     *
     * @param eventDto Details of the failed payment.
     */
    @Override
    @Transactional
    public void processPaymentFailure(PaymentFailedEventDto eventDto) {
        log.warn("Processing failed payment for order ID: {}, paymentIntentId: {}, reason: {}",
                eventDto.getOrderId(), eventDto.getPaymentIntentId(), eventDto.getFailureMessage());

        Order order = orderRepository.findById(eventDto.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found for payment failure event: {}", eventDto.getOrderId());
                    return new NoSuchElementException("Order not found: " + eventDto.getOrderId());
                });

        // If order is already successfully paid or cancelled, ignore this failure event.
        if (order.getOrderStatus() == OrderStatus.PAYMENT_SUCCESSFUL ||
                order.getOrderStatus() == OrderStatus.AWAITING_SHIPMENT ||
                order.getOrderStatus().name().startsWith("ORDER_CANCELLED")) {
            log.warn("Order {} already in a final state ({}). Ignoring payment failure event.",
                    order.getId(), order.getOrderStatus());
            return;
        }

        // Validate that the payment failure is for the current bidder on the order
        if (!order.getCurrentBidderId().equals(eventDto.getUserId())) { // Assuming DTO has userId
            log.error("Payment failure event for order {} has mismatched user. Order current bidder: {}, Event user: {}. Ignoring.",
                    order.getId(), order.getCurrentBidderId(), eventDto.getUserId());
            return;
        }

        log.info("Payment failed for order {}, current bidder {}, attempt {}. Reason: {}",
                order.getId(), order.getCurrentBidderId(), order.getPaymentOfferAttempt(), eventDto.getFailureMessage());

        // At this point, payment has failed for the currentBidderId at the currentAttempt.
        // Logic here is very similar to handlePaymentTimeout.
        // We can refactor this into a common method if the logic is identical.

        publishUserPaymentDefaultedEvent(order); // User defaulted due to payment failure

        int currentAttempt = order.getPaymentOfferAttempt();
        String originalAuctionType = order.getAuctionType(); // Now available on order entity

        if (currentAttempt == 1) { // Winner's payment failed
            order.setOrderStatus(OrderStatus.AWAITING_SELLER_DECISION); // After any payment fail by winner
            log.info("Order {} status set to AWAITING_SELLER_DECISION. Winner's payment failed.", order.getId());
            publishSellerDecisionRequiredEvent(order);

        } else if (currentAttempt == 2) { // Second bidder's payment failed
            log.info("Second offered bidder's payment failed for order {}.", order.getId());
            if (order.getEligibleThirdBidderId() != null && order.getEligibleThirdBidAmount() != null) {
                log.info("Offering order {} to third eligible bidder: {}", order.getId(), order.getEligibleThirdBidderId());
                order.setCurrentBidderId(order.getEligibleThirdBidderId());
                order.setCurrentAmountDue(order.getEligibleThirdBidAmount());
                order.setPaymentOfferAttempt(3);
                Duration nextBidderPaymentDuration = paymentProperties.getNextBidderDuration();
                LocalDateTime newDeadline = LocalDateTime.now().plus(nextBidderPaymentDuration);
                order.setPaymentDeadline(newDeadline);
                order.setOrderStatus(OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT);

                schedulePaymentTimeoutCheck(order.getId(), newDeadline, 3); // Reschedule timeout
                publishPaymentDueEvent(order); // Notify 3rd bidder
            } else {
                log.info("No eligible third bidder for order {}. Cancelling order due to payment failure.", order.getId());
                order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
            }
        } else if (currentAttempt >= 3) { // Third (or later) bidder's payment failed
            log.info("Third (or later) offered bidder's payment failed for order {}. Cancelling order.", order.getId());
            order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} updated after payment failure. New status: {}", order.getId(), savedOrder.getOrderStatus());

        if (savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL ||
                savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_SYSTEM) {
            publishOrderCancelledEvent(savedOrder, "Payment failed and no further options.");
        }
    }

    /**
     * Retrieves a paginated list of orders for the authenticated user.
     *
     * @param userId       The ID of the authenticated user.
     * @param statusFilter Optional status to filter by (e.g., "PENDING_PAYMENT").
     * @param pageable     Pagination information.
     * @return A page of OrderSummaryDto.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getMyOrders(String userId, String statusFilter, Pageable pageable) {
        log.debug("Fetching orders for user: {} with status filter: {}", userId, statusFilter);
        // Logic: Find orders where userId is either initialWinnerId or currentBidderId
        // AND optionally filter by status.
        // This query might become complex. For now, let's assume a simpler query by currentBidderId.
        // Or, if you want to show all orders a user is involved in (as seller or potential buyer),
        // the query in OrderRepository would need to be more sophisticated.
        // For "my orders as buyer", currentBidderId is a good start.
        Page<Order> ordersPage;
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isEmpty()) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
                ordersPage = orderRepository.findByCurrentBidderIdAndOrderStatus(userId, status, pageable);
                // log all orders fetched
                log.info("Fetching orders for user {} with status {}. Total orders: {}", userId, status, ordersPage.getTotalElements());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter provided: {}. Fetching all for user.", statusFilter);
                ordersPage = orderRepository.findByCurrentBidderId(userId, pageable); // Needs this method in repo
            }
        } else {
            ordersPage = orderRepository.findByCurrentBidderId(userId, pageable); // Needs this method in repo
            // log all orders fetched for me
            log.info("Fetching all orders for user {}. Total orders: {}", userId, ordersPage.getTotalElements());
        }
        return ordersPage.map(orderMapper::toOrderSummaryDto); // Use your mapper
    }

    /**
     * Retrieves the details of a specific order for an authorized user.
     *
     * @param orderId The ID of the order.
     * @param userId  The ID of the authenticated user (for authorization).
     * @return OrderDetailDto.
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetailsForUser(UUID orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // Authorization: User must be the seller or the current bidder (or initial winner)
        if (!order.getSellerId().equals(userId) &&
                !order.getInitialWinnerId().equals(userId) && // Allow initial winner to see
                !order.getCurrentBidderId().equals(userId)) { // Allow current bidder to see
            log.warn("User {} not authorized to view order {}", userId, orderId);
            throw new SecurityException("User not authorized to view this order.");
        }
        return orderMapper.toOrderDetailDto(order); // Use your mapper
    }


    /**
     * Allows a buyer (current bidder) to cancel their payment obligation for an order.
     *
     * @param orderId The ID of the order.
     * @param buyerId The ID of the authenticated buyer.
     */
    @Override
    @Transactional
    public void processBuyerCancelPaymentAttempt(UUID orderId, String buyerId) {
        log.info("Buyer {} attempting to cancel payment for order {}", buyerId, orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        // 1. Authorization: Is the authenticated user the current bidder for this order?
        if (!order.getCurrentBidderId().equals(buyerId)) {
            log.warn("User {} is not the current bidder for order {}. Current bidder: {}. Cannot cancel.",
                    buyerId, orderId, order.getCurrentBidderId());
            throw new SecurityException("Not authorized to cancel this payment attempt.");
        }

        // 2. State Validation: Can this order's payment attempt be cancelled by the buyer?
        // Typically, if it's AWAITING_WINNER_PAYMENT or AWAITING_NEXT_BIDDER_PAYMENT
        if (!(order.getOrderStatus() == OrderStatus.AWAITING_WINNER_PAYMENT ||
                order.getOrderStatus() == OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT)) {
            log.warn("Order {} is not in a state where buyer can cancel payment. Current status: {}",
                    orderId, order.getOrderStatus());
            throw new IllegalStateException("Order payment cannot be cancelled in its current state.");
        }

        log.info("Buyer {} confirmed cancellation for order {}. Current attempt: {}",
                buyerId, orderId, order.getPaymentOfferAttempt());

        // Treat this as if the payment timed out for this user/attempt
        // The existing handlePaymentTimeout logic is well-suited if we just advance the state to "expired" for this user.
        // Or, we can have a more specific status like "CANCELLED_BY_BUYER_PENDING_PAYMENT" before moving to next state.

        // Option 1: Re-use timeout logic by setting status and calling it (might be too coupled)
        // Option 2: Replicate parts of the timeout logic here.

        publishUserPaymentDefaultedEvent(order); // Buyer is defaulting on this attempt

        int currentAttempt = order.getPaymentOfferAttempt();
        // String auctionType = order.getAuctionType(); // Already on order entity

        if (currentAttempt == 1) { // Winner cancelled their payment attempt
            order.setOrderStatus(OrderStatus.AWAITING_SELLER_DECISION); // After any payment fail/cancel by winner
            log.info("Order {} status set to AWAITING_SELLER_DECISION. Winner cancelled payment attempt.", orderId);
            publishSellerDecisionRequiredEvent(order);
        } else if (currentAttempt == 2) { // Second bidder cancelled
            log.info("Second offered bidder cancelled payment for order {}.", orderId);
            if (order.getEligibleThirdBidderId() != null && order.getEligibleThirdBidAmount() != null) {
                log.info("Offering order {} to third eligible bidder: {}", orderId, order.getEligibleThirdBidderId());
                order.setCurrentBidderId(order.getEligibleThirdBidderId());
                order.setCurrentAmountDue(order.getEligibleThirdBidAmount());
                order.setPaymentOfferAttempt(3);
                Duration nextBidderPaymentDuration = paymentProperties.getNextBidderDuration();
                LocalDateTime newDeadline = LocalDateTime.now().plus(nextBidderPaymentDuration);
                order.setPaymentDeadline(newDeadline);
                order.setOrderStatus(OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT);

                schedulePaymentTimeoutCheck(order.getId(), newDeadline, 3);
                publishPaymentDueEvent(order);
            } else {
                log.info("No eligible third bidder for order {}. Cancelling order due to buyer cancellation.", orderId);
                order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
            }
        } else if (currentAttempt >= 3) { // Third (or later) bidder cancelled
            log.info("Third (or later) offered bidder cancelled for order {}. Cancelling order.", orderId);
            order.setOrderStatus(OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} updated after buyer cancelled payment. New status: {}", orderId, savedOrder.getOrderStatus());

        if (savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_NO_PAYMENT_FINAL ||
                savedOrder.getOrderStatus() == OrderStatus.ORDER_CANCELLED_SYSTEM) {
            publishOrderCancelledEvent(savedOrder, "Buyer cancelled payment and no further options.");
        }
    }

    /**
     * Retrieves a paginated list of orders for a seller.
     *
     * @param sellerId     The ID of the authenticated seller.
     * @param statusFilter Optional status to filter by (e.g., "PENDING_PAYMENT").
     * @param pageable     Pagination information.
     * @return A page of OrderSummaryDto.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getMySales(String sellerId, String statusFilter, Pageable pageable) {
        log.debug("Fetching sales for seller: {} with status filter: {}", sellerId, statusFilter);
        Page<Order> salesPage;
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.isEmpty()) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
                salesPage = orderRepository.findBySellerIdAndOrderStatus(sellerId, status, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter provided for getMySales: {}. Fetching all for seller.", statusFilter);
                salesPage = orderRepository.findBySellerId(sellerId, pageable);
            }
        } else {
            salesPage = orderRepository.findBySellerId(sellerId, pageable);
        }
        return salesPage.map(orderMapper::toOrderSummaryDto);
    }


    // --- Private Helper Methods for Publishing Events and Scheduling ---

    private void schedulePaymentTimeoutCheck(UUID orderId, LocalDateTime deadline, int attemptNumber) {
        long delayMillis = Duration.between(LocalDateTime.now(), deadline).toMillis();
        if (delayMillis > 0) {
            CheckPaymentTimeoutCommand timeoutCommand = new CheckPaymentTimeoutCommand(orderId, deadline, attemptNumber);
            log.info("Scheduling payment timeout check for order {} (attempt {}) in {} ms. Deadline: {}",
                    orderId, attemptNumber, delayMillis, deadline);
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.ORDERS_SCHEDULE_EXCHANGE,
                        RabbitMqConfig.ORDER_PAYMENT_TIMEOUT_SCHEDULE_ROUTING_KEY,
                        timeoutCommand,
                        message -> {
                            message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMillis, Integer.MAX_VALUE));
                            return message;
                        }
                );
            } catch (Exception e) {
                log.error("Error scheduling payment timeout check for order {}: {}", orderId, e.getMessage(), e);
            }
        } else {
            log.warn("Calculated payment timeout delay for order {} (attempt {}) was not positive ({}ms). Deadline: {}. Consider immediate check or error.",
                    orderId, attemptNumber, delayMillis, deadline);
            // TODO: Implement immediate timeout processing if deadline is already past.
        }
    }

    // Called after initial order creation
    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEventDto event = OrderCreatedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .productId(order.getProductId())
                .productTitleSnapshot(order.getProductTitleSnapshot())
                .sellerId(order.getSellerId())
                .currentBidderId(order.getCurrentBidderId())
                .amountDue(order.getCurrentAmountDue())
                .currency(order.getCurrency())
                .paymentDeadline(order.getPaymentDeadline())
                .initialOrderStatus(order.getOrderStatus())
                .auctionType(order.getAuctionType()) // Now directly from order entity
                .build();

        log.info("Publishing OrderCreatedEvent for order ID: {}", order.getId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_CREATED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Error publishing OrderCreatedEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    // Called when offering to subsequent bidders or re-notifying about payment
    private void publishPaymentDueEvent(Order order) {
        OrderCreatedEventDto event = OrderCreatedEventDto.builder() // Using OrderCreatedEventDto as the structure
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .productId(order.getProductId())
                .productTitleSnapshot(order.getProductTitleSnapshot())
                .sellerId(order.getSellerId())
                .currentBidderId(order.getCurrentBidderId())
                .amountDue(order.getCurrentAmountDue())
                .currency(order.getCurrency())
                .paymentDeadline(order.getPaymentDeadline())
                .initialOrderStatus(order.getOrderStatus()) // Current status when payment is due
                .auctionType(order.getAuctionType()) // Now directly from order entity
                .build();

        log.info("Publishing PaymentDueEvent for order ID: {}, bidder: {}", order.getId(), order.getCurrentBidderId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_PAYMENT_DUE_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Error publishing PaymentDueEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void publishUserPaymentDefaultedEvent(Order order) {
        UserPaymentDefaultedEventDto event = UserPaymentDefaultedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .defaultedUserId(order.getCurrentBidderId())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .amountDefaulted(order.getCurrentAmountDue())
                .currency(order.getCurrency())
                .paymentOfferAttempt(order.getPaymentOfferAttempt())
                .build();

        log.info("Publishing UserPaymentDefaultedEvent for user {}, order {}", event.getDefaultedUserId(), event.getOrderId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.USER_EVENTS_EXCHANGE, // Assumes USER_EVENTS_EXCHANGE is defined
                    RabbitMqConfig.USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.error("Error publishing UserPaymentDefaultedEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void publishSellerDecisionRequiredEvent(Order order) {
        SellerDecisionRequiredEventDto event = SellerDecisionRequiredEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .productId(order.getProductId())
                .productTitleSnapshot(order.getProductTitleSnapshot())
                .sellerId(order.getSellerId())
                .defaultedBidderId(order.getInitialWinnerId()) // This event is specific to winner's default
                .defaultedBidAmount(order.getInitialWinningBidAmount())
                .canOfferToSecondBidder(order.getEligibleSecondBidderId() != null && order.getEligibleSecondBidAmount() != null)
                .eligibleSecondBidderId(order.getEligibleSecondBidderId())
                .eligibleSecondBidAmount(order.getEligibleSecondBidAmount())
                .paymentOfferAttemptOfDefaultingBidder(1) // Specific to winner's default (attempt 1)
                .build();

        log.info("Publishing SellerDecisionRequiredEvent for order {}", event.getOrderId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_SELLER_DECISION_REQUIRED_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.error("Error publishing SellerDecisionRequiredEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void publishOrderReadyForShippingEvent(Order order) {
        OrderReadyForShippingEventDto event = OrderReadyForShippingEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .productId(order.getProductId())
                .productTitleSnapshot(order.getProductTitleSnapshot())
                .sellerId(order.getSellerId())
                .buyerId(order.getCurrentBidderId()) // This is the one who successfully paid
                .amountPaid(order.getCurrentAmountDue().longValueExact()) // Assuming amount is stored as such or from payment event
                .currency(order.getCurrency())
                .paymentTransactionRef(order.getPaymentTransactionRef())
                .build();

        log.info("Publishing OrderReadyForShippingEvent for order ID: {}", order.getId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.error("Error publishing OrderReadyForShippingEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        OrderCancelledEventDto event = OrderCancelledEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .productId(order.getProductId())
                .sellerId(order.getSellerId())
                .currentBidderIdAtCancellation(order.getCurrentBidderId())
                .finalOrderStatus(order.getOrderStatus())
                .cancellationReason(reason)
                .build();

        log.info("Publishing OrderCancelledEvent for order {} with reason: {}", event.getOrderId(), reason);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_CANCELLED_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.error("Error publishing OrderCancelledEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    private void publishAuctionReopenRequestedEvent(Order order) {
        AuctionReopenRequestedEventDto event = AuctionReopenRequestedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getId())
                .auctionId(order.getAuctionId())
                .sellerId(order.getSellerId())
                .build();

        log.info("Publishing AuctionReopenRequestedEvent for auction ID: {}", order.getAuctionId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ORDERS_EVENTS_EXCHANGE,
                    RabbitMqConfig.ORDER_EVENT_AUCTION_REOPEN_REQUESTED_ROUTING_KEY,
                    event
            );
        } catch (Exception e) {
            log.error("Error publishing AuctionReopenRequestedEvent for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }
}