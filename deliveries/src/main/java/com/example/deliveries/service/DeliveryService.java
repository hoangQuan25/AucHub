// File: com.example.deliveries.service.DeliveryService.java
package com.example.deliveries.service;

import com.example.deliveries.dto.event.OrderReadyForShippingEventDto;
import com.example.deliveries.dto.request.ReportDeliveryIssueRequestDto;
import com.example.deliveries.dto.request.MarkAsShippedRequestDto;
import com.example.deliveries.dto.request.UpdateToDeliveredRequestDto;
import com.example.deliveries.entity.Delivery; // So we can return Delivery objects if needed by controller

import java.util.UUID;
// Potentially add DTOs for return types if needed by controller later, e.g., DeliveryDetailsDto

public interface DeliveryService {

    /**
     * Creates a new delivery record based on an order that is ready for shipping.
     * This is typically triggered by an event from the OrdersService.
     * @param event The event DTO containing order and shipping details.
     */
    void createDeliveryFromOrderEvent(OrderReadyForShippingEventDto event);

    /**
     * Marks a delivery as shipped by the seller.
     * @param deliveryId The ID of the delivery to update.
     * @param sellerId The ID of the seller performing the action (for authorization).
     * @param requestDto DTO containing courier name, tracking number, and optional notes.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery markDeliveryAsShipped(UUID deliveryId, String sellerId, MarkAsShippedRequestDto requestDto);

    /**
     * Updates a delivery's status to DELIVERED, typically by the seller.
     * @param deliveryId The ID of the delivery.
     * @param sellerId The ID of the seller performing the action.
     * @param requestDto DTO containing optional notes.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery updateDeliveryStatusToDelivered(UUID deliveryId, String sellerId, UpdateToDeliveredRequestDto requestDto);

    /**
     * Allows a seller to report an issue with a delivery.
     * @param deliveryId The ID of the delivery.
     * @param sellerId The ID of the seller reporting the issue.
     * @param requestDto DTO containing notes about the issue.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery reportDeliveryIssue(UUID deliveryId, String sellerId, ReportDeliveryIssueRequestDto requestDto);

    // --- Query methods (can be added later as needed by controllers) ---
    // Optional<Delivery> getDeliveryById(UUID deliveryId);
    // Optional<Delivery> getDeliveryByOrderId(UUID orderId);
    // Page<Delivery> getDeliveriesForBuyer(String buyerId, Pageable pageable);
    // Page<Delivery> getDeliveriesForSeller(String sellerId, Pageable pageable);
}