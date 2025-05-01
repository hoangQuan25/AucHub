package com.example.timedauctions.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreateTimedAuctionDto {
    @NotNull
    private Long productId;

    @NotNull(message = "Start price cannot be null")
    @Positive(message = "Start price must be positive")
    private BigDecimal startPrice;

    // Reserve price is optional
    @Positive(message = "Reserve price must be positive")
    private BigDecimal reservePrice;

    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    // Use start time for scheduling, duration for end time calculation
    @AssertTrue(message = "End time must be at least 24 hours after the effective start time")
    private boolean isDurationValid() {
        LocalDateTime effectiveStartTime = (startTime != null) ? startTime : LocalDateTime.now();
        // Ensure endTime is not null before comparing (already handled by @NotNull)
        if (endTime == null) {
            return false; // Or true if @NotNull handles it, but defensive check is okay
        }
        // Check if endTime is at least 24 hours after effectiveStartTime
        return !endTime.isBefore(effectiveStartTime.plusHours(24));
    }
}