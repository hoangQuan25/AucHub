// In com.example.orders.dto.event.DeliveryReceiptConfirmedByBuyerEventDto.java (OrdersService)
package com.example.orders.dto.event; // Or your client DTO package

import lombok.Builder;
import lombok.Data; // Or @Value if you prefer immutable
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DeliveryReceiptConfirmedByBuyerEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID deliveryId; // From DeliveriesService
    private UUID orderId;    // Crucial for OrdersService to find its order
    private String buyerId;
    private String sellerId;
    private LocalDateTime confirmationTimestamp;
    private String productInfoSnapshot; // Optional, but good for context
}