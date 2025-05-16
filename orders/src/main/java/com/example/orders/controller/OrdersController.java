package com.example.orders.controller;

import com.example.orders.dto.request.SellerDecisionDto;
import com.example.orders.dto.response.OrderDetailDto;
import com.example.orders.dto.response.OrderSummaryDto;
import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OrdersController {

    private final OrderService orderService;
    private static final String USER_ID_HEADER = "X-User-ID";

    @PostMapping("/{orderId}/seller-decision")
    public ResponseEntity<Void> handleSellerDecision(
            @PathVariable UUID orderId,
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody SellerDecisionDto decisionDto) {
        log.info("Received seller decision for order ID: {} from seller ID: {}. Decision: {}",
                orderId, sellerId, decisionDto.getDecisionType());
        orderService.processSellerDecision(orderId, sellerId, decisionDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OrderSummaryDto>> getMyOrders(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestParam(name = "status", required = false) String statusFilter,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        log.info("User {} fetching their orders. Filter: {}, Page: {}", userId, statusFilter, pageable);
        Page<OrderSummaryDto> myOrders = orderService.getMyOrders(userId, statusFilter, pageable);
        return ResponseEntity.ok(myOrders);
    }

    @GetMapping("/my-sales")
    public ResponseEntity<Page<OrderSummaryDto>> getMySales(
            @RequestHeader(USER_ID_HEADER) String sellerId, // This header now represents the sellerId
            @RequestParam(name = "status", required = false) String statusFilter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Seller {} fetching their sales. Filter: {}, Page: {}", sellerId, statusFilter, pageable);
        Page<OrderSummaryDto> mySales = orderService.getMySales(sellerId, statusFilter, pageable);
        return ResponseEntity.ok(mySales);
    }

    @PostMapping("/my-sales/{orderId}/cancel")
    public ResponseEntity<Void> sellerCancelOrder(
            @PathVariable UUID orderId,
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @RequestBody(required = false) /* Optional DTO for cancellation reason */ String reason) { // Or a DTO
        log.info("Seller {} attempting to cancel order {}", sellerId, orderId);
        orderService.processSellerInitiatedCancellation(orderId, sellerId, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDto> getOrderDetails(
            @PathVariable UUID orderId,
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("User {} fetching details for order {}", userId, orderId);
        OrderDetailDto orderDetails = orderService.getOrderDetailsForUser(orderId, userId);
        return ResponseEntity.ok(orderDetails);
    }

    @PostMapping("/{orderId}/buyer-cancel-attempt")
    public ResponseEntity<Void> buyerCancelPaymentAttempt(
            @PathVariable UUID orderId,
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("User {} attempting to cancel their payment for order {}", userId, orderId);
        orderService.processBuyerCancelPaymentAttempt(orderId, userId);
        return ResponseEntity.ok().build();
    }
}