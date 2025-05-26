package com.example.products.dto.event;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private Long productId; // Crucial for product-service
    private String sellerId;
    private String buyerId;
    // You can add other details if any other service might be interested,
    // but productId is key for your current requirement.
}