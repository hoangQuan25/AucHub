package com.example.orders.dto.event;

import com.example.orders.entity.OrderStatus; // Assuming OrderStatus is in this path
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId;
    private UUID auctionId;
    private Long productId;
    private String sellerId;
    private String currentBidderIdAtCancellation; // Who was responsible when it was cancelled (if applicable)

    private OrderStatus finalOrderStatus; // e.g., ORDER_CANCELLED_NO_PAYMENT_FINAL, ORDER_CANCELLED_BY_SELLER
    private String cancellationReason;
}