// In com.example.orders.listener.DeliveryEventListener.java (OrdersService)
package com.example.orders.listener;

import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.DeliveryReceiptConfirmedByBuyerEventDto;
import com.example.orders.dto.event.RefundRequiredForReturnEventDto;
import com.example.orders.entity.Order;
import com.example.orders.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDERS_DELIVERY_RECEIPT_CONFIRMED_QUEUE)
    public void handleDeliveryReceiptConfirmedByBuyer(@Payload DeliveryReceiptConfirmedByBuyerEventDto event) {
        log.info("Received DeliveryReceiptConfirmedByBuyerEvent for orderId: {}, deliveryId: {}",
                event.getOrderId(), event.getDeliveryId());
        try {
            orderService.processOrderCompletionByBuyer(event.getOrderId(), event.getBuyerId(), event.getConfirmationTimestamp());
        } catch (Exception e) {
            log.error("Error processing DeliveryReceiptConfirmedByBuyerEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
            throw e; // Or handle more gracefully
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDERS_REFUND_REQUIRED_QUEUE)
    @Transactional
    public void handleRefundRequiredForReturn(RefundRequiredForReturnEventDto event) {
        log.info("Received refund requirement for orderId {} from deliveryId {}", event.getOrderId(), event.getDeliveryId());
        try {
            orderService.processRefundRequiredForReturnEvent(event);
        } catch (Exception e) {
            log.error("Error processing RefundRequiredForReturnEvent for order {}: {}", event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }
}