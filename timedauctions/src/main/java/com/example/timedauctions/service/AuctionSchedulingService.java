// src/main/java/com/example/timedauctions/service/AuctionSchedulingService.java
package com.example.timedauctions.service;

import com.example.timedauctions.entity.TimedAuction;

public interface AuctionSchedulingService {

    void scheduleAuctionStart(TimedAuction auction);

    void scheduleAuctionEnd(TimedAuction auction);

}