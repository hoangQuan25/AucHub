// File: com.example.deliveries.dto.event.DeliveryDeliveredEventDto.java
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
public class DeliveryDeliveredEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID deliveryId;
    private UUID orderId;
    private String buyerId;
    private String sellerId;
    private LocalDateTime deliveredAt;
}