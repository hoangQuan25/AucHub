package com.example.deliveries.dto.response;

import com.example.deliveries.entity.DeliveryStatus; // Assuming this path
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DeliveryDetailDto {
    private UUID deliveryId;
    private UUID orderId;
    private String buyerId;
    private String sellerId;
    private String shippingRecipientName;
    private String shippingStreetAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private String shippingPhoneNumber;
    private String productInfoSnapshot;
    private DeliveryStatus deliveryStatus;
    private String courierName;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private String returnCourier;
    private String returnTrackingNumber;
    private LocalDateTime returnApprovedAt;
    private LocalDateTime returnItemReceivedAt;
    private String returnReason;
    private String returnComments;
    private String returnImageUrls;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}