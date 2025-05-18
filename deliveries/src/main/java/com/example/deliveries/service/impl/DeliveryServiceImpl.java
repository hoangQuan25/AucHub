// File: com.example.deliveries.service.impl.DeliveryServiceImpl.java
package com.example.deliveries.service.impl;

import com.example.deliveries.config.RabbitMqConfig;
import com.example.deliveries.dto.event.DeliveryCreatedEventDto;
import com.example.deliveries.dto.event.DeliveryDeliveredEventDto;
import com.example.deliveries.dto.event.DeliveryIssueReportedEventDto;
import com.example.deliveries.dto.event.DeliveryShippedEventDto;
import com.example.deliveries.dto.event.OrderReadyForShippingEventDto;
import com.example.deliveries.dto.request.MarkAsShippedRequestDto;
import com.example.deliveries.dto.request.ReportDeliveryIssueRequestDto;
import com.example.deliveries.dto.request.UpdateToDeliveredRequestDto;
import com.example.deliveries.entity.Delivery;
import com.example.deliveries.entity.DeliveryStatus;
import com.example.deliveries.repository.DeliveryRepository;
import com.example.deliveries.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RabbitTemplate rabbitTemplate;

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
        delivery.setShippedAt(LocalDateTime.now());
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
    public Delivery updateDeliveryStatusToDelivered(UUID deliveryId, String sellerId, UpdateToDeliveredRequestDto requestDto) {
        log.info("Seller {} attempting to mark delivery {} as delivered.", sellerId, deliveryId);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NoSuchElementException("Delivery not found with ID: " + deliveryId));

        if (!delivery.getSellerId().equals(sellerId)) {
            log.warn("User {} is not the seller for delivery {}. Cannot mark as delivered.", sellerId, deliveryId);
            throw new SecurityException("User not authorized for this action.");
        }

        if (delivery.getDeliveryStatus() != DeliveryStatus.SHIPPED_IN_TRANSIT &&
                delivery.getDeliveryStatus() != DeliveryStatus.ISSUE_REPORTED) { // Allow marking delivered after an issue was resolved
            log.warn("Delivery {} is not in SHIPPED_IN_TRANSIT or ISSUE_REPORTED state. Current status: {}. Cannot mark as delivered by seller.", deliveryId, delivery.getDeliveryStatus());
            throw new IllegalStateException("Delivery cannot be marked as delivered from its current state by seller.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        if (requestDto.getNotes() != null && !requestDto.getNotes().isBlank()) {
            delivery.setNotes(delivery.getNotes() == null ? requestDto.getNotes() : delivery.getNotes() + "; Delivered: " + requestDto.getNotes());
        }

        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} marked as DELIVERED by seller.", updatedDelivery.getDeliveryId());

        publishDeliveryDeliveredEvent(updatedDelivery);
        return updatedDelivery;
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

        // Allow reporting issue from most active states, but not if already resolved or cancelled
        if (delivery.getDeliveryStatus() == DeliveryStatus.DELIVERED || delivery.getDeliveryStatus() == DeliveryStatus.CANCELLED) {
            log.warn("Cannot report issue for delivery {} as it is already {}." , deliveryId, delivery.getDeliveryStatus());
            throw new IllegalStateException("Cannot report issue for a delivery that is already delivered or cancelled.");
        }

        delivery.setDeliveryStatus(DeliveryStatus.ISSUE_REPORTED);
        delivery.setNotes(requestDto.getNotes()); // Overwrites or appends to existing notes as per business logic

        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Issue reported for delivery {}. Status: {}. Notes: {}",
                updatedDelivery.getDeliveryId(), updatedDelivery.getDeliveryStatus(), updatedDelivery.getNotes());

        publishDeliveryIssueReportedEvent(updatedDelivery);
        return updatedDelivery;
    }

    // --- Helper methods for publishing events ---
    private void publishDeliveryCreatedEvent(Delivery delivery) {
        String addressSummary = delivery.getShippingCity() + ", " + delivery.getShippingCountry();
        DeliveryCreatedEventDto event = DeliveryCreatedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
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
                .eventTimestamp(LocalDateTime.now())
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .shippedAt(delivery.getShippedAt())
                .courierName(delivery.getCourierName())
                .trackingNumber(delivery.getTrackingNumber())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_SHIPPED_ROUTING_KEY, event);
            log.info("Published DeliveryShippedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryShippedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryDeliveredEvent(Delivery delivery) {
        DeliveryDeliveredEventDto event = DeliveryDeliveredEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId())
                .sellerId(delivery.getSellerId())
                .deliveredAt(delivery.getDeliveredAt())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_DELIVERED_ROUTING_KEY, event);
            log.info("Published DeliveryDeliveredEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryDeliveredEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }

    private void publishDeliveryIssueReportedEvent(Delivery delivery) {
        DeliveryIssueReportedEventDto event = DeliveryIssueReportedEventDto.builder()
                .eventId(UUID.randomUUID())
                .eventTimestamp(LocalDateTime.now())
                .deliveryId(delivery.getDeliveryId())
                .orderId(delivery.getOrderId())
                .buyerId(delivery.getBuyerId()) // Or based on who can report
                .sellerId(delivery.getSellerId()) // Or based on who can report
                .reporterId(delivery.getSellerId()) // Assuming seller reports for now
                .issueNotes(delivery.getNotes())
                .newStatus(delivery.getDeliveryStatus().name())
                .build();
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.DELIVERIES_EVENTS_EXCHANGE, RabbitMqConfig.DELIVERY_EVENT_ISSUE_REPORTED_ROUTING_KEY, event);
            log.info("Published DeliveryIssueReportedEvent for deliveryId {}", delivery.getDeliveryId());
        } catch (Exception e) {
            log.error("Error publishing DeliveryIssueReportedEvent for deliveryId {}: {}", delivery.getDeliveryId(), e.getMessage(), e);
        }
    }
}