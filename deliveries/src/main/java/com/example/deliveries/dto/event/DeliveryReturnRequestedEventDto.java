package com.example.deliveries.dto.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class DeliveryReturnRequestedEventDto {
    UUID eventId; LocalDateTime eventTimestamp; UUID deliveryId; UUID orderId; String buyerId; String sellerId; String reason; String comments; LocalDateTime requestTimestamp; String productInfoSnapshot;
}
