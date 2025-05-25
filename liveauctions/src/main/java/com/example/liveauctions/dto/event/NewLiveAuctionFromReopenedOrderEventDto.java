package com.example.liveauctions.dto.event; // Or a shared event DTO module

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
    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    private UUID newLiveAuctionId; // ID of the LiveAuction just created
    private Long productId;
    private String sellerId;
    private UUID originalOrderId;  // ID of the Order that was "reopened"
}