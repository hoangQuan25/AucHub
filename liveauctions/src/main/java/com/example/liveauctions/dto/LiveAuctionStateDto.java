package com.example.liveauctions.dto;

import com.example.liveauctions.entity.AuctionStatus; // Import the enum
import lombok.Builder;
import lombok.Value; // Immutable DTO, good for state snapshots
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class LiveAuctionStateDto {

    UUID auctionId;
    AuctionStatus status;
    BigDecimal currentBid;
    String highestBidderId; // Can be null
    String highestBidderUsername; // Can be null
    BigDecimal nextBidAmount; // Calculated: currentBid + currentBidIncrement
    long timeLeftMs; // Milliseconds remaining
    LocalDateTime endTime;
    boolean reserveMet;

    BidDto newBid;           // present only on “bid placed” events
    String winnerId;         // present when status = SOLD
    BigDecimal winningBid;
}