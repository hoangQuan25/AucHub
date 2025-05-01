package com.example.timedauctions.listener;

import com.example.timedauctions.commands.AuctionLifecycleCommands; // Ensure these command records exist
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.exception.AuctionNotFoundException;
import com.example.timedauctions.repository.TimedAuctionRepository;
// Import service if complex end logic is needed, otherwise repo is enough
// import com.example.timedauctions.service.TimedAuctionService;
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
    // Inject RabbitTemplate only if the listener needs to send *new* messages (e.g., reschedule end)
    // private final RabbitTemplate rabbitTemplate;
    // Inject Service if more complex logic beyond status update is needed
    // private final TimedAuctionService timedAuctionService;


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
                scheduleAuctionEnd(auction); // Requires access to schedule logic (e.g., via service or helper)

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

    // --- TODO: Add listener for Cancel Command if needed ---
    // @RabbitListener(queues = RabbitMqConfig.TD_AUCTION_CANCEL_QUEUE)
    // @Transactional
    // public void handleAuctionCancel(AuctionLifecycleCommands.CancelAuctionCommand command) { ... }


    // --- Helper to Reschedule End (needs RabbitTemplate) ---
    // If rescheduling happens inside listener, inject RabbitTemplate
    // private final RabbitTemplate rabbitTemplate; // Inject if needed

    private void scheduleAuctionEnd(TimedAuction auction) {
        // NOTE: This duplicates logic from the service. Consider a shared SchedulingService
        // or making scheduleAuctionEnd public in TimedAuctionService and injecting the service here.
        // For now, simplified logic: Assume rescheduling isn't done *from* listener easily without service call.
        // If scheduleAuctionEnd is only ever called from the ServiceImpl, this method isn't needed here.
        log.warn("Placeholder: scheduleAuctionEnd called from Listener - requires RabbitTemplate/Service injection if actually needed here.");
    }

    // --- Helper to Publish Internal Event (needs RabbitTemplate) ---
    private void publishInternalEvent(TimedAuction auction, String eventType) {
        log.warn("Placeholder: publishInternalEvent called from Listener - requires RabbitTemplate injection if actually needed here.");
        // rabbitTemplate.convertAndSend(RabbitMqConfig.TD_AUCTION_EVENTS_EXCHANGE, "td.auction.event." + eventType.toLowerCase(), eventPayload);
    }

}