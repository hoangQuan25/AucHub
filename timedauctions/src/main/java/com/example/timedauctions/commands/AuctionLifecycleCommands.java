package com.example.timedauctions.commands;
import java.util.UUID;

// Simple records for commands sent via RabbitMQ
public final class AuctionLifecycleCommands {
    public record StartAuctionCommand(UUID auctionId) {}
    public record EndAuctionCommand(UUID auctionId) {}
    public record CancelAuctionCommand(UUID auctionId, String sellerId) {}
    public record HammerDownCommand(UUID auctionId, String sellerId) {}
}