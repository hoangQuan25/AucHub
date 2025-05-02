package com.example.timedauctions.listener;

import com.example.timedauctions.commands.AuctionLifecycleCommands; // Ensure these command records exist
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.exception.AuctionNotFoundException;
import com.example.timedauctions.repository.TimedAuctionRepository;
// Import service if complex end logic is needed, otherwise repo is enough
// import com.example.timedauctions.service.TimedAuctionService;
import com.example.timedauctions.service.AuctionSchedulingService;
import com.example.timedauctions.service.TimedAuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleListener {

    private final TimedAuctionRepository timedAuctionRepository;
    private final AuctionSchedulingService auctionSchedulingService;


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
                timedAuctionRepository.save(auction);

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

        if (auction.getStatus() == AuctionStatus.SCHEDULED || auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setStatus(AuctionStatus.CANCELLED);
            auction.setActualEndTime(LocalDateTime.now()); // Record when cancelled
            timedAuctionRepository.save(auction);
            log.info("Auction {} status set to CANCELLED.", auction.getId());
            // Optional: Publish internal "AuctionCancelled" event
            // publishInternalEvent(auction, "CANCELLED");
        } else {
            log.warn("Cancel cmd for auction {} ignored, status was already {}.", command.auctionId(), auction.getStatus());
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

        // --- Determine Final State ---
        // Ending early via "Hammer" implies SOLD, even if reserve wasn't technically met.
        // The seller is overriding the reserve requirement by choosing to end now.
        auction.setStatus(AuctionStatus.SOLD);
        auction.setWinnerId(auction.getHighestBidderId());
        auction.setWinningBid(auction.getCurrentBid()); // Sold at the current visible price
        auction.setActualEndTime(LocalDateTime.now()); // Record when hammered
        timedAuctionRepository.save(auction);
        log.info("Auction {} ended early (hammered). Status: SOLD. Winner: {}, Price: {}",
                auction.getId(), auction.getWinnerId(), auction.getWinningBid());

        log.debug("Scheduled end message for auction {} will be ignored by listener due to status change.", auction.getId());

    }



}