package com.example.orders.listener;

import com.example.orders.commands.OrderWorkflowCommands.CheckPaymentTimeoutCommand;
import com.example.orders.config.RabbitMqConfig;
import com.example.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentTimeoutListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_TIMEOUT_QUEUE)
    public void handleCheckPaymentTimeout(@Payload CheckPaymentTimeoutCommand command) {
        log.info("Received CheckPaymentTimeoutCommand for orderId: {}, attempt: {}, originalDeadline: {}",
                command.orderId(), command.attemptNumber(), command.originalPaymentDeadline());
        try {
            orderService.handlePaymentTimeout(command.orderId(), command.attemptNumber());
        } catch (Exception e) {
            log.error("Error processing CheckPaymentTimeoutCommand for order {}: {}", command.orderId(), e.getMessage(), e);
            throw e;
        }
    }
}