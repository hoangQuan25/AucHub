package com.example.timedauctions.dto;

import com.example.timedauctions.entity.AuctionStatus; // Use correct status enum
import lombok.Builder;
import lombok.Value; // Use @Value for immutable DTO

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Value // Makes class final, fields private final, generates constructor, equals, hashCode, toString
@Builder
public class TimedAuctionSummaryDto {
    UUID id;
    String productTitleSnapshot;
    String productImageUrlSnapshot;
    BigDecimal currentBid; // Represents VISIBLE current bid or start price
    LocalDateTime endTime;
    AuctionStatus status;
    // Add other fields like reserveMet if needed on summary card
    // boolean reserveMet;
    Set<Long> categoryIds;
}