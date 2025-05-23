package com.example.orders.dto.event;

import lombok.Builder;
import lombok.Data; // Or @Value for immutability
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderReadyForShippingEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private UUID auctionId;
    private Long productId; // Important: Type is Long
    private String productTitleSnapshot;
    private String sellerId;
    private String buyerId;
    private BigDecimal amountPaid; // Use BigDecimal for currency
    private String currency;
    private String paymentTransactionRef;

    // Shipping Address Details
    private String shippingRecipientName;
    private String shippingStreetAddress;
    private String shippingCity;
    // private String shippingStateProvince; // Optional
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhoneNumber;
}