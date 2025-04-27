package com.example.liveauctions.listener; // Or manager, component package

import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.dto.LiveAuctionStateDto; // The DTO structure clients expect
import com.example.liveauctions.events.AuctionStateUpdateEvent; // The event received
import com.example.liveauctions.websocket.WebSocketSessionManager; // Your session manager
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

    // Placeholder: Manages sessions ONLY for the current service instance
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper; // For converting DTO to JSON

    // Listens to an instance-specific anonymous queue bound to the main events exchange
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    // value = "${spring.application.name}-auction-updates", // Explicit name if needed, else anonymous
                    durable = "false", autoDelete = "true", exclusive = "true"), // Instance-specific queue
            exchange = @Exchange(value = RabbitMqConfig.AUCTION_EVENTS_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = RabbitMqConfig.UPDATE_ROUTING_KEY_PREFIX + "*" // Listen to all auction updates
    ))
    public void handleAuctionUpdate(AuctionStateUpdateEvent event) {
        if (event == null || event.getAuctionId() == null) {
            log.warn("Received invalid auction update event via RabbitMQ: {}", event);
            return;
        }
        log.debug("Received auction update event via RabbitMQ for auctionId: {}", event.getAuctionId());

        // Check if this instance has active clients for this auction
        if (!sessionManager.hasActiveSessions(event.getAuctionId())) {
            log.trace("No active WebSocket sessions for auction {} on this instance. Ignoring event.", event.getAuctionId());
            return;
        }

        // Map event to the DTO clients expect (maybe identical or slightly different)
        LiveAuctionStateDto stateDto = LiveAuctionStateDto.builder()
                .auctionId(event.getAuctionId())
                .status(event.getStatus())
                .currentBid(event.getCurrentBid())
                .highestBidderId(event.getHighestBidderId())
                .highestBidderUsername(event.getHighestBidderUsername())
                .nextBidAmount(event.getNextBidAmount())
                .timeLeftMs(event.getTimeLeftMs())
                .reserveMet(event.isReserveMet())
                .build();

        try {
            // Serialize DTO to JSON
            String payload = objectMapper.writeValueAsString(stateDto);

            // Broadcast payload to clients connected for this auction *on this instance*
            sessionManager.broadcastToAuction(event.getAuctionId(), payload);
            log.debug("Broadcasted state update via WebSocket for auction {}", event.getAuctionId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize auction state DTO for WebSocket broadcast for auction {}", event.getAuctionId(), e);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message for auction {}", event.getAuctionId(), e);
        }
    }
}