// File: com.example.deliveries.listener.OrderEventListener.java
package com.example.deliveries.listener;

import com.example.deliveries.config.RabbitMqConfig;
import com.example.deliveries.dto.event.OrderReadyForShippingEventDto;
import com.example.deliveries.service.DeliveryService; // <<< Import DeliveryService
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final DeliveryService deliveryService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_READY_FOR_SHIPPING_DELIVERY_QUEUE)
    public void handleOrderReadyForShipping(@Payload OrderReadyForShippingEventDto event) {
        log.info("Received OrderReadyForShippingEvent for orderId: {}. BuyerId: {}, SellerId: {}. Shipping to: {}, {}",
                event.getOrderId(), event.getBuyerId(), event.getSellerId(),
                event.getShippingStreetAddress(), event.getShippingCity());

        if (event.getOrderId() == null || event.getBuyerId() == null || event.getSellerId() == null ||
                event.getShippingRecipientName() == null || event.getShippingStreetAddress() == null ||
                event.getShippingCity() == null || event.getShippingPostalCode() == null ||
                event.getShippingCountry() == null) {
            log.error("OrderReadyForShippingEvent for orderId {} is missing crucial information. Cannot create delivery record.", event.getOrderId());
            // DLQ or alert
            return;
        }

        try {
            deliveryService.createDeliveryFromOrderEvent(event);
            log.info("Successfully processed OrderReadyForShippingEvent and initiated delivery creation for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing OrderReadyForShippingEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}