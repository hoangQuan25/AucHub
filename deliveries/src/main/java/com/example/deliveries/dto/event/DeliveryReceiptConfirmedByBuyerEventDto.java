package com.example.deliveries.dto.event;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class DeliveryReceiptConfirmedByBuyerEventDto {
    UUID eventId; LocalDateTime eventTimestamp; UUID deliveryId; UUID orderId; String buyerId; String sellerId; LocalDateTime confirmationTimestamp; String productInfoSnapshot;
}
