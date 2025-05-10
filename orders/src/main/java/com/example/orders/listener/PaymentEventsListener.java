package com.example.orders.listener;

// Assuming these DTOs are now available in the Orders service
// (either copied or via a shared library)
import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.PaymentSucceededEventDto;
import com.example.orders.dto.event.PaymentFailedEventDto;
import com.example.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventsListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDERS_PAYMENT_SUCCEEDED_QUEUE)
    public void handlePaymentSucceeded(@Payload PaymentSucceededEventDto eventDto) {
        log.info("Received PaymentSucceededEvent for order ID: {}, paymentIntentId: {}",
                eventDto.getOrderId(), eventDto.getPaymentIntentId());
        try {
            orderService.processPaymentSuccess(eventDto);
        } catch (Exception e) {
            log.error("Error processing PaymentSucceededEvent for order {}: {}", eventDto.getOrderId(), e.getMessage(), e);
            // Consider DLQ strategy / rethrowing to leverage Spring AMQP default retries
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDERS_PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(@Payload PaymentFailedEventDto eventDto) {
        log.info("Received PaymentFailedEvent for order ID: {}, paymentIntentId: {}, reason: {}",
                eventDto.getOrderId(), eventDto.getPaymentIntentId(), eventDto.getFailureMessage());
        try {
            orderService.processPaymentFailure(eventDto);
        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent for order {}: {}", eventDto.getOrderId(), e.getMessage(), e);
            // Consider DLQ strategy
            throw e;
        }
    }
}