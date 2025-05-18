// File: com.example.deliveries.dto.event.DeliveryCreatedEventDto.java
package com.example.deliveries.dto.event;

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
public class DeliveryCreatedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID deliveryId;
    private UUID orderId;
    private String buyerId;
    private String sellerId;
    private String initialDeliveryStatus; // e.g., "PENDING_PREPARATION"
    private String productInfoSnapshot; // Brief description of items
    private String shippingAddressSummary; // e.g., "City, Country" for quick view
}