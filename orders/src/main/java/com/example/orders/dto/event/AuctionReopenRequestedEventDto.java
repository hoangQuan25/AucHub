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
public class AuctionReopenRequestedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId; // Order that led to this decision
    private UUID auctionId; // The auction to be reopened
    private String sellerId; // For verification by the auction service
}