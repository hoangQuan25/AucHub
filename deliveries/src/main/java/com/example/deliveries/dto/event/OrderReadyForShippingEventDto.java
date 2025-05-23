// File: com.example.deliveries.dto.event.OrderReadyForShippingEventDto.java
// This is the structure DeliveriesService EXPECTS to receive from OrdersService.
// OrdersService must publish an event matching this.
package com.example.deliveries.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
// Assuming BigDecimal for amounts, adjust if OrdersService sends Long for smallest unit
import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReadyForShippingEventDto {
    // Event specific fields
    private UUID eventId; // Event ID from OrdersService
    private LocalDateTime eventTimestamp;

    // Order and User Identifiers
    private UUID orderId;
    private String buyerId;
    private String sellerId;
    private UUID auctionId; // For context if needed

    // Product Information (Snapshot)
    private Long productId; // Product ID from OrdersService
    private String productTitleSnapshot;
    // private int quantity; // If relevant for delivery
    // private String productImageUrlSnapshot; // Optional

    // Shipping Address Details (Essential for Delivery Service)
    private String shippingRecipientName;
    private String shippingStreetAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhoneNumber; // Optional

    // Payment Details (Optional, for reference if needed by delivery)
    // private BigDecimal amountPaid;
    // private String currency;
}