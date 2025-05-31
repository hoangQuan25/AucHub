package com.example.deliveries.entity;

// In com.example.deliveries.entity.DeliveryStatus.java
public enum DeliveryStatus {
    PENDING_PREPARATION,
    READY_FOR_SHIPMENT,
    SHIPPED_IN_TRANSIT,
    DELIVERED,            // Courier/Seller marks as physically delivered
    // --- NEW STATUSES for buyer confirmation flow ---
    AWAITING_BUYER_CONFIRMATION, // Item delivered, waiting for buyer to confirm receipt or system to auto-confirm
    RECEIPT_CONFIRMED_BY_BUYER,  // Buyer clicked "Item Received"
    COMPLETED_AUTO,              // System auto-confirmed after X days
    RETURN_REQUESTED_BY_BUYER,   // Buyer initiated a return/refund request
    // --- End New Statuses ---
    ISSUE_REPORTED,       // General issue reported (can be by seller or system)
    CANCELLED,             // Delivery cancelled (rare, usually before shipment)

    RETURN_APPROVED_AWAITING_ITEM,  // Seller acknowledges the return, waits for physical item
    RETURN_ITEM_RECEIVED,           // Seller confirms they have received the returned item
    RETURN_COMPLETED,               // The entire return & refund process is complete from Delivery's perspective
    RETURN_REJECTED_BY_SELLER;
}