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
import java.util.Optional;
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
            webSocketEventPublisher.publishAuctionStateUpdate(updatedAuction, null);

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
        Optional<LiveAuction> opt = auctionRepository.findById(command.auctionId());

        if (opt.isEmpty()) {
            log.warn("END cmd for {}, but auction row is gone – ignoring", command.auctionId());
            return;                             // <— ACK the msg, no retry
        }

        LiveAuction auction = opt.get();

        /* IGNORE stale commands -------------------------------------------- */
        if (!command.fireAt().isEqual(auction.getEndTime())) {
            log.info("Stale END cmd for {} – fireAt {}, currentEnd {} – ignored",
                    command.auctionId(), command.fireAt(), auction.getEndTime());
            return;
        }

        /* ------------------------------------------------------------------- */

        // Idempotency check: Only proceed if currently ACTIVE
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setActualEndTime(LocalDateTime.now());

            // Determine final status and winner
            // AuctionLifecycleManager.handleEndAuctionCommand()
            if (auction.getHighestBidderId() != null) {
                boolean reserveSatisfied = auction.isReserveMet()
                        || auction.getReservePrice() == null; // NEW

                if (reserveSatisfied) {
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
                auction.setStatus(
                        AuctionStatus.RESERVE_NOT_MET); // same outcome: unsold
            }

            LiveAuction endedAuction = auctionRepository.save(auction);

            // Publish the final state update for WebSocket clients
            webSocketEventPublisher.publishAuctionStateUpdate(endedAuction, null);

            // TODO (Future): Publish a different event (e.g., AuctionConcludedEvent)
            // to trigger post-auction logic like notifications, payment processing etc.
            // rabbitTemplate.convertAndSend("post_auction_exchange", "auction.concluded." + auctionId, concludedEvent);

        } else {
            log.warn("Auction {} was not in ACTIVE state when end command received. Current state: {}. No action taken.",
                    auctionId, auction.getStatus());
        }
    }

    @RabbitListener(queues = RabbitMqConfig.AUCTION_CANCEL_QUEUE)
    @Transactional
    public void handleCancelCommand(CancelAuctionCommand cmd) {

        LiveAuction a = auctionRepository.findById(cmd.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        if (!a.getSellerId().equals(cmd.sellerId())) return; // belt-and-suspenders

        if (!(a.getStatus() == AuctionStatus.SCHEDULED
                || (a.getStatus() == AuctionStatus.ACTIVE))) {
            log.warn("Cancel ignored – auction {} wrong state {}", a.getId(), a.getStatus());
            return;
        }

        a.setStatus(AuctionStatus.CANCELLED);
        a.setActualEndTime(LocalDateTime.now());

        LiveAuction saved = auctionRepository.save(a);
        webSocketEventPublisher.publishAuctionStateUpdate(saved, null);

        log.info("Auction {} cancelled by seller {}", a.getId(), cmd.sellerId());
    }


    // Helper to schedule the end command (called from handleStartAuctionCommand)
    public void scheduleAuctionEnd(LiveAuction auction) {
        LocalDateTime now = LocalDateTime.now();
        long delayMillis = Duration.between(LocalDateTime.now(),
                auction.getEndTime()).toMillis();

        if (delayMillis > 0) {
            EndAuctionCommand command = new EndAuctionCommand(auction.getId(), auction.getEndTime());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.END_ROUTING_KEY,
                    command,
                    message -> {
                        // --- CORRECTED LINE ---
                        // Set the 'x-delay' header for the plugin
                        int delayHeaderValue = (int) Math.min(delayMillis, Integer.MAX_VALUE);
                        if (delayHeaderValue > 0) {
                            message.getMessageProperties().setHeader("x-delay", delayHeaderValue);
                            log.debug("Setting x-delay header for END command routingKey {}: {}", RabbitMqConfig.END_ROUTING_KEY, delayHeaderValue);
                        } else {
                            log.warn("Calculated end delay for auction {} is not positive ({}ms), not setting x-delay header.", auction.getId(), delayMillis);
                        }
                        // --- END CORRECTION ---
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
                handleEndAuctionCommand(new EndAuctionCommand(auction.getId(), auction.getEndTime()));
            } catch (Exception e) {
                log.error("Error during immediate end processing for auction {}", auction.getId(), e);
                // Consider retry logic or marking auction as ERRORED
            }
        }
    }
}