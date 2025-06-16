// com.example.users.listener.UserEventListener.java (New class)
package com.example.users.listener;

import com.example.users.dto.event.UserPaymentDefaultedEventDto;
import com.example.users.service.UserService;
import com.example.users.config.RabbitMqConfig; // Your Users service RabbitMqConfig
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final UserService userService;

    @RabbitListener(queues = RabbitMqConfig.USER_PAYMENT_DEFAULTED_QUEUE)
    public void handleUserPaymentDefaulted(UserPaymentDefaultedEventDto event) {
        log.info("Received UserPaymentDefaultedEvent: userId={}, orderId={}, attempt={}",
                event.getDefaultedUserId(), event.getOrderId(), event.getPaymentOfferAttempt());

        if (event.getPaymentOfferAttempt() == 1) { // Only apply ban logic for 1st winner defaults
            try {
                userService.processFirstWinnerPaymentDefault(event.getDefaultedUserId());
            } catch (Exception e) {
                log.error("Error processing first winner payment default for user {}: {}",
                        event.getDefaultedUserId(), e.getMessage(), e);
            }
        } else {
            log.info("Ignoring payment default for user {} as it was not the first winner (attempt={})",
                    event.getDefaultedUserId(), event.getPaymentOfferAttempt());
        }
    }
}