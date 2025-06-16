package com.example.orders.listener;

import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.AuctionSoldEventDto; // Use the specific DTO
import com.example.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionSoldEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.ORDERS_AUCTION_ENDED_QUEUE)
    public void handleAuctionSoldEvent(@Payload AuctionSoldEventDto eventDto) {
        log.info("Received AuctionSoldEvent: auctionId={}, status={}",
                eventDto.getAuctionId(), eventDto.getFinalStatus());

        if ("SOLD".equalsIgnoreCase(eventDto.getFinalStatus())) {
            try {
                orderService.processAuctionSoldEvent(eventDto);
            } catch (Exception e) {
                log.error("Error processing AuctionSoldEvent for auction ID {}: {}", eventDto.getAuctionId(), e.getMessage(), e);
                throw e;
            }
        } else {
            log.info("Auction {} ended with status '{}'. No order will be created.",
                    eventDto.getAuctionId(), eventDto.getFinalStatus());
        }
    }
}