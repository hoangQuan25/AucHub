package com.example.notifications.client.dto;

import com.example.notifications.entity.AuctionStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
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
    Set<Long> categoryIds;
}
