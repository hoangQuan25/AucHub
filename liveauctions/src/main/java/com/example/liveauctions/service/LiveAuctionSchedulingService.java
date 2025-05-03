package com.example.liveauctions.service;

import com.example.liveauctions.entity.LiveAuction;
import java.util.UUID;

public interface LiveAuctionSchedulingService {

    /** Schedules the auction start command via delayed queue. */
    void scheduleAuctionStart(LiveAuction auction);

    /** Schedules the auction end command via delayed queue. */
    void scheduleAuctionEnd(LiveAuction auction);

    // Optional: Cancel scheduled end (if feasible)
    // void cancelScheduledEnd(UUID auctionId);
}