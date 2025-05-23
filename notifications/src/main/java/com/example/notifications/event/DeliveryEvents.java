package com.example.notifications.event;// Package: com.example.notifications.event.dto (or similar)

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;
import java.util.UUID;

public final class DeliveryEvents {

    private DeliveryEvents() {}

    @Value
    @Builder
    public static class DeliveryCreatedEventDto {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID deliveryId;
        UUID orderId;
        String buyerId;
        String sellerId;
        String initialDeliveryStatus;
        String productInfoSnapshot;   // e.g., "Vintage Leather Jacket - Size L"
        String shippingAddressSummary;
    }

    @Value
    @Builder
    public static class DeliveryShippedEventDto {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID deliveryId;
        UUID orderId;
        String buyerId;
        String sellerId;
        LocalDateTime shippedAt;
        String courierName;
        String trackingNumber;
        String productInfoSnapshot; // Added for notification context
    }

    @Value
    @Builder
    public static class DeliveryDeliveredEventDto {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID deliveryId;
        UUID orderId;
        String buyerId;
        String sellerId;
        LocalDateTime deliveredAt;
        String productInfoSnapshot; // Added for notification context
    }

    @Value
    @Builder
    public static class DeliveryAwaitingBuyerConfirmationEventDto { // From DeliveriesService
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID deliveryId;
        UUID orderId;
        String buyerId;
        String sellerId;
        LocalDateTime deliveredAt;
        String productInfoSnapshot;
        // int confirmationWindowDays; // Optional, if backend sends it
    }

    @Value
    @Builder
    public static class DeliveryIssueReportedEventDto {
        UUID eventId;
        LocalDateTime eventTimestamp;
        UUID deliveryId;
        UUID orderId;
        String buyerId;
        String sellerId;
        String reporterId;
        String issueNotes;
        String newStatus;
        String productInfoSnapshot; // Added for notification context
    }
}