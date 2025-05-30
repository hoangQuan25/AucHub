package com.example.liveauctions.service;

import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.entity.LiveAuction;
import jakarta.annotation.Nullable;

// Interface for the component that publishes state updates for WebSocket broadcasting
public interface WebSocketEventPublisher {

    /**
     * Builds the state update event from the auction entity and publishes it
     * to the appropriate RabbitMQ exchange (e.g., auction_events_exchange).
     *
     * @param auction The updated LiveAuction entity.
     */
    void publishAuctionStateUpdate(LiveAuction auction, @Nullable Bid newBidEntity);

    // You might add more specific methods like publishAuctionStarted, publishAuctionEnded if needed
}