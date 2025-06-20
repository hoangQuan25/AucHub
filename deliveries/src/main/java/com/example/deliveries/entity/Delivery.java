// File: com.example.deliveries.entity.Delivery.java
package com.example.deliveries.entity;

import jakarta.persistence.*; // Or javax.persistence if using older Spring Boot
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    private UUID deliveryId;

    @Column(nullable = false, unique = true)
    private UUID orderId; // Links back to the order in OrdersService

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String shippingRecipientName;

    @Column(nullable = false)
    private String shippingStreetAddress;

    @Column(nullable = false)
    private String shippingCity;

    @Column(nullable = false)
    private String shippingPostalCode;

    @Column(nullable = false)
    private String shippingCountry;

    private String shippingPhoneNumber;

    @Column(columnDefinition = "TEXT")
    private String productInfoSnapshot; // e.g., "Item: Awesome T-Shirt, Qty: 1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeliveryStatus deliveryStatus;

    @Column(length = 100) // Max length for courier name
    private String courierName;

    @Column(length = 100) // Max length for tracking number
    private String trackingNumber;

    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    @Column(columnDefinition = "TEXT")
    private String notes; // For seller updates, reasons for status changes, etc.

    @Column(length = 255)
    private String returnReason;

    @Column(columnDefinition = "TEXT")
    private String returnComments;

    // To store URLs of uploaded images, perhaps as a JSON string or in a separate table
    @Column(columnDefinition = "TEXT")
    private String returnImageUrls; // e.g., ["url1.jpg", "url2.jpg"]

    private String returnCourier;
    private String returnTrackingNumber;
    private LocalDateTime returnApprovedAt;
    private LocalDateTime returnItemReceivedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}