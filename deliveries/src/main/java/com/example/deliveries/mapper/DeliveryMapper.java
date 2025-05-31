 package com.example.deliveries.mapper;

import com.example.deliveries.dto.response.DeliveryDetailDto;
import com.example.deliveries.entity.Delivery;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {

    public DeliveryDetailDto toDeliveryDetailDto(Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        return DeliveryDetailDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .shippingRecipientName(delivery.getShippingRecipientName())
                .shippingStreetAddress(delivery.getShippingStreetAddress())
                .shippingCity(delivery.getShippingCity())
                .shippingPostalCode(delivery.getShippingPostalCode())
                .shippingCountry(delivery.getShippingCountry())
                .shippingPhoneNumber(delivery.getShippingPhoneNumber())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .deliveryStatus(delivery.getDeliveryStatus())
                .courierName(delivery.getCourierName())
                .trackingNumber(delivery.getTrackingNumber())
                .shippedAt(delivery.getShippedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .returnCourier(delivery.getReturnCourier())
                .returnTrackingNumber(delivery.getReturnTrackingNumber())
                .returnApprovedAt(delivery.getReturnApprovedAt())
                .returnItemReceivedAt(delivery.getReturnItemReceivedAt())
                .returnReason(delivery.getReturnReason())
                .returnComments(delivery.getReturnComments())
                .returnImageUrls(delivery.getReturnImageUrls())
                .notes(delivery.getNotes())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}