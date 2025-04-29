package com.example.liveauctions.service.impl; // Or a dedicated publisher package

import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.dto.LiveAuctionStateDto; // Use this for structure consistency
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.event.AuctionStateUpdateEvent;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.mapper.AuctionMapper;
import com.example.liveauctions.service.WebSocketEventPublisher;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventPublisherImpl implements WebSocketEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AuctionMapper auctionMapper;
    // Assuming getIncrement logic is accessible, maybe via a helper bean
    // private final AuctionHelper auctionHelper;

    @Override
    public void publishAuctionStateUpdate(LiveAuction auction, @Nullable Bid newBidEntity) {
        if (auction == null) {
            log.warn("Attempted to publish state update for null auction.");
            return;
        }

        try {
            // Calculate dynamic fields needed for the state DTO
            long timeLeftMs = 0;
            if (auction.getStatus() == AuctionStatus.ACTIVE && auction.getEndTime() != null) {
                timeLeftMs = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
                timeLeftMs = Math.max(0, timeLeftMs); // Ensure non-negative
            }

            BigDecimal nextBidAmount = null;
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                BigDecimal currentBid = auction.getCurrentBid() == null ? BigDecimal.ZERO : auction.getCurrentBid();
                if (auction.getHighestBidderId() == null) {
                    // If no bids yet, next bid is start price
                    nextBidAmount = auction.getStartPrice();
                } else if (auction.getCurrentBidIncrement() != null) {
                    // Otherwise, it's current + increment
                    nextBidAmount = currentBid.add(auction.getCurrentBidIncrement());
                } else {
                    // Fallback if increment somehow null - maybe recalculate?
                    log.warn("currentBidIncrement is null for active auction {}", auction.getId());
                    // BigDecimal increment = getIncrement(currentBid); // Recalculate if necessary
                    // nextBidAmount = currentBid.add(increment);
                    nextBidAmount = currentBid; // Or provide a sensible default/error indicator
                }
            }


            // Build the event payload (can reuse LiveAuctionStateDto structure or dedicated event DTO)
            AuctionStateUpdateEvent event = AuctionStateUpdateEvent.builder()
                    .auctionId(auction.getId())
                    .status(auction.getStatus())
                    .currentBid(auction.getCurrentBid())
                    .highestBidderId(auction.getHighestBidderId())
                    .highestBidderUsername(auction.getHighestBidderUsernameSnapshot()) // Use snapshot
                    .nextBidAmount(nextBidAmount) // Calculated
                    .endTime(auction.getEndTime())
                    .timeLeftMs(timeLeftMs) // Calculated
                    .reserveMet(auction.isReserveMet())
                    .newBid(newBidEntity == null ? null : auctionMapper.mapToBidDto(newBidEntity))
                    .winnerId(auction.getStatus() == AuctionStatus.SOLD ? auction.getWinnerId() : null)
                    .winningBid(auction.getStatus() == AuctionStatus.SOLD ? auction.getWinningBid() : null)
                    // -------------------------
                    .build();

            log.info("EVENT: {}", event);

            String routingKey = RabbitMqConfig.UPDATE_ROUTING_KEY_PREFIX + auction.getId();

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.AUCTION_EVENTS_EXCHANGE,
                    routingKey,
                    event
            );

            log.info("Published state update event for auction {} to exchange {} with key {}",
                    auction.getId(), RabbitMqConfig.AUCTION_EVENTS_EXCHANGE, routingKey);

        } catch (Exception e) {
            log.error("Failed to publish auction state update event for auction {}", auction.getId(), e);
            // Consider adding metrics/alerts here
        }
    }

    // Placeholder for getIncrement logic
    private BigDecimal getIncrement(BigDecimal currentBid) {
        // ... implement the tiered logic ...
        return BigDecimal.valueOf(5000);
    }
}