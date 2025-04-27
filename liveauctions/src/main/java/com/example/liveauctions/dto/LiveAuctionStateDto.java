package com.example.liveauctions.dto;

import com.example.liveauctions.entity.AuctionStatus; // Import the enum
import lombok.Builder;
import lombok.Value; // Immutable DTO, good for state snapshots
import java.math.BigDecimal;
import java.util.UUID;

@Value // Creates final fields, getters, constructor, equals/hashCode, toString
@Builder // Allows easy construction
public class LiveAuctionStateDto {

    UUID auctionId;
    AuctionStatus status;
    BigDecimal currentBid;
    String highestBidderId; // Can be null
    String highestBidderUsername; // Can be null
    BigDecimal nextBidAmount; // Calculated: currentBid + currentBidIncrement
    long timeLeftMs; // Milliseconds remaining
    boolean reserveMet;

    // Consider adding auction title/image snapshot here if needed frequently by WS clients,
    // though typically static info is loaded once via REST.
    // String productTitleSnapshot;
    // String productImageUrlSnapshot;
}