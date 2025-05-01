package com.example.timedauctions.entity; // Or a dedicated 'enums' package

public enum AuctionStatus {
    /**
     * Auction has been created but not yet started (scheduled for the future).
     */
    SCHEDULED,

    /**
     * Auction is currently running and accepting bids.
     */
    ACTIVE,

    /**
     * Auction has ended, the reserve price was met (or no reserve), and there was at least one bid.
     */
    SOLD,

    /**
     * Auction has ended, and the reserve price was not met.
     */
    RESERVE_NOT_MET,

    /**
     * Auction has ended with no bids placed (optional, could also be covered by RESERVE_NOT_MET if start price acts as implicit reserve). Consider if needed.
     */
    // NO_BIDS,

    /**
     * Auction was cancelled before or during its active phase (e.g., by admin or seller under certain conditions).
     */
    CANCELLED
}