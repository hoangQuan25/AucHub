package com.example.liveauctions.dto.event; // Your events package

import com.example.liveauctions.dto.BidDto;
import com.example.liveauctions.entity.AuctionStatus; // Use the enum
import lombok.Builder;
import lombok.Data; // Using @Data for simplicity, or @Value for immutability
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionStateUpdateEvent {

    private UUID auctionId;
    private AuctionStatus status;
    private BigDecimal currentBid; // Can be null
    private String highestBidderId; // Can be null
    private String highestBidderUsername; // Can be null (use snapshot)
    private BigDecimal nextBidAmount; // Calculated
    private long timeLeftMs; // Calculated
    private boolean reserveMet;
    private LocalDateTime endTime;

    private BidDto newBid;
    private String winnerId;
    private BigDecimal winningBid;
}