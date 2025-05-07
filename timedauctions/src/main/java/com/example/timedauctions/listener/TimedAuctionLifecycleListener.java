package com.example.timedauctions.listener;

import com.example.timedauctions.commands.AuctionLifecycleCommands; // Ensure these command records exist
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.event.NotificationEvents;
import com.example.timedauctions.exception.AuctionNotFoundException;
import com.example.timedauctions.repository.TimedAuctionRepository;
import com.example.timedauctions.service.AuctionSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimedAuctionLifecycleListener {

    private final TimedAuctionRepository timedAuctionRepository;
    private final AuctionSchedulingService auctionSchedulingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_START_QUEUE)
    @Transactional
    public void handleAuctionStart(AuctionLifecycleCommands.StartAuctionCommand command) {
        log.info("Received start command for auction: {}", command.auctionId());
        TimedAuction auction = timedAuctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for start command: " + command.auctionId()));

        TimedAuction startedAuction = null;

        if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            // Check if start time is actually reached (belt-and-suspenders)
            if (!LocalDateTime.now().isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);
                startedAuction = timedAuctionRepository.save(auction);
                log.info("Auction {} status set to ACTIVE.", auction.getId());

                // Schedule the end now that it's active (moved from createAuction for SCHEDULED auctions)
                auctionSchedulingService.scheduleAuctionEnd(auction);; // Requires access to schedule logic (e.g., via service or helper)

            } else {
                log.warn("Received start command for auction {} but its start time {} is still in the future. Ignoring.",
                        auction.getId(), auction.getStartTime());
                // Optionally reschedule if timing seems off? Or rely on original schedule.
            }
        } else {
            log.warn("Received start command for auction {} but its status was already {}. Ignoring.",
                    auction.getId(), auction.getStatus());
        }

        // --- Publish AuctionStartedEvent ---
        if (startedAuction != null) {
            try {
                NotificationEvents.AuctionStartedEvent event = NotificationEvents.AuctionStartedEvent.builder()
                        .auctionId(startedAuction.getId())
                        .productTitleSnapshot(startedAuction.getProductTitleSnapshot())
                        .auctionType("TIMED") // Specify type
                        .sellerId(startedAuction.getSellerId())
                        .startTime(startedAuction.getStartTime())
                        .endTime(startedAuction.getEndTime())
                        .build();
                // Use specific routing key prefix + type + action
                String routingKey = RabbitMqConfig.AUCTION_STARTED_ROUTING_KEY_PREFIX + "timed.started";
                rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
                log.info("[Listener - Timed] Published AuctionStartedEvent for auction {}", startedAuction.getId());
            } catch (Exception e) {
                log.error("[Listener - Timed] Failed to publish AuctionStartedEvent for auction {}: {}", startedAuction.getId(), e.getMessage(), e);
            }
        }
        // --- End Publish ---
    }


    @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_END_QUEUE)
    @Transactional
    public void handleAuctionEnd(AuctionLifecycleCommands.EndAuctionCommand command) {
        log.info("Received end command for auction: {}", command.auctionId());
        TimedAuction auction = timedAuctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for end command: " + command.auctionId()));

        TimedAuction endedAuction = null; // To hold the saved state
        AuctionStatus originalStatus = auction.getStatus(); // Track original
        // Only process if the auction is currently ACTIVE

        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            // Double check end time (allow for small clock skew/delay)
            if (!LocalDateTime.now().isBefore(auction.getEndTime().minusSeconds(5))) { // Allow 5 sec grace?
                log.info("Processing end for auction {}. Current time >= end time {}.", auction.getId(), auction.getEndTime());

                // Determine final status
                if (auction.getHighestBidderId() != null) {
                    // Bids were placed
                    if (auction.isReserveMet()) {
                        auction.setStatus(AuctionStatus.SOLD);
                        auction.setWinnerId(auction.getHighestBidderId());
                        auction.setWinningBid(auction.getCurrentBid());
                        log.info("Auction {} ended. Status: SOLD. Winner: {}, Price: {}",
                                auction.getId(), auction.getWinnerId(), auction.getWinningBid());
                    } else {
                        auction.setStatus(AuctionStatus.RESERVE_NOT_MET);
                        log.info("Auction {} ended. Status: RESERVE_NOT_MET.", auction.getId());
                    }
                } else {
                    // No bids placed
                    // If start price acts as implicit reserve, this is RESERVE_NOT_MET
                    // Or could have a NO_BIDS status if desired
                    auction.setStatus(AuctionStatus.RESERVE_NOT_MET); // Assuming no bids = reserve not met
                    log.info("Auction {} ended. Status: RESERVE_NOT_MET (No bids placed).", auction.getId());
                }
                auction.setActualEndTime(LocalDateTime.now());
                endedAuction = timedAuctionRepository.save(auction);

                // Optional: Publish internal event
                // publishInternalEvent(auction, "ENDED");

            } else {
                log.warn("Received end command for auction {} but its end time {} is still in the future? Current time {}. Ignoring/Rescheduling?",
                        auction.getId(), auction.getEndTime(), LocalDateTime.now());
                // This might happen if clock skew is significant or if end was rescheduled.
                // Optionally reschedule based on auction.getEndTime() again.
            }

        } else {
            log.warn("Received end command for auction {} but its status was {}. Ignoring.",
                    auction.getId(), auction.getStatus());
        }

        if (endedAuction != null) {
            publishAuctionEndedNotification(endedAuction);
        }

    }

    @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_CANCEL_QUEUE)
    @Transactional
    public void handleCancelCommand(AuctionLifecycleCommands.CancelAuctionCommand command) {
        log.info("Processing CancelAuctionCommand for auction: {}", command.auctionId());
        TimedAuction auction = timedAuctionRepository.findById(command.auctionId())
                .orElse(null); // Find, don't throw if already gone

        if (auction == null) {
            log.warn("Cancel cmd for {}, but auction row is gone – ignoring", command.auctionId());
            return;
        }

        // Final validation checks (defense-in-depth)
        if (!auction.getSellerId().equals(command.sellerId())) {
            log.warn("Cancel cmd for auction {} ignored, seller ID mismatch.", command.auctionId());
            return; // Invalid requestor
        }

        TimedAuction cancelledAuction = null; // To hold saved state

        if (auction.getStatus() == AuctionStatus.SCHEDULED || auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setStatus(AuctionStatus.CANCELLED);
            auction.setActualEndTime(LocalDateTime.now()); // Record when cancelled
            cancelledAuction = timedAuctionRepository.save(auction);
            log.info("Auction {} status set to CANCELLED.", auction.getId());
            // Optional: Publish internal "AuctionCancelled" event
            // publishInternalEvent(auction, "CANCELLED");
        } else {
            log.warn("Cancel cmd for auction {} ignored, status was already {}.", command.auctionId(), auction.getStatus());
        }

        if (cancelledAuction != null) {
            publishAuctionEndedNotification(cancelledAuction);
        }

    }

    @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_HAMMER_QUEUE)
    @Transactional
    public void handleHammerDownCommand(AuctionLifecycleCommands.HammerDownCommand command) { // Reuse HammerDownCommand
        log.info("Processing HammerDownCommand (end early) for auction: {}", command.auctionId());
        TimedAuction auction = timedAuctionRepository.findById(command.auctionId())
                .orElse(null);

        if (auction == null) {
            log.warn("Hammer cmd for {}, but auction row is gone – ignoring", command.auctionId());
            return;
        }

        // Final validation checks
        if (!auction.getSellerId().equals(command.sellerId())) {
            log.warn("Hammer cmd for auction {} ignored, seller ID mismatch.", command.auctionId());
            return;
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            log.warn("Hammer cmd for auction {} ignored, status was {}.", command.auctionId(), auction.getStatus());
            return;
        }
        if (auction.getHighestBidderId() == null) {
            log.warn("Hammer cmd for auction {} ignored, no highest bidder.", command.auctionId());
            return; // Cannot hammer without bids
        }

        TimedAuction hammeredAuction = null; // To hold saved state

        // --- Determine Final State ---
        // Ending early via "Hammer" implies SOLD, even if reserve wasn't technically met.
        // The seller is overriding the reserve requirement by choosing to end now.
        auction.setStatus(AuctionStatus.SOLD);
        auction.setWinnerId(auction.getHighestBidderId());
        auction.setWinningBid(auction.getCurrentBid()); // Sold at the current visible price
        auction.setActualEndTime(LocalDateTime.now()); // Record when hammered
        hammeredAuction = timedAuctionRepository.save(auction);
        log.info("Auction {} ended early (hammered). Status: SOLD. Winner: {}, Price: {}",
                auction.getId(), auction.getWinnerId(), auction.getWinningBid());

        log.debug("Scheduled end message for auction {} will be ignored by listener due to status change.", auction.getId());

        publishAuctionEndedNotification(hammeredAuction);
    }

    private void publishAuctionEndedNotification(TimedAuction auction) {
        if (auction == null) return;

        // --- REMOVED bidder ID fetching ---

        NotificationEvents.AuctionEndedEvent event = NotificationEvents.AuctionEndedEvent.builder()
                .auctionId(auction.getId())
                .productTitleSnapshot(auction.getProductTitleSnapshot()) // Ensure snapshot is loaded/available
                .finalStatus(auction.getStatus())
                .actualEndTime(auction.getActualEndTime())
                .sellerId(auction.getSellerId())
                .winnerId(auction.getWinnerId()) // Null if not SOLD/CANCELLED
                .winnerUsernameSnapshot(auction.getHighestBidderUsernameSnapshot()) // Null if not SOLD/CANCELLED with bids
                .winningBid(auction.getWinningBid()) // Null if not SOLD
                // No allBidderIds field anymore
                .build();
        try {
            // Use specific routing key for timed auctions
            String routingKey = RabbitMqConfig.AUCTION_ENDED_ROUTING_KEY_PREFIX + "timed.ended";
            rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
            log.info("[Listener - Timed] Published AuctionEndedEvent (Status: {}) for auction {} to exchange {}",
                    event.getFinalStatus(), event.getAuctionId(), RabbitMqConfig.NOTIFICATIONS_EXCHANGE);
        } catch (Exception e) {
            log.error("[Listener - Timed] Failed to publish AuctionEndedEvent for timed auction {}: {}", auction.getId(), e.getMessage(), e);
            // Consider retry or dead-lettering for critical notifications
        }
    }
}