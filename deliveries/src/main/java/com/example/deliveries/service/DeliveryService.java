// File: com.example.deliveries.service.DeliveryService.java
package com.example.deliveries.service;

import com.example.deliveries.dto.event.OrderReadyForShippingEventDto;
import com.example.deliveries.dto.request.ReportDeliveryIssueRequestDto;
import com.example.deliveries.dto.request.MarkAsShippedRequestDto;
import com.example.deliveries.dto.request.ReturnRequestDto;
import com.example.deliveries.dto.request.UpdateToDeliveredRequestDto;
import com.example.deliveries.entity.Delivery; // So we can return Delivery objects if needed by controller

import java.time.LocalDateTime;
import java.util.UUID;
// Potentially add DTOs for return types if needed by controller later, e.g., DeliveryDetailsDto

public interface DeliveryService {

    void createDeliveryFromOrderEvent(OrderReadyForShippingEventDto event);

    Delivery markDeliveryAsShipped(UUID deliveryId, String sellerId, MarkAsShippedRequestDto requestDto);

    Delivery updateDeliveryStatusToDelivered(UUID deliveryId, String sellerId, UpdateToDeliveredRequestDto requestDto);

    Delivery confirmReceiptByBuyer(UUID deliveryId, String buyerId);

    Delivery requestReturnByBuyer(UUID deliveryId, String buyerId, ReturnRequestDto returnRequest);

    void processAutoCompletion(UUID deliveryId, LocalDateTime originalConfirmationDeadline);

    Delivery reportDeliveryIssue(UUID deliveryId, String sellerId, ReportDeliveryIssueRequestDto requestDto);

    Delivery approveReturnBySeller(UUID deliveryId, String sellerId);

    Delivery confirmReturnItemReceived(UUID deliveryId, String sellerId);

    Delivery getDeliveryByOrderId(UUID orderId, String userId);

    Delivery getDeliveryById(UUID deliveryId, String userId);
}