package com.example.orders.listener;

import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.UserUpdatedEventDto;
import com.example.orders.entity.Order;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventsListener {

    private final OrderRepository orderRepository;

    @Transactional
    @RabbitListener(queues = RabbitMqConfig.USER_UPDATED_QUEUE_FOR_ORDERS)
    public void handleUserUpdatedEvent(UserUpdatedEventDto event) {
        if (event == null || event.getUpdatedUser() == null || event.getUpdatedUser().getId() == null) {
            log.error("Received invalid UserUpdatedEventDto: {}", event);
            return;
        }
        String userId = event.getUpdatedUser().getId();
        log.info("Received UserUpdatedEvent for userId={}", userId);

        List<Order> sellerOrders = orderRepository.findBySellerId(userId);
        for (Order order : sellerOrders) {
            order.setSellerUsernameSnapshot(event.getUpdatedUser().getUsername());
        }

        List<Order> bidderOrders = orderRepository.findByCurrentBidderId(userId);
        for (Order order : bidderOrders) {
        }

        orderRepository.saveAll(sellerOrders);
        orderRepository.saveAll(bidderOrders);

        log.info("Updated {} seller orders and {} bidder orders for userId={}", sellerOrders.size(), bidderOrders.size(), userId);
    }
}
