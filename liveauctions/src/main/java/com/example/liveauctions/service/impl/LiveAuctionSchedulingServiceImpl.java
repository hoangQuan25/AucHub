package com.example.liveauctions.service.impl;

import com.example.liveauctions.commands.AuctionLifecycleCommands; // Import commands
import com.example.liveauctions.config.RabbitMqConfig;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.service.LiveAuctionSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveAuctionSchedulingServiceImpl implements LiveAuctionSchedulingService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void scheduleAuctionStart(LiveAuction auction) {
        // Logic moved from LiveAuctionServiceImpl/previous manager attempts
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime() == null || !auction.getStartTime().isAfter(now)) {
            log.warn("[Scheduler - Live] Cannot schedule start for auction {} as start time is null or not in future.", auction.getId());
            return;
        }

        long delayMillis = Duration.between(now, auction.getStartTime()).toMillis();
        if (delayMillis > 0) {
            log.info("[Scheduler - Live] Scheduling start for auction {} in {} ms", auction.getId(), delayMillis);
            AuctionLifecycleCommands.StartAuctionCommand command = new AuctionLifecycleCommands.StartAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.START_ROUTING_KEY,
                    command,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMillis, Integer.MAX_VALUE));
                        return message;
                    }
            );
        } else {
            log.warn("[Scheduler - Live] Calculated start delay for auction {} was not positive ({}ms). Not scheduling.", auction.getId(), delayMillis);
        }
    }

    @Override
    public void scheduleAuctionEnd(LiveAuction auction) {
        LocalDateTime now = LocalDateTime.now();
        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) {
            log.warn("[Scheduler - Live] Cannot schedule end for auction {} as end time is null or not in future. Triggering immediate end check.", auction.getId());
            AuctionLifecycleCommands.EndAuctionCommand command = new AuctionLifecycleCommands.EndAuctionCommand(auction.getId(), auction.getEndTime()); // Include time for stale check
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.END_ROUTING_KEY,
                    command,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", 0); // Explicitly 0 delay
                        return message;
                    });
            return;
        }

        long delayMillis = Duration.between(now, auction.getEndTime()).toMillis();

        AuctionLifecycleCommands.EndAuctionCommand command = new AuctionLifecycleCommands.EndAuctionCommand(auction.getId(), auction.getEndTime());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                RabbitMqConfig.END_ROUTING_KEY,
                command,
                message -> {
                    int delayHeaderValue = (int) Math.min(delayMillis, Integer.MAX_VALUE);
                    message.getMessageProperties().setHeader("x-delay", delayHeaderValue);
                    log.debug("[Scheduler - Live] Setting x-delay header for END command routingKey {}: {}", RabbitMqConfig.END_ROUTING_KEY, delayHeaderValue);
                    return message;
                }
        );
        log.info("[Scheduler - Live] Scheduled auction end for auctionId: {} with effective delay: {} ms", auction.getId(), Math.max(0, delayMillis));
    }
}