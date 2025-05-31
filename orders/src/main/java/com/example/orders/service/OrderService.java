package com.example.orders.service; // Suggested package

import com.example.orders.dto.event.*;
import com.example.orders.dto.request.SellerDecisionDto;
import com.example.orders.dto.response.OrderDetailDto;
import com.example.orders.dto.response.OrderSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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

    /**
     * Retrieves a paginated list of orders for a seller.
     * @param sellerId The ID of the authenticated seller.
     * @param statusFilter Optional status to filter by (e.g., "PENDING_PAYMENT").
     * @param pageable Pagination information.
     * @return A page of OrderSummaryDto.
     */
    Page<OrderSummaryDto> getMySales(String sellerId, String statusFilter, Pageable pageable);

    /**
     * Allows the seller to cancel an order before the buyer has made payment.
     * Typically used if the seller cannot fulfill the order or if there is an issue before payment is completed.
     * @param orderId The ID of the order to cancel.
     * @param sellerId The ID of the authenticated seller initiating the cancellation.
     * @param reason The reason provided by the seller for the cancellation.
     */
    void processSellerInitiatedCancellation(UUID orderId, String sellerId, String reason);

    /**
     * Allows the seller to confirm that payment has been received and the order is ready to be fulfilled (e.g., prepared for shipping).
     * This should be called after payment is confirmed to proceed with order fulfillment.
     * @param orderId The ID of the order to fulfill.
     * @param sellerId The ID of the authenticated seller confirming fulfillment.
     */
    void confirmOrderFulfillment(UUID orderId, String sellerId);

    /**
     * Processes the confirmation of delivery receipt by the buyer.
     * This is typically called when the buyer confirms they have received the order.
     * @param orderId The ID of the order.
     * @param buyerId The ID of the authenticated buyer confirming receipt.
     * @param confirmationTimestamp The timestamp of when the confirmation was made.
     */
    void processOrderCompletionByBuyer(UUID orderId, String buyerId, LocalDateTime confirmationTimestamp);

    /**
     * Processes an event indicating that an order has been returned.
     * This is typically triggered when a buyer returns an item after receiving it.
     * @param event The event data containing details about the returned order.
     */
    void processRefundRequiredForReturnEvent(RefundRequiredForReturnEventDto event);

    /**
     * Processes a successful refund event.
     * This is typically triggered when a refund is successfully processed for an order.
     * @param event The event data containing details about the successful refund.
     */
    void processRefundSuccess(RefundSucceededEventDto event);

    /**
     * Processes a failed refund event.
     * This is typically triggered when a refund attempt fails for an order.
     * @param event The event data containing details about the failed refund.
     */
    void processRefundFailure(RefundFailedEventDto event);
}