package com.example.timedauctions.dto.event; // Or a shared event DTO module

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
public class NewTimedAuctionFromReopenedOrderEventDto {
    private UUID eventId;
    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    private UUID newTimedAuctionId; // ID of the TimedAuction just created
    private Long productId;
    private String sellerId;
    private UUID originalOrderId;   // ID of the Order that was "reopened"
}