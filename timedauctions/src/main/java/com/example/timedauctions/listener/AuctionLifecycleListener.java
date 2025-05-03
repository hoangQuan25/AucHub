package com.example.timedauctions.listener;

import com.example.timedauctions.commands.AuctionLifecycleCommands; // Ensure these command records exist
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.event.NotificationEvents;
import com.example.timedauctions.exception.AuctionNotFoundException;
import com.example.timedauctions.repository.BidRepository;
import com.example.timedauctions.repository.TimedAuctionRepository;
// Import service if complex end logic is needed, otherwise repo is enough
// import com.example.timedauctions.service.TimedAuctionService;
import com.example.timedauctions.service.AuctionSchedulingService;
import com.example.timedauctions.service.TimedAuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleListener {

    private final TimedAuctionRepository timedAuctionRepository;
    private final AuctionSchedulingService auctionSchedulingService;
    private final RabbitTemplate rabbitTemplate;
    private final BidRepository bidRepository;


    @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_START_QUEUE)
    @Transactional
    public void handleAuctionStart(AuctionLifecycleCommands.StartAuctionCommand command) {
        log.info("Received start command for auction: {}", command.auctionId());
        TimedAuction auction = timedAuctionRepository.findById(command.auctionId())
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for start command: " + command.auctionId()));

        if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            // Check if start time is actually reached (belt-and-suspenders)
            if (!LocalDateTime.now().isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);
                timedAuctionRepository.save(auction);
                log.info("Auction {} status set to ACTIVE.", auction.getId());

                // Schedule the end now that it's active (moved from createAuction for SCHEDULED auctions)
                auctionSchedulingService.scheduleAuctionEnd(auction);; // Requires access to schedule logic (e.g., via service or helper)

                // Optional: Publish internal event
                // publishInternalEvent(auction, "STARTED");

            } else {
                log.warn("Received start command for auction {} but its start time {} is still in the future. Ignoring.",
                        auction.getId(), auction.getStartTime());
                // Optionally reschedule if timing seems off? Or rely on original schedule.
            }
        } else {
            log.warn("Received start command for auction {} but its status was already {}. Ignoring.",
                    auction.getId(), auction.getStatus());
        }
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

        Set<String> bidderIds = getUniqueBidderIds(endedAuction.getId());
        // --- Publish AuctionEndedEvent ---
        if (endedAuction != null && (endedAuction.getStatus() == AuctionStatus.SOLD || endedAuction.getStatus() == AuctionStatus.RESERVE_NOT_MET || endedAuction.getStatus() == AuctionStatus.CANCELLED)) {
            try {
                NotificationEvents.AuctionEndedEvent event = NotificationEvents.AuctionEndedEvent.builder()
                        .auctionId(endedAuction.getId())
                        .productTitleSnapshot(endedAuction.getProductTitleSnapshot()) // Ensure snapshot is loaded/available
                        .finalStatus(endedAuction.getStatus())
                        .actualEndTime(endedAuction.getActualEndTime())
                        .sellerId(endedAuction.getSellerId())
                        .winnerId(endedAuction.getWinnerId())
                        .winnerUsernameSnapshot(endedAuction.getHighestBidderUsernameSnapshot()) // Use highest if winner isn't set for non-sold
                        .winningBid(endedAuction.getWinningBid())
                        .allBidderIds(bidderIds)
                        .build();

                rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, RabbitMqConfig.AUCTION_ENDED_ROUTING_KEY, event);
                log.info("Published AuctionEndedEvent for auction {}", endedAuction.getId());
            } catch (Exception e) {
                log.error("Failed to publish AuctionEndedEvent for auction {}: {}", endedAuction.getId(), e.getMessage(), e);
                // Log error but allow transaction to commit the auction state change
            }
        }
        // --- End Publish ---
    }

    private Set<String> getUniqueBidderIds(UUID auctionId) {
        // Option A: From Bid history (visible bids)
        return bidRepository.findDistinctBidderIdsByTimedAuctionId(auctionId); // Add this method to BidRepository
        // Option B: From Proxy Bids (if you want anyone who ever placed a max bid)
        // return auctionProxyBidRepository.findDistinctBidderIdsByTimedAuctionId(auctionId); // Add this method to Proxy repo
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

        Set<String> bidderIds = getUniqueBidderIds(cancelledAuction.getId());
        // --- Publish AuctionEndedEvent (CANCELLED) ---
        if (cancelledAuction != null) {
            try {
                NotificationEvents.AuctionEndedEvent event = NotificationEvents.AuctionEndedEvent.builder()
                        .auctionId(cancelledAuction.getId())
                        .productTitleSnapshot(cancelledAuction.getProductTitleSnapshot())
                        .finalStatus(cancelledAuction.getStatus()) // CANCELLED
                        .actualEndTime(cancelledAuction.getActualEndTime())
                        .sellerId(cancelledAuction.getSellerId())
                        .allBidderIds(bidderIds)
                        .build();

                rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, RabbitMqConfig.AUCTION_ENDED_ROUTING_KEY, event);
                log.info("Published AuctionEndedEvent (CANCELLED) for auction {}", cancelledAuction.getId());
            } catch (Exception e) {
                log.error("Failed to publish AuctionEndedEvent (CANCELLED) for auction {}: {}", cancelledAuction.getId(), e.getMessage(), e);
            }
        }
        // --- End Publish ---
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

        // --- Publish AuctionEndedEvent (SOLD via Hammer) ---
        if (hammeredAuction != null) {
            try {
                Set<String> bidderIds = getUniqueBidderIds(hammeredAuction.getId());
                NotificationEvents.AuctionEndedEvent event = NotificationEvents.AuctionEndedEvent.builder()
                        .auctionId(hammeredAuction.getId())
                        .productTitleSnapshot(hammeredAuction.getProductTitleSnapshot())
                        .finalStatus(hammeredAuction.getStatus()) // SOLD
                        .actualEndTime(hammeredAuction.getActualEndTime())
                        .sellerId(hammeredAuction.getSellerId())
                        .winnerId(hammeredAuction.getWinnerId())
                        .winnerUsernameSnapshot(hammeredAuction.getHighestBidderUsernameSnapshot())
                        .winningBid(hammeredAuction.getWinningBid())
                        .allBidderIds(bidderIds)
                        .build();

                rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, RabbitMqConfig.AUCTION_ENDED_ROUTING_KEY, event);
                log.info("Published AuctionEndedEvent (Hammered) for auction {}", hammeredAuction.getId());
            } catch (Exception e) {
                log.error("Failed to publish AuctionEndedEvent (Hammered) for auction {}: {}", hammeredAuction.getId(), e.getMessage(), e);
            }
        }
        // --- End Publish ---
    }



}