package com.example.orders.dto.event;

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
public class NewLiveAuctionFromReopenedOrderEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp; // Ensure publisher also sets this

    private UUID newLiveAuctionId;
    private Long productId;
    private String sellerId;
    private UUID originalOrderId;
}