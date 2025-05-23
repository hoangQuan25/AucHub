// package com.example.deliveries.commands;
package com.example.deliveries.commands; // Or your preferred package

import java.time.LocalDateTime;
import java.util.UUID;

public interface DeliveryWorkflowCommands {

    /**
     * Command sent to a delayed queue, to be consumed by DeliveriesService
     * to check if a delivery (awaiting buyer confirmation) should be auto-completed.
     */
    record AutoCompleteDeliveryCommand(
            UUID deliveryId,
            LocalDateTime originalConfirmationDeadline // For context/logging
    ) {}

    // Other internal commands for Delivery service workflow can be added here.
}