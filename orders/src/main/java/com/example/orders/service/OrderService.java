package com.example.orders.service; // Suggested package

import com.example.orders.dto.event.AuctionSoldEventDto;
import com.example.orders.dto.event.PaymentFailedEventDto;
import com.example.orders.dto.event.PaymentSucceededEventDto;
import com.example.orders.dto.request.SellerDecisionDto;
import com.example.orders.dto.response.OrderDetailDto;
import com.example.orders.dto.response.OrderSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    /**
     * Processes an event indicating an auction has been won,
     * creates an order, and initiates the payment process.
     * @param eventDto The event data.
     */
    void processAuctionSoldEvent(AuctionSoldEventDto eventDto);

    /**
     * Handles the logic when a payment deadline for an order expires.
     * @param orderId The ID of the order.
     * @param paymentOfferAttempt The attempt number (1 for initial winner, 2 for 2nd offer, etc.)
     */
    void handlePaymentTimeout(UUID orderId, int paymentOfferAttempt);

    /**
     * Processes a decision made by a seller regarding an order
     * that is awaiting their action (e.g., after a winner defaults).
     * @param orderId The ID of the order.
     * @param authenticatedSellerId The ID of the seller making the request (from auth context).
     * @param decisionDto The seller's decision.
     */
    void processSellerDecision(UUID orderId, String authenticatedSellerId, SellerDecisionDto decisionDto);

    /**
     * Processes a successful payment event from the Payment Service.
     * @param eventDto Details of the successful payment.
     */
    void processPaymentSuccess(PaymentSucceededEventDto eventDto);

    /**
     * Processes a failed payment event from the Payment Service.
     * @param eventDto Details of the failed payment.
     */
    void processPaymentFailure(PaymentFailedEventDto eventDto);

    /**
     * Retrieves a paginated list of orders for the authenticated user.
     * @param userId The ID of the authenticated user.
     * @param statusFilter Optional status to filter by (e.g., "PENDING_PAYMENT").
     * @param pageable Pagination information.
     * @return A page of OrderSummaryDto.
     */
    Page<OrderSummaryDto> getMyOrders(String userId, String statusFilter, Pageable pageable);

    /**
     * Retrieves the details of a specific order for an authorized user.
     * @param orderId The ID of the order.
     * @param userId The ID of the authenticated user (for authorization).
     * @return OrderDetailDto.
     */
    OrderDetailDto getOrderDetailsForUser(UUID orderId, String userId);

    /**
     * Allows a buyer (current bidder) to cancel their payment obligation for an order.
     * @param orderId The ID of the order.
     * @param buyerId The ID of the authenticated buyer.
     */
    void processBuyerCancelPaymentAttempt(UUID orderId, String buyerId);
}