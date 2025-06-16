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

    void processAuctionSoldEvent(AuctionSoldEventDto eventDto);

    void handlePaymentTimeout(UUID orderId, int paymentOfferAttempt);

    void processSellerDecision(UUID orderId, String authenticatedSellerId, SellerDecisionDto decisionDto);

    void processPaymentSuccess(PaymentSucceededEventDto eventDto);

    void processPaymentFailure(PaymentFailedEventDto eventDto);

    Page<OrderSummaryDto> getMyOrders(String userId, String statusFilter, Pageable pageable);

    OrderDetailDto getOrderDetailsForUser(UUID orderId, String userId);

    void processBuyerCancelPaymentAttempt(UUID orderId, String buyerId);

    Page<OrderSummaryDto> getMySales(String sellerId, String statusFilter, Pageable pageable);

    void processSellerInitiatedCancellation(UUID orderId, String sellerId, String reason);

    void confirmOrderFulfillment(UUID orderId, String sellerId);

    void processOrderCompletionByBuyer(UUID orderId, String buyerId, LocalDateTime confirmationTimestamp);

    void processRefundRequiredForReturnEvent(RefundRequiredForReturnEventDto event);

    void processRefundSuccess(RefundSucceededEventDto event);

    void processRefundFailure(RefundFailedEventDto event);
}