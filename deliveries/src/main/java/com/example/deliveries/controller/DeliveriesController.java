 package com.example.deliveries.controller;

import com.example.deliveries.dto.request.MarkAsShippedRequestDto;
import com.example.deliveries.dto.request.ReportDeliveryIssueRequestDto;
import com.example.deliveries.dto.request.ReturnRequestDto;
import com.example.deliveries.dto.request.UpdateToDeliveredRequestDto;
import com.example.deliveries.dto.response.DeliveryDetailDto; // Using DTO for response
import com.example.deliveries.entity.Delivery;
import com.example.deliveries.mapper.DeliveryMapper; // Import mapper
import com.example.deliveries.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DeliveriesController {

    private final DeliveryService deliveryService;
    private final DeliveryMapper deliveryMapper; // Inject mapper
    private static final String USER_ID_HEADER = "X-User-ID";

    // Endpoint for Seller to mark a delivery as shipped
    @PostMapping("/{deliveryId}/ship")
    public ResponseEntity<DeliveryDetailDto> markAsShipped(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody MarkAsShippedRequestDto requestDto) {
        log.info("Seller {} marking delivery {} as shipped. Courier: {}, Tracking: {}",
                sellerId, deliveryId, requestDto.getCourierName(), requestDto.getTrackingNumber());
        Delivery updatedDelivery = deliveryService.markDeliveryAsShipped(deliveryId, sellerId, requestDto);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    // Endpoint for Seller to mark a delivery as delivered
    @PostMapping("/{deliveryId}/mark-delivered")
    public ResponseEntity<DeliveryDetailDto> markAsDelivered(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody UpdateToDeliveredRequestDto requestDto) {
        log.info("Seller {} marking delivery {} as delivered.", sellerId, deliveryId);
        Delivery updatedDelivery = deliveryService.updateDeliveryStatusToDelivered(deliveryId, sellerId, requestDto);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    // Endpoint for Seller to report an issue with a delivery
    @PostMapping("/{deliveryId}/report-issue")
    public ResponseEntity<DeliveryDetailDto> reportDeliveryIssue(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String sellerId, // Or could be buyerId depending on who can report
            @Valid @RequestBody ReportDeliveryIssueRequestDto requestDto) {
        log.info("User {} reporting issue for delivery {}: {}", sellerId, deliveryId, requestDto.getNotes());
        Delivery updatedDelivery = deliveryService.reportDeliveryIssue(deliveryId, sellerId, requestDto);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    @PostMapping("/{deliveryId}/buyer-confirm-receipt")
    public ResponseEntity<DeliveryDetailDto> buyerConfirmReceipt(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String buyerId) {
        log.info("Buyer {} confirming receipt for delivery {}", buyerId, deliveryId);
        Delivery updatedDelivery = deliveryService.confirmReceiptByBuyer(deliveryId, buyerId);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    @PostMapping("/{deliveryId}/request-return")
    public ResponseEntity<DeliveryDetailDto> buyerRequestReturn(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String buyerId,
            @Valid @RequestBody ReturnRequestDto returnRequest) {
        log.info("Buyer {} requesting return for delivery {}. Reason: {}", buyerId, deliveryId, returnRequest.getReason());
        Delivery updatedDelivery = deliveryService.requestReturnByBuyer(deliveryId, buyerId, returnRequest);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    // Endpoint to get delivery details by Order ID
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<DeliveryDetailDto> getDeliveryByOrderId(
            @PathVariable UUID orderId,
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("User {} fetching delivery details for order ID: {}", userId, orderId);
        Delivery delivery = deliveryService.getDeliveryByOrderId(orderId, userId);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(delivery));
    }

    // Endpoint to get delivery details by Delivery ID
    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryDetailDto> getDeliveryById(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("User {} fetching delivery details for delivery ID: {}", userId, deliveryId);
        Delivery delivery = deliveryService.getDeliveryById(deliveryId, userId);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(delivery));
    }

    @PostMapping("/{deliveryId}/approve-return")
    public ResponseEntity<DeliveryDetailDto> approveReturnRequest(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String sellerId) {
        log.info("Seller {} approving return request for delivery {}", sellerId, deliveryId);
        Delivery updatedDelivery = deliveryService.approveReturnBySeller(deliveryId, sellerId);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }

    // Endpoint for Seller to confirm they have RECEIVED the returned item
    @PostMapping("/{deliveryId}/confirm-return-received")
    public ResponseEntity<DeliveryDetailDto> confirmReturnReceived(
            @PathVariable UUID deliveryId,
            @RequestHeader(USER_ID_HEADER) String sellerId) {
        log.info("Seller {} confirming receipt of returned item for delivery {}", sellerId, deliveryId);
        Delivery updatedDelivery = deliveryService.confirmReturnItemReceived(deliveryId, sellerId);
        return ResponseEntity.ok(deliveryMapper.toDeliveryDetailDto(updatedDelivery));
    }
}