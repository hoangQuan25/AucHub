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
     * Confirms receipt of the delivery by the buyer.
     * @param deliveryId The ID of the delivery.
     * @param buyerId The ID of the buyer confirming receipt.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery confirmReceiptByBuyer(UUID deliveryId, String buyerId);

    /**
     * Initiates a return request by the buyer for a delivery.
     * @param deliveryId The ID of the delivery.
     * @param buyerId The ID of the buyer requesting the return.
     * @param returnRequest DTO containing details about the return request.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery requestReturnByBuyer(UUID deliveryId, String buyerId, ReturnRequestDto returnRequest);

    /**
     * Processes auto-completion of a delivery if the confirmation deadline has passed.
     * @param deliveryId The ID of the delivery.
     * @param originalConfirmationDeadline The original deadline for confirming receipt.
     */
    void processAutoCompletion(UUID deliveryId, LocalDateTime originalConfirmationDeadline);

    /**
     * Allows a seller to report an issue with a delivery.
     * @param deliveryId The ID of the delivery.
     * @param sellerId The ID of the seller reporting the issue.
     * @param requestDto DTO containing notes about the issue.
     * @return The updated Delivery entity or a DTO.
     */
    Delivery reportDeliveryIssue(UUID deliveryId, String sellerId, ReportDeliveryIssueRequestDto requestDto);

    /**
     * Retrieves a delivery record by its associated order ID.
     * @param orderId The ID of the order.
     * @param userId The ID of the user requesting the information (for authorization).
     * @return The Delivery entity or a DTO.
     */
    Delivery getDeliveryByOrderId(UUID orderId, String userId);

    /**
     * Retrieves a delivery record by its ID.
     * @param deliveryId The ID of the delivery.
     * @param userId The ID of the user requesting the information (for authorization).
     * @return The Delivery entity or a DTO.
     */
    Delivery getDeliveryById(UUID deliveryId, String userId);
}