package com.example.deliveries.listener;

import com.example.deliveries.commands.DeliveryWorkflowCommands;
import com.example.deliveries.config.RabbitMqConfig;
import com.example.deliveries.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryWorkflowListener {

    private final DeliveryService deliveryService;

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_AUTO_COMPLETE_COMMAND_QUEUE)
    public void handleAutoCompleteDelivery(@Payload DeliveryWorkflowCommands.AutoCompleteDeliveryCommand command) {
        log.info("Received AutoCompleteDeliveryCommand for deliveryId: {}, originalDeadline: {}",
                command.deliveryId(), command.originalConfirmationDeadline());
        try {
            deliveryService.processAutoCompletion(command.deliveryId(), command.originalConfirmationDeadline());
        } catch (Exception e) {
            log.error("Error processing AutoCompleteDeliveryCommand for delivery {}: {}", command.deliveryId(), e.getMessage(), e);
            // DLQ strategy needed
            throw e;
        }
    }
}