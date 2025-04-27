package com.example.liveauctions.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class BidDto {
    // Long id; // Optional: Usually not needed for display history
    // UUID liveAuctionId; // Optional: Context is already known
    String bidderId;
    String bidderUsernameSnapshot; // Use the snapshot stored in Bid entity
    BigDecimal amount;
    LocalDateTime bidTime;
}