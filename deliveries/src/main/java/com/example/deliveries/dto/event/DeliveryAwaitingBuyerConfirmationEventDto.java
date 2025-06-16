package com.example.deliveries.dto.event;

// package com.example.deliveries.dto.event;
import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class DeliveryAwaitingBuyerConfirmationEventDto {
    UUID eventId;
    LocalDateTime eventTimestamp;
    UUID deliveryId;
    UUID orderId;
    String buyerId;
    String sellerId;
    LocalDateTime deliveredAt; // When it was marked as physically delivered
    String productInfoSnapshot;
}
