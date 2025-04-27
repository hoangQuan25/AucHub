package com.example.liveauctions.manager; // Or service package

import com.example.liveauctions.commands.AuctionLifecycleCommands.*; // Import the records
import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.exception.AuctionNotFoundException;
import com.example.liveauctions.repository.LiveAuctionRepository;
import com.example.liveauctions.service.WebSocketEventPublisher; // Import the publisher
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleManager {

    private final LiveAuctionRepository auctionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final WebSocketEventPublisher webSocketEventPublisher; // Inject publisher

    @RabbitListener(queues = RabbitMqConfig.AUCTION_START_QUEUE)
    @Transactional // Keep state change and scheduling atomic
    public void handleStartAuctionCommand(StartAuctionCommand command) {
        UUID auctionId = command.auctionId();
        log.info("Processing StartAuctionCommand for auctionId: {}", auctionId);

        // Consider Pessimistic Lock if high concurrency expected on start
        LiveAuction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for start command: " + auctionId));

        // Idempotency check: Only proceed if still SCHEDULED
        if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            auction.setStatus(AuctionStatus.ACTIVE);
            // Optionally set actual start time more precisely if needed:
            // auction.setActualStartTime(LocalDateTime.now());

            LiveAuction updatedAuction = auctionRepository.save(auction);
            log.info("Auction {} status set to ACTIVE", auctionId);

            // Schedule the end of the auction now that it's active
            scheduleAuctionEnd(updatedAuction);

            // Publish the state update for WebSocket clients
            webSocketEventPublisher.publishAuctionStateUpdate(updatedAuction);

        } else {
            log.warn("Auction {} was not in SCHEDULED state when start command received. Current state: {}. No action taken.",
                    auctionId, auction.getStatus());
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_END_QUEUE)
    @Transactional // Keep state change and notification atomic
    public void handleEndAuctionCommand(EndAuctionCommand command) {
        UUID auctionId = command.auctionId();
        log.info("Processing EndAuctionCommand for auctionId: {}", auctionId);

        // Consider Pessimistic Lock
        LiveAuction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for end command: " + auctionId));

        // Idempotency check: Only proceed if currently ACTIVE
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setActualEndTime(LocalDateTime.now());

            // Determine final status and winner
            if (auction.getHighestBidderId() != null) { // Check if there were any bids
                if (auction.isReserveMet()) {
                    auction.setStatus(AuctionStatus.SOLD);
                    auction.setWinnerId(auction.getHighestBidderId());
                    auction.setWinningBid(auction.getCurrentBid());
                    log.info("Auction {} ended. Status: SOLD. Winner: {}, Bid: {}",
                            auctionId, auction.getWinnerId(), auction.getWinningBid());
                } else {
                    auction.setStatus(AuctionStatus.RESERVE_NOT_MET);
                    log.info("Auction {} ended. Status: RESERVE_NOT_MET.", auctionId);
                }
            } else {
                // No bids were placed
                auction.setStatus(AuctionStatus.RESERVE_NOT_MET); // Or a dedicated NO_BIDS status
                log.info("Auction {} ended. Status: RESERVE_NOT_MET (No bids).", auctionId);
            }

            LiveAuction endedAuction = auctionRepository.save(auction);

            // Publish the final state update for WebSocket clients
            webSocketEventPublisher.publishAuctionStateUpdate(endedAuction);

            // TODO (Future): Publish a different event (e.g., AuctionConcludedEvent)
            // to trigger post-auction logic like notifications, payment processing etc.
            // rabbitTemplate.convertAndSend("post_auction_exchange", "auction.concluded." + auctionId, concludedEvent);

        } else {
            log.warn("Auction {} was not in ACTIVE state when end command received. Current state: {}. No action taken.",
                    auctionId, auction.getStatus());
        }
    }

    // Helper to schedule the end command (called from handleStartAuctionCommand)
    private void scheduleAuctionEnd(LiveAuction auction) {
        LocalDateTime now = LocalDateTime.now();
        long delayMillis = Duration.between(now, auction.getEndTime()).toMillis();

        if (delayMillis > 0) {
            EndAuctionCommand command = new EndAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.END_ROUTING_KEY,
                    command,
                    message -> {
                        message.getMessageProperties().setDelay((int) Math.min(delayMillis, Integer.MAX_VALUE));
                        return message;
                    }
            );
            log.info("Scheduled auction end for auctionId: {} with delay: {} ms", auction.getId(), delayMillis);
        } else {
            // End time is already past or now, trigger immediate end processing
            log.warn("Auction {} end time is not in the future ({}), triggering immediate end processing.", auction.getId(), auction.getEndTime());
            // Run in a separate transaction maybe? Or handle potential recursion carefully.
            // For simplicity now, directly call the handler (beware of transactional context)
            try {
                handleEndAuctionCommand(new EndAuctionCommand(auction.getId()));
            } catch (Exception e) {
                log.error("Error during immediate end processing for auction {}", auction.getId(), e);
                // Consider retry logic or marking auction as ERRORED
            }
        }
    }
}