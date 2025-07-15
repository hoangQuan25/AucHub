package com.example.liveauctions.listener;

import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.dto.LiveAuctionStateDto;
import com.example.liveauctions.dto.event.AuctionStateUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(durable = "false", autoDelete = "true", exclusive = "true"), // Anonymous queue for this instance
            exchange = @Exchange(value = RabbitMqConfig.AUCTION_EVENTS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitMqConfig.UPDATE_ROUTING_KEY_PREFIX + "*" // Listen to all auction updates
    ))
    public void handleAuctionUpdate(AuctionStateUpdateEvent event) {
        if (event == null || event.getAuctionId() == null) {
            log.warn("Received invalid auction update event via RabbitMQ: {}", event);
            return;
        }
        log.debug("Received auction update event via RabbitMQ for auctionId: {}", event.getAuctionId());

        // Map event to the DTO clients expect
        LiveAuctionStateDto stateDto = LiveAuctionStateDto.builder()
                .auctionId(event.getAuctionId())
                .status(event.getStatus())
                .currentBid(event.getCurrentBid())
                .highestBidderId(event.getHighestBidderId())
                .highestBidderUsername(event.getHighestBidderUsername())
                .nextBidAmount(event.getNextBidAmount())
                .timeLeftMs(event.getTimeLeftMs())
                .endTime(event.getEndTime())
                .reserveMet(event.isReserveMet())
                .newBid(event.getNewBid())
                .winnerId(event.getWinnerId())
                .winningBid(event.getWinningBid())
                .build();

        String destination = "/topic/auctions/" + event.getAuctionId();

        try {
            messagingTemplate.convertAndSend(destination, stateDto);
            log.debug("Broadcasted state update via STOMP to destination '{}'", destination);

        } catch (Exception e) {
            log.error("Failed to broadcast STOMP message to destination {}: {}", destination, e.getMessage(), e);
        }
    }
}
