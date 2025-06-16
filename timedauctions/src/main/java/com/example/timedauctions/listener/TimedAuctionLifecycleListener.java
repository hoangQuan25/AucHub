package com.example.timedauctions.listener;

import com.example.timedauctions.client.UserServiceClient;
import com.example.timedauctions.client.dto.UserBasicInfoDto;
import com.example.timedauctions.commands.AuctionLifecycleCommands; // Ensure these command records exist
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.entity.Bid;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.event.NotificationEvents;
import com.example.timedauctions.exception.AuctionNotFoundException;
import com.example.timedauctions.repository.BidRepository;
import com.example.timedauctions.repository.TimedAuctionRepository;
import com.example.timedauctions.service.AuctionSchedulingService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimedAuctionLifecycleListener {

    private final TimedAuctionRepository timedAuctionRepository;
    private final AuctionSchedulingService auctionSchedulingService;
    private final UserServiceClient userServiceClient; // Assuming this is a client to fetch user details
    private final BidRepository bidRepository;
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
                    auction.setStatus(AuctionStatus.RESERVE_NOT_MET); // Assuming no bids = reserve not met
                    log.info("Auction {} ended. Status: RESERVE_NOT_MET (No bids placed).", auction.getId());
                }
                auction.setActualEndTime(LocalDateTime.now());
                endedAuction = timedAuctionRepository.save(auction);


            } else {
                log.warn("Received end command for auction {} but its end time {} is still in the future? Current time {}. Ignoring/Rescheduling?",
                        auction.getId(), auction.getEndTime(), LocalDateTime.now());
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

    /* ------------------------------------------------------------------
     *  Helper record – now includes username snapshot
     * ------------------------------------------------------------------*/
    @Getter
    @AllArgsConstructor
    private static class EligibleBidderInfo {
        private final String bidderId;
        private final String usernameSnapshot;
        private final BigDecimal maxBidAmount;
    }

    /* ------------------------------------------------------------------
     *  ENRICHED “auction ended” publisher – TIMED edition
     * ------------------------------------------------------------------*/
    private void publishAuctionEndedNotification(TimedAuction auction) {
        if (auction == null) return;

        final String auctionType = "TIMED";

        // ---------- Base fields ----------
        NotificationEvents.AuctionEndedEvent.AuctionEndedEventBuilder builder =
                NotificationEvents.AuctionEndedEvent.builder()
                        .eventId(UUID.randomUUID())
                        .eventTimestamp(LocalDateTime.now())
                        .auctionId(auction.getId())
                        .productId(auction.getProductId())
                        .productTitleSnapshot(auction.getProductTitleSnapshot())
                        .productImageUrlSnapshot(auction.getProductImageUrlSnapshot())
                        .auctionType(auctionType)
                        .sellerId(auction.getSellerId())
                        .sellerUsernameSnapshot(auction.getSellerUsernameSnapshot())
                        .finalStatus(auction.getStatus())
                        .actualEndTime(auction.getActualEndTime());

        // ---------- SOLD-only enrichment ----------
        if (auction.getStatus() == AuctionStatus.SOLD) {
            builder.winnerId(auction.getWinnerId())
                    .winnerUsernameSnapshot(auction.getHighestBidderUsernameSnapshot())
                    .winningBid(auction.getWinningBid())
                    .reservePrice(auction.getReservePrice());

            List<EligibleBidderInfo> next = findEligibleNextRawBidders(auction);
            if (!next.isEmpty()) {
                EligibleBidderInfo second = next.get(0);
                builder.secondHighestBidderId(second.getBidderId())
                        .secondHighestBidderUsernameSnapshot(second.getUsernameSnapshot())
                        .secondHighestBidAmount(second.getMaxBidAmount());

                if (next.size() > 1) {
                    EligibleBidderInfo third = next.get(1);
                    builder.thirdHighestBidderId(third.getBidderId())
                            .thirdHighestBidderUsernameSnapshot(third.getUsernameSnapshot())
                            .thirdHighestBidAmount(third.getMaxBidAmount());
                }
            }
        }

        // ---------- Publish ----------
        try {
            NotificationEvents.AuctionEndedEvent event = builder.build();
            String routingKey = RabbitMqConfig.AUCTION_ENDED_ROUTING_KEY_PREFIX + "timed.ended";
            rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
            log.info("[Listener - Timed] Published ENRICHED AuctionEndedEvent (Status: {}) for auction {}",
                    event.getFinalStatus(), event.getAuctionId());
        } catch (Exception e) {
            log.error("[Listener - Timed] Failed to publish ENRICHED AuctionEndedEvent for auction {}: {}",
                    auction.getId(), e.getMessage(), e);
        }
    }


    private List<EligibleBidderInfo> findEligibleNextRawBidders(TimedAuction auction) {
        if (auction.getWinnerId() == null || auction.getStatus() != AuctionStatus.SOLD) {
            return Collections.emptyList();
        }

        List<Bid> allBids = bidRepository
                .findByTimedAuctionId(auction.getId(), Pageable.unpaged())
                .getContent();
        if (allBids.isEmpty()) return Collections.emptyList();

        // Map <bidderId, EligibleBidderInfo(max bid & username)>
        Map<String, EligibleBidderInfo> maxByBidder = new HashMap<>();
        for (Bid b : allBids) {
            maxByBidder.compute(b.getBidderId(), (id, current) -> {
                if (current == null || b.getAmount().compareTo(current.getMaxBidAmount()) > 0) {
                    return new EligibleBidderInfo(id, b.getBidderUsernameSnapshot(), b.getAmount());
                }
                return current;
            });
        }
        maxByBidder.remove(auction.getWinnerId());

        // Filter by reserve (if any) and grab the top 2
        return maxByBidder.values().stream()
                .filter(info -> auction.getReservePrice() == null ||
                        info.getMaxBidAmount().compareTo(auction.getReservePrice()) >= 0)
                .sorted((a, b) -> b.getMaxBidAmount().compareTo(a.getMaxBidAmount()))
                .limit(2)
                .collect(Collectors.toList());
    }

}