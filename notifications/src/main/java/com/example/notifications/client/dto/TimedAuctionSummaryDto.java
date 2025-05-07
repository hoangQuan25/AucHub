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
public class TimedAuctionSummaryDto {

    UUID id;
    String productTitleSnapshot;
    String productImageUrlSnapshot;
    BigDecimal currentBid;
    LocalDateTime endTime;
    AuctionStatus status;
    Set<Long> categoryIds;
}
