// src/main/java/com/example/timedauctions/service/AuctionSchedulingService.java
package com.example.timedauctions.service;

import com.example.timedauctions.entity.TimedAuction;

public interface AuctionSchedulingService {

    /**
     * Schedules the auction start command via delayed message queue.
     * @param auction The auction to schedule start for.
     */
    void scheduleAuctionStart(TimedAuction auction);

    /**
     * Schedules the auction end command via delayed message queue.
     * @param auction The auction to schedule end for.
     */
    void scheduleAuctionEnd(TimedAuction auction);

}