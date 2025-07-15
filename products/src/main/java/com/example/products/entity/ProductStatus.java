package com.example.products.entity;

public enum ProductStatus {
    /**
     * The product is available to be put into an auction.
     * Initial state and state after a failed/cancelled sale.
     */
    AVAILABLE,

    /**
     * The product is currently in an active or scheduled auction.
     * It cannot be sold or put into another auction.
     */
    IN_AUCTION,

    /**
     * An auction has ended successfully, and the product is in the order process.
     * It is reserved until the order is completed or cancelled.
     */
    AWAITING_COMPLETION,

    /**
     * The order was successfully paid, delivered, and confirmed by the buyer.
     * This is a final, terminal state.
     */
    SOLD
}