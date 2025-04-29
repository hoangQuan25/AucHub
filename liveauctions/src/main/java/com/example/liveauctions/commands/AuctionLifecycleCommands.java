package com.example.liveauctions.commands; // Or your commands/dtos package

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuctionLifecycleCommands {

    // Command sent to trigger starting a scheduled auction
    record StartAuctionCommand(
            UUID auctionId
    ) {}

    // Command sent to trigger ending an active auction
    record EndAuctionCommand(
            UUID auctionId, LocalDateTime fireAt
    ) {}

    record HammerDownCommand(UUID auctionId, String sellerId) { }

    record CancelAuctionCommand(UUID auctionId, String sellerId) {}

}

// You can place these records in separate files if preferred.