package com.example.liveauctions.service;

import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.entity.LiveAuction;
import jakarta.annotation.Nullable;

// Interface for the component that publishes state updates for WebSocket broadcasting
public interface WebSocketEventPublisher {

    void publishAuctionStateUpdate(LiveAuction auction, @Nullable Bid newBidEntity);
}