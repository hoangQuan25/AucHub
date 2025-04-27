package com.example.liveauctions.dto;

import com.example.liveauctions.entity.AuctionStatus; // Optional: Maybe filter by status later
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class LiveAuctionSummaryDto {
    UUID id;
    String productTitleSnapshot;
    String productImageUrlSnapshot;
    BigDecimal currentBid; // Or startPrice if no bids yet? Service logic decides.
    LocalDateTime endTime; // Useful for displaying end time or calculating time left on FE
    AuctionStatus status; // Good to know if it's ACTIVE or SCHEDULED
    // String sellerUsernameSnapshot; // Optional: If needed on listing card
    // long timeLeftMs; // Alternative to endTime, calculated by service
}