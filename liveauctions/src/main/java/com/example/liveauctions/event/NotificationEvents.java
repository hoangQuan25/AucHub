package com.example.liveauctions.event; // Or shared package

import com.example.liveauctions.entity.AuctionStatus; // Use LiveAuction's status enum
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set; // Make sure Set is imported
import java.util.UUID;

public final class NotificationEvents {

    @Value @Builder
    public static class AuctionStartedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull String auctionType; // "LIVE" or "TIMED"
        @NotNull String sellerId;
        LocalDateTime startTime;
        LocalDateTime endTime;
    }

    @Value @Builder
    public static class AuctionEndedEvent {
        @NotNull UUID auctionId;
        @NotNull String productTitleSnapshot;
        @NotNull AuctionStatus finalStatus;
        LocalDateTime actualEndTime;
        @NotNull String sellerId;
        String winnerId;
        String winnerUsernameSnapshot;
        BigDecimal winningBid;
    }


}