package com.example.orders.commands; // Or your preferred package for commands

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderWorkflowCommands {

    /**
     * Command sent to a delayed queue, to be consumed by the Orders service
     * to check if payment for an order has been made by its deadline.
     */
    record CheckPaymentTimeoutCommand(
            UUID orderId,
            LocalDateTime originalPaymentDeadline, // For context/logging when the command is processed
            int attemptNumber // Which payment attempt this timeout is for (e.g., 1 for winner, 2 for 2nd bidder)
    ) {}

    // Other internal commands for the Orders service workflow can be added here later.
}