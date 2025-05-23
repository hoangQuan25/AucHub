// In com.example.orders.listener.DeliveryEventListener.java (OrdersService)
package com.example.orders.listener;

import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.DeliveryReceiptConfirmedByBuyerEventDto;
import com.example.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
            // Implement appropriate error handling (e.g., DLQ)
            // For now, rethrowing might leverage default Spring AMQP retry/DLQ if configured.
            throw e; // Or handle more gracefully
        }
    }

    // Later, you'll add another listener here for DeliveryAutoCompletedEvent
    // @RabbitListener(queues = RabbitMqConfig.ORDERS_DELIVERY_AUTO_COMPLETED_QUEUE)
    // public void handleDeliveryAutoCompleted(@Payload DeliveryAutoCompletedEventDto event) { ... }
}