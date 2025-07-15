package com.example.products.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEventDto {
    private UUID eventId;
    private UUID orderId;
    private Long productId;
    private String cancellationReason;
}