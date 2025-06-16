package com.example.liveauctions.commands;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuctionLifecycleCommands {

    record StartAuctionCommand(
            UUID auctionId
    ) {}

    record EndAuctionCommand(
            UUID auctionId, LocalDateTime fireAt
    ) {}

    record HammerDownCommand(UUID auctionId, String sellerId) { }

    record CancelAuctionCommand(UUID auctionId, String sellerId) {}

}
