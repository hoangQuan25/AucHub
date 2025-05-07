package com.example.liveauctions.listener;

import com.example.liveauctions.commands.AuctionLifecycleCommands.*; // Import commands
import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.event.NotificationEvents;
import com.example.liveauctions.exception.AuctionNotFoundException;
import com.example.liveauctions.repository.BidRepository;
import com.example.liveauctions.repository.LiveAuctionRepository;
import com.example.liveauctions.service.LiveAuctionSchedulingService; // Import new service
import com.example.liveauctions.service.WebSocketEventPublisher; // Import publisher
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveAuctionLifecycleListener {

    private final LiveAuctionRepository auctionRepository;
    private final LiveAuctionSchedulingService schedulingService;
    private final WebSocketEventPublisher webSocketEventPublisher;
    private final BidRepository bidRepository;

    private final RabbitTemplate rabbitTemplate;

    // --- Listener for Start Command ---
    @RabbitListener(queues = RabbitMqConfig.AUCTION_START_QUEUE)
    @Transactional
    public void handleStartAuctionCommand(StartAuctionCommand command) {
        // Logic from AuctionLifecycleManager
        UUID auctionId = command.auctionId();
        log.info("[Listener - Live] Processing StartAuctionCommand for auctionId: {}", auctionId);
        LiveAuction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found for start command: " + auctionId));

        if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            // Check start time just in case message was delayed significantly
            if (!LocalDateTime.now().isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);
                LiveAuction updatedAuction = auctionRepository.save(auction);
                log.info("[Listener - Live] Auction {} status set to ACTIVE", auctionId);
                // Use scheduling service to schedule the end
                schedulingService.scheduleAuctionEnd(updatedAuction);
                webSocketEventPublisher.publishAuctionStateUpdate(updatedAuction, null);

                try {
                    NotificationEvents.AuctionStartedEvent event = NotificationEvents.AuctionStartedEvent.builder()
                            .auctionId(updatedAuction.getId())
                            .productTitleSnapshot(updatedAuction.getProductTitleSnapshot())
                            .auctionType("LIVE") // Hardcode or derive type
                            .sellerId(updatedAuction.getSellerId())
                            .startTime(updatedAuction.getStartTime())
                            .endTime(updatedAuction.getEndTime())
                            .build();
                    String routingKey = RabbitMqConfig.AUCTION_STARTED_ROUTING_KEY_PREFIX + "live.started";
                    rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
                    log.info("[Listener - Live] Published AuctionStartedEvent for auction {}", updatedAuction.getId());
                    log.info("[Listener - Live] Published AuctionStartedEvent to exchange {} with routing key {}",
                            RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey);
                    log.info("[Listener - Live] Published AuctionStartedEvent for auction {}", event);
                } catch (Exception e) {
                    log.error("[Listener - Live] Failed to publish AuctionStartedEvent for auction {}: {}", updatedAuction.getId(), e.getMessage(), e);
                }
            } else {
                log.warn("[Listener - Live] Start command for auction {} received early. Start time: {}. Re-queueing/Ignoring? (Currently ignored)", auctionId, auction.getStartTime());
                // Basic behavior is ACK and ignore. More robust might re-schedule with shorter delay.
            }
        } else {
            log.warn("[Listener - Live] Auction {} not SCHEDULED when start command received. Status: {}. Ignoring.", auctionId, auction.getStatus());
        }
    }

    // --- Listener for End Command ---
    @RabbitListener(queues = RabbitMqConfig.AUCTION_END_QUEUE)
    @Transactional
    public void handleEndAuctionCommand(EndAuctionCommand command) {
        // Logic from AuctionLifecycleManager (with fixed stale check)
        UUID auctionId = command.auctionId();
        log.info("[Listener - Live] Processing EndAuctionCommand for auctionId: {}", auctionId);

        Optional<LiveAuction> opt = auctionRepository.findById(auctionId);
        if (opt.isEmpty()) { log.warn("END cmd for {}, but auction row gone – ignoring", auctionId); return; }
        LiveAuction auction = opt.get();

        /* Stale Check (using truncated comparison) */
        LocalDateTime fireAtFromCommand = command.fireAt();
        LocalDateTime endTimeFromDb = auction.getEndTime();
        if (fireAtFromCommand == null || endTimeFromDb == null) { log.warn("Stale check failed for {}: null time(s)", auctionId); return; }
        LocalDateTime truncatedFireAt = fireAtFromCommand.truncatedTo(ChronoUnit.MICROS);
        LocalDateTime truncatedEndTime = endTimeFromDb.truncatedTo(ChronoUnit.MICROS);
        if (!truncatedFireAt.isEqual(truncatedEndTime)) { log.info("Stale END cmd for {} ignored.", auctionId); return; }
        log.info("Stale check passed for auction {}.", command.auctionId());
        /* End Stale Check */

        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setActualEndTime(LocalDateTime.now());
            // Determine final status based on bids and reserve
            if (auction.getHighestBidderId() != null) { // Bids placed
                boolean reserveSatisfied = auction.isReserveMet() || auction.getReservePrice() == null;
                auction.setStatus(reserveSatisfied ? AuctionStatus.SOLD : AuctionStatus.RESERVE_NOT_MET);
                if(auction.getStatus() == AuctionStatus.SOLD) {
                    auction.setWinnerId(auction.getHighestBidderId());
                    auction.setWinningBid(auction.getCurrentBid());
                }
            } else { // No bids placed
                auction.setStatus(AuctionStatus.RESERVE_NOT_MET);
            }
            log.info("[Listener - Live] Auction {} ended. Final Status: {}", auctionId, auction.getStatus());
            LiveAuction endedAuction = auctionRepository.save(auction);

            // Publish final state
            try {
                webSocketEventPublisher.publishAuctionStateUpdate(endedAuction, null);
                log.info("[Listener - Live] Published final state for ended auction {}", auctionId);
            } catch (Exception e) {
                log.error("[Listener - Live] Failed to publish end state for auction {}: {}", auctionId, e.getMessage(), e);
                // Allow transaction to commit even if publish fails? Or rethrow? Currently logs and continues.
            }
            // TODO: Publish specific notification event (AuctionEndedEvent) to NOTIFICATIONS_EXCHANGE
             publishAuctionEndedNotification(endedAuction);

        } else {
            log.warn("[Listener - Live] Auction {} not ACTIVE when end command received. Status: {}. Ignoring.", auctionId, auction.getStatus());
        }
    }

    // --- Listener for Cancel Command ---
    @RabbitListener(queues = RabbitMqConfig.AUCTION_CANCEL_QUEUE)
    @Transactional
    public void handleCancelCommand(CancelAuctionCommand command) {
        // Logic from AuctionLifecycleManager
        UUID auctionId = command.auctionId();
        log.info("[Listener - Live] Processing CancelAuctionCommand for auction: {}", auctionId);
        LiveAuction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) { log.warn("Cancel cmd for {}, but auction gone – ignoring", auctionId); return; }
        if (!auction.getSellerId().equals(command.sellerId())) { log.warn("Cancel cmd for {} ignored, seller mismatch.", auctionId); return; }

        if (auction.getStatus() == AuctionStatus.SCHEDULED || auction.getStatus() == AuctionStatus.ACTIVE) {
            auction.setStatus(AuctionStatus.CANCELLED);
            auction.setActualEndTime(LocalDateTime.now());
            LiveAuction saved = auctionRepository.save(auction);
            log.info("[Listener - Live] Auction {} CANCELLED by seller {}", auctionId, command.sellerId());
            try {
                webSocketEventPublisher.publishAuctionStateUpdate(saved, null);
                log.info("[Listener - Live] Published final state for cancelled auction {}", auctionId);
            } catch (Exception e) { log.error("[Listener - Live] Failed to publish cancel state for auction {}: {}", auctionId, e.getMessage(), e); }
            // TODO: Publish specific notification event (AuctionEndedEvent with CANCELLED status)
             publishAuctionEndedNotification(saved);
        } else {
            log.warn("[Listener - Live] Cancel cmd for {} ignored, status was {}.", auctionId, auction.getStatus());
        }
    }

    // --- Listener for Hammer Command ---
    @RabbitListener(queues = RabbitMqConfig.AUCTION_HAMMER_QUEUE)
    @Transactional
    public void handleHammerDownCommand(HammerDownCommand command) {
        // Logic from AuctionEventListener
        UUID auctionId = command.auctionId();
        log.info("[Listener - Live] Processing HammerDownCommand for auction: {}", auctionId);
        LiveAuction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) { log.warn("Hammer cmd for {}, but auction gone – ignoring", auctionId); return; }
        if (!auction.getSellerId().equals(command.sellerId())) { log.warn("Hammer cmd for {} ignored, seller mismatch.", auctionId); return; }
        if (auction.getStatus() != AuctionStatus.ACTIVE) { log.warn("Hammer cmd for {} ignored, status was {}.", auctionId, auction.getStatus()); return; }
        if (auction.getHighestBidderId() == null) { log.warn("Hammer cmd for {} ignored, no highest bidder.", auctionId); return; }

        // Set final state
        auction.setStatus(AuctionStatus.SOLD);
        auction.setWinnerId(auction.getHighestBidderId());
        auction.setWinningBid(auction.getCurrentBid());
        auction.setActualEndTime(LocalDateTime.now());
        LiveAuction saved = auctionRepository.save(auction);
        log.info("[Listener - Live] Auction {} HAMMERED by seller {}. Status: SOLD", auctionId, command.sellerId());

        try {
            webSocketEventPublisher.publishAuctionStateUpdate(saved, null);
            log.info("[Listener - Live] Published final state for hammered auction {}", auctionId);
        } catch (Exception e) { log.error("[Listener - Live] Failed to publish hammered state for auction {}: {}", auctionId, e.getMessage(), e); }

        // TODO: Publish specific notification event (AuctionEndedEvent with SOLD status)
         publishAuctionEndedNotification(saved);

        // Cancel any pending scheduled end - complicated, rely on listener status check for now.
        log.debug("Scheduled end message for auction {} will be ignored by listener due to status change.", auction.getId());
    }

    private void publishAuctionEndedNotification(LiveAuction auction) {
        if (auction == null) return; // Safety check

        // Build the event
        NotificationEvents.AuctionEndedEvent event = NotificationEvents.AuctionEndedEvent.builder()
                .auctionId(auction.getId())
                .productTitleSnapshot(auction.getProductTitleSnapshot()) // Ensure this is available
                .finalStatus(auction.getStatus())
                .actualEndTime(auction.getActualEndTime())
                .sellerId(auction.getSellerId())
                .winnerId(auction.getWinnerId()) // Will be null if not SOLD
                .winnerUsernameSnapshot(auction.getHighestBidderUsernameSnapshot()) // Use highest if available
                .winningBid(auction.getWinningBid()) // Will be null if not SOLD
                .build();
        try {
            // Publish to the notifications exchange with a LIVE-specific key
            String routingKey = RabbitMqConfig.AUCTION_STARTED_ROUTING_KEY_PREFIX + "live.ended";
            rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
            log.info("[Listener - Live] Published AuctionEndedEvent (Status: {}) for auction {} to exchange {}",
                    event.getFinalStatus(), event.getAuctionId(), RabbitMqConfig.NOTIFICATIONS_EXCHANGE);
        } catch (Exception e) {
            log.error("[Listener - Live] Failed to publish AuctionEndedEvent for live auction {}: {}", auction.getId(), e.getMessage(), e);
            // Consider retry or dead-lettering for critical notifications
        }
    }
}