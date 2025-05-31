// Create this file in: com.example.orders.dto.event.OrderReturnedEventDto.java
package com.example.orders.dto.event;

import com.example.orders.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReturnedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private UUID auctionId;
    private Long productId;
    private String sellerId;
    private String buyerId;
    private OrderStatus finalOrderStatus; // Will be ORDER_RETURNED
}