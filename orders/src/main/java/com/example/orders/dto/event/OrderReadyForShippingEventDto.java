package com.example.orders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Use Long for amount in smallest unit if consistent
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReadyForShippingEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID orderId;
    private UUID auctionId;
    private Long productId;
    private String productTitleSnapshot;

    private String sellerId;
    private String buyerId; // The user who successfully paid

    private Long amountPaid; // Amount in smallest currency unit
    private String currency; // e.g., "vnd"

    private String paymentTransactionRef; // e.g., Stripe PaymentIntent ID

    // Shipping address details would ideally be included here if available.
    // For now, this event just signals readiness. Delivery service might fetch/confirm address.
    // private String shippingAddressLine1;
    // private String shippingCity;
    // private String shippingPostalCode;
    // private String shippingCountry;
}