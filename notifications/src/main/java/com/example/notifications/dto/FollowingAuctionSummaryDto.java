package com.example.notifications.dto;

import com.example.notifications.entity.AuctionStatus; // Use Notification's enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowingAuctionSummaryDto {
    private UUID id;
    private String auctionType; // "LIVE" or "TIMED" - Crucial for FE routing
    private String productTitleSnapshot;
    private String productImageUrlSnapshot;
    private BigDecimal currentBid;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private Set<Long> categoryIds;
}