// package com.example.deliveries.commands;
package com.example.deliveries.commands; // Or your preferred package

import java.time.LocalDateTime;
import java.util.UUID;

public interface DeliveryWorkflowCommands {

    record AutoCompleteDeliveryCommand(
            UUID deliveryId,
            LocalDateTime originalConfirmationDeadline // For context/logging
    ) {}

}