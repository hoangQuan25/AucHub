package com.example.deliveries.entity;

public enum DeliveryStatus {
    PENDING_PREPARATION,    // Initial: Order confirmed, seller to prepare package.
    READY_FOR_SHIPMENT,   // Seller has packed, ready to give to courier.
    SHIPPED_IN_TRANSIT,   // Seller handed to courier, provided tracking.
    DELIVERED,            // Seller confirms (based on tracking/buyer) it's delivered.
    ISSUE_REPORTED,       // Seller reports an issue (e.g., buyer claim, return by courier).
    CANCELLED             // If a delivery itself can be cancelled before shipment (rare for this phase)
}