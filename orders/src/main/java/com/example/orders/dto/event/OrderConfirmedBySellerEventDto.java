package com.example.orders.dto.event;

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
public class OrderConfirmedBySellerEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String sellerId;
    private String buyerId;
    private String productTitleSnapshot;
}