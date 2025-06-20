// File: com.example.deliveries.service.impl.DeliveryServiceImpl.java
package com.example.deliveries.service.impl;

import com.example.deliveries.commands.DeliveryWorkflowCommands;
import com.example.deliveries.config.RabbitMqConfig;
import com.example.deliveries.dto.event.*;
import com.example.deliveries.dto.request.MarkAsShippedRequestDto;
import com.example.deliveries.dto.request.ReportDeliveryIssueRequestDto;
import com.example.deliveries.dto.request.ReturnRequestDto;
import com.example.deliveries.dto.request.UpdateToDeliveredRequestDto;
import com.example.deliveries.entity.Delivery;
import com.example.deliveries.entity.DeliveryStatus;
import com.example.deliveries.repository.DeliveryRepository;
import com.example.deliveries.service.DeliveryService;
import com.example.deliveries.utils.DateTimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration BUYER_CONFIRMATION_WINDOW = Duration.ofDays(7);

    @Override
    @Transactional
    public void createDeliveryFromOrderEvent(OrderReadyForShippingEventDto event) {
        log.info("Creating delivery record for orderId: {}", event.getOrderId());

        // Idempotency check: Ensure a delivery for this order doesn't already exist
        if (deliveryRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Delivery record for orderId {} already exists. Skipping creation.", event.getOrderId());
            return;
        }

        Delivery delivery = Delivery.builder()
                .deliveryId(UUID.randomUUID()) // Generate new ID for the delivery
                .orderId(event.getOrderId())
                .buyerId(event.getBuyerId())
                .sellerId(event.getSellerId())
                .shippingRecipientName(event.getShippingRecipientName())
                .shippingStreetAddress(event.getShippingStreetAddress())
                .shippingCity(event.getShippingCity())
                .shippingPostalCode(event.getShippingPostalCode())
                .shippingCountry(event.getShippingCountry())
                .shippingPhoneNumber(event.getShippingPhoneNumber())
                .productInfoSnapshot(event.getProductTitleSnapshot()) // Keep it simple or build a more detailed snapshot
                .deliveryStatus(DeliveryStatus.PENDING_PREPARATION)
                // courierName, trackingNumber, shippedAt, deliveredAt will be set later
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery record created with ID: {} for order ID: {}", savedDelivery.getDeliveryId(), savedDelivery.getOrderId());

        // Publish DeliveryCreatedEvent
        publishDeliveryCreatedEvent(savedDelivery);
    }

    @Override
    @Transactional
    public Delivery markDeliveryAsShipped(UUID deliveryId, String sellerId, MarkAsShippedRequestDto requestDto) {
        log.info("Seller {} attempting to mark delivery {} as shipped.", sellerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        if (!delivery.getSellerId().equals(sellerId)) {
            log.warn("User {} is not the seller for delivery {}. Cannot mark as shipped.", sellerId, deliveryId);
            throw new SecurityException("User not authorized for this action."); // Or a custom access denied exception
        }

        // Validate current status (e.g., can only mark as shipped if PENDING_PREPARATION or READY_FOR_SHIPMENT)
        if (delivery.getDeliveryStatus() != DeliveryStatus.PENDING_PREPARATION &&
                delivery.getDeliveryStatus() != DeliveryStatus.READY_FOR_SHIPMENT) { // Add READY_FOR_SHIPMENT if you use it
            log.warn("Delivery {} is not in a state to be marked as shipped. Current status: {}", deliveryId, delivery.getDeliveryStatus());
            throw new IllegalStateException("Delivery cannot be marked as shipped from its current state.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.SHIPPED_IN_TRANSIT);
        delivery.setCourierName(requestDto.getCourierName());
        delivery.setTrackingNumber(requestDto.getTrackingNumber());
        delivery.setShippedAt(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()));
        if (requestDto.getNotes() != null && !requestDto.getNotes().isBlank()) {
            delivery.setNotes(requestDto.getNotes());
        }

        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} marked as SHIPPED_IN_TRANSIT. Tracking: {} via {}",
                updatedDelivery.getDeliveryId(), updatedDelivery.getTrackingNumber(), updatedDelivery.getCourierName());

        publishDeliveryShippedEvent(updatedDelivery);
        return updatedDelivery;
    }

    @Override
    @Transactional
    public Delivery updateDeliveryStatusToDelivered(UUID deliveryId, String actorId, UpdateToDeliveredRequestDto requestDto) {
        // actorId could be sellerId or a system/admin ID if courier updates
        log.info("Actor {} attempting to mark delivery {} as physically delivered, transitioning to awaiting buyer confirmation.", actorId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        // Authorization: typically seller or an admin/system role from courier
        if (!delivery.getSellerId().equals(actorId) /* && !isSystemOrAdmin(actorId) */) {
            log.warn("User {} is not authorized to mark delivery {} as delivered.", actorId, deliveryId);
            throw new SecurityException("User not authorized for this action.");
        }

        if (delivery.getDeliveryStatus() != DeliveryStatus.SHIPPED_IN_TRANSIT &&
                delivery.getDeliveryStatus() != DeliveryStatus.ISSUE_REPORTED) { // Allow marking delivered after an issue was resolved
            log.warn("Delivery {} is not in SHIPPED_IN_TRANSIT or ISSUE_REPORTED state. Current status: {}. Cannot mark as delivered.", deliveryId, delivery.getDeliveryStatus());
            throw new IllegalStateException("Delivery cannot be marked as delivered from its current state.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.AWAITING_BUYER_CONFIRMATION); // NEW State
        delivery.setDeliveredAt(DateTimeUtil.roundToMicrosecond(LocalDateTime.now())); // Physical delivery time
        if (requestDto.getNotes() != null && !requestDto.getNotes().isBlank()) {
            String newNote = "Marked delivered by " + actorId + ": " + requestDto.getNotes();
            delivery.setNotes(delivery.getNotes() == null ? newNote : delivery.getNotes() + "; " + newNote);
        } else {
            String newNote = "Marked delivered by " + actorId + " at " + delivery.getDeliveredAt().toString();
            delivery.setNotes(delivery.getNotes() == null ? newNote : delivery.getNotes() + "; " + newNote);
        }


        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} status set to AWAITING_BUYER_CONFIRMATION by actor {}.", updatedDelivery.getDeliveryId(), actorId);

        // Publish new event instead of DeliveryDeliveredEvent directly
        publishDeliveryAwaitingBuyerConfirmationEvent(updatedDelivery);

        return updatedDelivery;
    }

    @Override
    @Transactional
    public Delivery confirmReceiptByBuyer(UUID deliveryId, String buyerId) {
        log.info("Buyer {} attempting to confirm receipt for delivery {}", buyerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));

        if (!delivery.getBuyerId().equals(buyerId)) {
            throw new SecurityException("User not authorized to confirm receipt for this delivery.");
        }
        if (delivery.getDeliveryStatus() != DeliveryStatus.AWAITING_BUYER_CONFIRMATION) {
            throw new IllegalStateException("Delivery is not awaiting buyer confirmation. Current status: " + delivery.getDeliveryStatus());
        }

        delivery.setDeliveryStatus(DeliveryStatus.RECEIPT_CONFIRMED_BY_BUYER);
        // delivery.setBuyerConfirmationAt(LocalDateTime.now()); // Optional: add new timestamp field
        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} receipt confirmed by buyer {}. Status: {}", savedDelivery.getDeliveryId(), buyerId, savedDelivery.getDeliveryStatus());

        publishDeliveryReceiptConfirmedByBuyerEvent(savedDelivery);
        return savedDelivery;
    }


    @Override
    @Transactional
    public Delivery requestReturnByBuyer(UUID deliveryId, String buyerId, ReturnRequestDto returnRequest) {
        log.info("Buyer {} requesting return for delivery {} with reason: {}", buyerId, deliveryId, returnRequest.getReason());
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        if (!delivery.getBuyerId().equals(buyerId)) {
            throw new SecurityException("User not authorized to request return for this delivery.");
        }
        if (delivery.getDeliveryStatus() != DeliveryStatus.AWAITING_BUYER_CONFIRMATION && delivery.getDeliveryStatus() != DeliveryStatus.RECEIPT_CONFIRMED_BY_BUYER && delivery.getDeliveryStatus() != DeliveryStatus.COMPLETED_AUTO) {
            throw new IllegalStateException("Return can only be requested after delivery is complete. Current status: " + delivery.getDeliveryStatus());
        }

        // Add logic here to check if within the 7-day window from delivery.getDeliveredAt()

        delivery.setDeliveryStatus(DeliveryStatus.RETURN_REQUESTED_BY_BUYER);
        delivery.setReturnReason(returnRequest.getReason());
        delivery.setReturnComments(returnRequest.getComments());
        delivery.setReturnCourier(returnRequest.getReturnCourier());
        delivery.setReturnTrackingNumber(returnRequest.getReturnTrackingNumber());

        // Convert image URL list to a JSON string to store in the TEXT column
        if (returnRequest.getImageUrls() != null && !returnRequest.getImageUrls().isEmpty()) {
            try {
                delivery.setReturnImageUrls(objectMapper.writeValueAsString(returnRequest.getImageUrls()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing image URLs to JSON for deliveryId {}: {}", deliveryId, e.getMessage());
                // Decide if this should throw an exception or just log
            }
        }

        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} return requested by buyer {}. Status: {}", savedDelivery.getDeliveryId(), buyerId, savedDelivery.getDeliveryStatus());

        publishDeliveryReturnRequestedEvent(savedDelivery, returnRequest.getReason(), returnRequest.getComments());
        return savedDelivery;
    }

    @Override
    @Transactional
    public void processAutoCompletion(UUID deliveryId, LocalDateTime originalConfirmationDeadline) {
        log.info("Processing auto-completion for delivery ID: {}. Original deadline: {}", deliveryId, originalConfirmationDeadline);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found for auto-completion: " + deliveryId));

        // CRITICAL CHECK: Only auto-complete if buyer hasn't acted yet
        if (delivery.getDeliveryStatus() == DeliveryStatus.AWAITING_BUYER_CONFIRMATION) {
            delivery.setDeliveryStatus(DeliveryStatus.COMPLETED_AUTO);
            String note = "Delivery auto-completed by system as buyer did not confirm within the window (Original deadline: " + originalConfirmationDeadline.toString() + ").";
            delivery.setNotes(delivery.getNotes() == null ? note : delivery.getNotes() + "; " + note);

            Delivery savedDelivery = deliveryRepository.save(delivery);
            log.info("Delivery {} auto-completed. Status set to COMPLETED_AUTO.", savedDelivery.getDeliveryId());

            publishDeliveryAutoCompletedEvent(savedDelivery);
        } else {
            log.info("Auto-completion for delivery {} skipped. Current status is {} (not AWAITING_BUYER_CONFIRMATION).",
                    deliveryId, delivery.getDeliveryStatus());
        }
    }

    @Override
    @Transactional
    public Delivery reportDeliveryIssue(UUID deliveryId, String sellerId, ReportDeliveryIssueRequestDto requestDto) {
        log.info("Seller {} attempting to report issue for delivery {}.", sellerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        if (!delivery.getSellerId().equals(sellerId)) {
            log.warn("User {} is not the seller for delivery {}. Cannot report issue.", sellerId, deliveryId);
            throw new SecurityException("User not authorized for this action.");
        }

        Set<DeliveryStatus> NON_REPORTABLE_STATUSES = Set.of(
                DeliveryStatus.RECEIPT_CONFIRMED_BY_BUYER,
                DeliveryStatus.COMPLETED_AUTO,
                DeliveryStatus.CANCELLED
        );
        if (NON_REPORTABLE_STATUSES.contains(delivery.getDeliveryStatus())) {
            log.warn("Cannot report issue for delivery {} as it is already {}." , deliveryId, delivery.getDeliveryStatus());
            throw new IllegalStateException("Cannot report issue for a delivery in its current state.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.ISSUE_REPORTED);
        delivery.setNotes(requestDto.getNotes()); // Overwrites or appends to existing notes as per business logic

        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Issue reported for delivery {}. Status: {}. Notes: {}",
                updatedDelivery.getDeliveryId(), updatedDelivery.getDeliveryStatus(), updatedDelivery.getNotes());

        publishDeliveryIssueReportedEvent(updatedDelivery);
        return updatedDelivery;
    }

    @Override
    @Transactional
    public Delivery approveReturnBySeller(UUID deliveryId, String sellerId) {
        log.info("Seller {} is approving the return for delivery {}", sellerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));

        if (!delivery.getSellerId().equals(sellerId)) {
            throw new SecurityException("User not authorized to approve returns for this delivery.");
        }
        if (delivery.getDeliveryStatus() != DeliveryStatus.RETURN_REQUESTED_BY_BUYER) {
            throw new IllegalStateException("Delivery is not awaiting return approval.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.RETURN_APPROVED_AWAITING_ITEM);
        delivery.setReturnApprovedAt(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()));
        Delivery savedDelivery = deliveryRepository.save(delivery);

        publishDeliveryReturnApprovedEvent(savedDelivery);

        log.info("Delivery {} return approved by seller. Status: {}", savedDelivery.getDeliveryId(), savedDelivery.getDeliveryStatus());
        return savedDelivery;
    }

    @Override
    @Transactional
    public Delivery confirmReturnItemReceived(UUID deliveryId, String sellerId) {
        log.info("Seller {} confirms receipt of returned item for delivery {}", sellerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found: " + deliveryId));

        if (!delivery.getSellerId().equals(sellerId)) {
            throw new SecurityException("User not authorized for this action.");
        }
        if (delivery.getDeliveryStatus() != DeliveryStatus.RETURN_REQUESTED_BY_BUYER) {
            throw new IllegalStateException("Delivery is not in the correct state to confirm return receipt. Expected RETURN_REQUESTED_BY_BUYER but was " + delivery.getDeliveryStatus());
        }

        delivery.setDeliveryStatus(DeliveryStatus.RETURN_ITEM_RECEIVED);
        delivery.setReturnItemReceivedAt(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()));
        Delivery savedDelivery = deliveryRepository.save(delivery);

        publishDeliveryReturnApprovedEvent(savedDelivery);
        publishRefundRequiredForReturnEvent(savedDelivery);

        log.info("Delivery {} return item received. Refund process initiated.", savedDelivery.getDeliveryId());
        return savedDelivery;
    }

    @Override
    @Transactional(readOnly = true)
    public Delivery getDeliveryByOrderId(UUID orderId, String userId) {
        log.debug("Fetching delivery details for order ID: {} by user: {}", orderId, userId);
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found for order ID: " + orderId));

        // Authorization: User must be buyer or seller for this delivery
        if (!delivery.getBuyerId().equals(userId) && !delivery.getSellerId().equals(userId)) {
            log.warn("User {} not authorized to view delivery for order {}", userId, orderId);
            throw new SecurityException("User not authorized for this delivery.");
        }
        return delivery; // Or deliveryMapper.toDeliveryDetailDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public Delivery getDeliveryById(UUID deliveryId, String userId) {
        log.debug("Fetching delivery details for delivery ID: {} by user: {}", deliveryId, userId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        // Authorization: User must be buyer or seller
        if (!delivery.getBuyerId().equals(userId) && !delivery.getSellerId().equals(userId)) {
            log.warn("User {} not authorized to view delivery {}", userId, deliveryId);
            throw new SecurityException("User not authorized for this delivery.");
        }
        return delivery; // Or deliveryMapper.toDeliveryDetailDto(delivery);
    }

    // --- Helper methods for publishing events ---
    private void publishDeliveryCreatedEvent(Delivery delivery) {
        String addressSummary = delivery.getShippingCity() + ", " + delivery.getShippingCountry();
        DeliveryCreatedEventDto event = DeliveryCreatedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .initialDeliveryStatus(delivery.getDeliveryStatus().name())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .shippingAddressSummary(addressSummary)
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_CREATED_ROUTING_KEY, event);
            log.info("Published DeliveryCreatedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryCreatedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryShippedEvent(Delivery delivery) {
        DeliveryShippedEventDto event = DeliveryShippedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .shippedAt(delivery.getShippedAt())
                .courierName(delivery.getCourierName())
                .trackingNumber(delivery.getTrackingNumber())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_SHIPPED_ROUTING_KEY, event);
            log.info("Published DeliveryShippedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryShippedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryAwaitingBuyerConfirmationEvent(Delivery delivery) {
        DeliveryAwaitingBuyerConfirmationEventDto event = DeliveryAwaitingBuyerConfirmationEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .deliveredAt(delivery.getDeliveredAt())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE,
                    RabbitMqConfig.DELIVERY_EVENT_AWAITING_BUYER_CONFIRMATION_ROUTING_KEY, // NEW ROUTING KEY
                    event
            );
            log.info("Published DeliveryAwaitingBuyerConfirmationEvent for deliveryId {}", delivery.getDeliveryId());

            if (delivery.getDeliveryStatus() == DeliveryStatus.AWAITING_BUYER_CONFIRMATION) {
                scheduleAutoCompletionCheck(delivery);
            }
        } catch (Exception e) {
            log.error("Error publishing DeliveryAwaitingBuyerConfirmationEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void scheduleAutoCompletionCheck(Delivery delivery) {
        LocalDateTime roundedNow = DateTimeUtil.roundToMicrosecond(LocalDateTime.now());
        LocalDateTime confirmationDeadline;

        // If basing deadline on when the item was marked delivered (AWAITING_BUYER_CONFIRMATION status set)
        if (delivery.getDeliveredAt() != null) {
            // delivery.getDeliveredAt() should already be rounded if set correctly in updateDeliveryStatusToDelivered
            confirmationDeadline = delivery.getDeliveredAt().plus(BUYER_CONFIRMATION_WINDOW);
        } else {
            // Fallback or if logic dictates using 'now' as base
            log.warn("Delivery {} has null deliveredAt timestamp when scheduling auto-completion. Using current time as base.", delivery.getDeliveryId());
            confirmationDeadline = roundedNow.plus(BUYER_CONFIRMATION_WINDOW);
        }
        LocalDateTime roundedConfirmationDeadline = DateTimeUtil.roundToMicrosecond(confirmationDeadline);

        long delayMillis = Duration.between(roundedNow, roundedConfirmationDeadline).toMillis();

        if (delayMillis > 0) {
            DeliveryWorkflowCommands.AutoCompleteDeliveryCommand command =
                    new DeliveryWorkflowCommands.AutoCompleteDeliveryCommand(delivery.getDeliveryId(), roundedConfirmationDeadline);

            log.info("Scheduling auto-completion check for delivery {} in {} ms (Deadline: {})",
                    delivery.getDeliveryId(), delayMillis, confirmationDeadline);

            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.DELIVERIES_SCHEDULE_EXCHANGE,
                        RabbitMqConfig.DELIVERY_AUTO_COMPLETE_SCHEDULE_ROUTING_KEY,
                        command,
                        message -> {
                            message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMillis, Integer.MAX_VALUE));
                            return message;
                        }
                );
            } catch (Exception e) {
                log.error("Error scheduling auto-completion check for delivery {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
            }
        } else {
            log.warn("Auto-completion check delay for delivery {} was not positive ({}ms). Consider immediate check or error.",
                    delivery.getDeliveryId(), delayMillis);
        }
    }

    private void publishDeliveryReceiptConfirmedByBuyerEvent(Delivery delivery) {
        DeliveryReceiptConfirmedByBuyerEventDto event = DeliveryReceiptConfirmedByBuyerEventDto.builder()
                .eventId(UUID.randomUUID()).eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId()).orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId()).sellerId(delivery.getSellerId())
                .confirmationTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now())) // Or use a field from delivery if you add it
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY, event);
        log.info("Published DeliveryReceiptConfirmedByBuyerEvent for deliveryId {}", delivery.getDeliveryId());
    }

    private void publishDeliveryReturnRequestedEvent(Delivery delivery, String reason, String comments) {
        DeliveryReturnRequestedEventDto event = DeliveryReturnRequestedEventDto.builder()
                .eventId(UUID.randomUUID()).eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId()).orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId()).sellerId(delivery.getSellerId())
                .reason(reason).comments(comments).requestTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_RETURN_REQUESTED_ROUTING_KEY, event);
        log.info("Published DeliveryReturnRequestedEvent for deliveryId {}", delivery.getDeliveryId());
    }

    private void publishDeliveryAutoCompletedEvent(Delivery delivery) {
        DeliveryAutoCompletedEventDto event = DeliveryAutoCompletedEventDto.builder()
                .eventId(UUID.randomUUID()).eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId()).orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId()).sellerId(delivery.getSellerId())
                .autoCompletionTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE,
                    RabbitMqConfig.DELIVERY_EVENT_AUTO_COMPLETED_ROUTING_KEY, // Ensure this key is in RabbitMqConfig
                    event
            );
            log.info("Published DeliveryAutoCompletedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryAutoCompletedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryIssueReportedEvent(Delivery delivery) {
        DeliveryIssueReportedEventDto event = DeliveryIssueReportedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId()) // Or based on who can report
                .sellerId(delivery.getSellerId()) // Or based on who can report
                .reporterId(delivery.getSellerId()) // Assuming seller reports for now
                .issueNotes(delivery.getNotes())
                .newStatus(delivery.getDeliveryStatus().name())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_ISSUE_REPORTED_ROUTING_KEY, event);
            log.info("Published DeliveryIssueReportedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryIssueReportedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryReturnApprovedEvent(Delivery delivery) {
        DeliveryReturnApprovedEventDto event = DeliveryReturnApprovedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .productInfoSnapshot(delivery.getProductInfoSnapshot())
                .returnApprovedAt(delivery.getReturnApprovedAt())
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE,
                    RabbitMqConfig.DELIVERY_EVENT_RETURN_APPROVED_ROUTING_KEY,
                    event
            );
            log.info("Published DeliveryReturnApprovedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryReturnApprovedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishRefundRequiredForReturnEvent(Delivery delivery) {
        RefundRequiredForReturnEventDto event = RefundRequiredForReturnEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(DateTimeUtil.roundToMicrosecond(LocalDateTime.now()))
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .reason("Return completed for delivery " + delivery.getDeliveryId())
                .build();
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE,
                    RabbitMqConfig.DELIVERY_EVENT_REFUND_REQUIRED_ROUTING_KEY,
                    event
            );
            log.info("Published RefundRequiredForReturnEvent for orderId {}", delivery.getOrderId());
        } catch (Exception e) {
            log.error("Error publishing RefundRequiredForReturnEvent for orderId {}: {}", delivery.getOrderId(), e.getMessage(), e);
        }
    }

}