package com.example.liveauctions.event; // Your events package

import com.example.liveauctions.dto.BidDto;
import com.example.liveauctions.entity.AuctionStatus; // Use the enum
import lombok.Builder;
import lombok.Data; // Using @Data for simplicity, or @Value for immutability
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published to RabbitMQ when an auction's state changes
 * in a way that needs to be broadcast to WebSocket clients.
 * Contains the data needed to build the LiveAuctionStateDto.
 */
@Data // Or @Value for immutable event
@Builder
@NoArgsConstructor // Needed for Jackson deserialization if using @Data/@Value with Builder
@AllArgsConstructor // Needed for Jackson deserialization if using @Data/@Value with Builder
public class AuctionStateUpdateEvent {

    // Fields closely mirroring LiveAuctionStateDto for easy mapping

    private UUID auctionId;
    private AuctionStatus status;
    private BigDecimal currentBid; // Can be null
    private String highestBidderId; // Can be null
    private String highestBidderUsername; // Can be null (use snapshot)
    private BigDecimal nextBidAmount; // Calculated
    private long timeLeftMs; // Calculated
    private boolean reserveMet;

    private BidDto newBid;                     // <— sent only when placeBid() succeeds
    private String winnerId;                   // <— filled when status = SOLD
    private BigDecimal winningBid;
}