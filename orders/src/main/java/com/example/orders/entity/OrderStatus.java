package com.example.orders.entity; // Adjust package as needed

public enum OrderStatus {
    // Initial state after auction won
    AWAITING_WINNER_PAYMENT,

    // Payment window expired for the winner
    PAYMENT_WINDOW_EXPIRED_WINNER,
    AWAITING_SELLER_DECISION, // Seller needs to decide next steps

    // When offered to subsequent bidders
    AWAITING_NEXT_BIDDER_PAYMENT, // Generic for 2nd, 3rd etc.
    PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER,

    // Payment outcomes
    PAYMENT_SUCCESSFUL, // Payment completed, ready for delivery handoff

    // Post-payment states (often updated via events from other services like Delivery)
    // Or directly if Orders service also manages these high-level states
    AWAITING_SHIPMENT, // If you want this intermediary step before Delivery service takes over
    // ORDER_SHIPPED,
    // ORDER_DELIVERED,

    // Order cancellation states
    ORDER_CANCELLED_NO_PAYMENT_FINAL, // No one paid after all attempts
    ORDER_CANCELLED_BY_SELLER,
    ORDER_CANCELLED_SYSTEM, // e.g., no eligible next bidders

    // If seller chooses to reopen auction
    AUCTION_REOPEN_INITIATED // Order is effectively terminated from here, auction service takes over
}