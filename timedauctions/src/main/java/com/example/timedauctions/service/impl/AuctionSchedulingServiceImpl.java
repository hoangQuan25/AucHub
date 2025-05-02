// src/main/java/com/example/timedauctions/service/impl/AuctionSchedulingServiceImpl.java
package com.example.timedauctions.service.impl;

import com.example.timedauctions.commands.AuctionLifecycleCommands;
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.service.AuctionSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSchedulingServiceImpl implements AuctionSchedulingService {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void scheduleAuctionStart(TimedAuction auction) {
        long delayMillis = Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis();
        if (delayMillis > 0) {
            log.info("[Scheduler] Scheduling start for auction {} in {} ms", auction.getId(), delayMillis);
            AuctionLifecycleCommands.StartAuctionCommand command = new AuctionLifecycleCommands.StartAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.TD_AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.TD_START_ROUTING_KEY,
                    command,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMillis, Integer.MAX_VALUE));
                        return message;
                    }
            );
        } else {
            log.warn("[Scheduler] Attempted to schedule start for auction {} but delay was not positive.", auction.getId());
        }
    }

    @Override
    public void scheduleAuctionEnd(TimedAuction auction) {
        // Ensure endTime is not null before scheduling
        if (auction.getEndTime() == null) {
            log.error("[Scheduler] Cannot schedule end for auction {} because endTime is null.", auction.getId());
            return;
        }

        long delayMillis = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delayMillis > 0) {
            log.info("[Scheduler] Scheduling end for auction {} in {} ms", auction.getId(), delayMillis);
            AuctionLifecycleCommands.EndAuctionCommand command = new AuctionLifecycleCommands.EndAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.TD_AUCTION_SCHEDULE_EXCHANGE,
                    RabbitMqConfig.TD_END_ROUTING_KEY,
                    command,
                    message -> {
                        int delayHeaderValue = (int) Math.min(delayMillis, Integer.MAX_VALUE);
                        if (delayMillis > Integer.MAX_VALUE) {
                            log.warn("[Scheduler] Auction {} end delay ({}) exceeds max RabbitMQ delay. Clamping.", auction.getId(), delayMillis);
                        }
                        message.getMessageProperties().setHeader("x-delay", delayHeaderValue);
                        return message;
                    }
            );
        } else {
            log.warn("[Scheduler] Attempted to schedule end for auction {} but delay ({}) was not positive. It might end immediately or has passed.", auction.getId(), delayMillis);
            // Maybe send an immediate (non-delayed) EndAuctionCommand if delay <=0 ?
            // Or let the listener handle the state if it's already past due.
            // Sending immediate command if already past end time:
            if (delayMillis <= 0) {
                log.info("[Scheduler] End time for auction {} is past. Sending immediate EndAuctionCommand.", auction.getId());
                AuctionLifecycleCommands.EndAuctionCommand command = new AuctionLifecycleCommands.EndAuctionCommand(auction.getId());
                // Send to the command queue directly or let the schedule exchange handle immediate routing if delay=0?
                // Sending directly to command queue is clearer:
                rabbitTemplate.convertAndSend(RabbitMqConfig.TD_AUCTION_COMMAND_EXCHANGE, RabbitMqConfig.TD_END_ROUTING_KEY, command);
                // NOTE: This requires a binding from TD_AUCTION_COMMAND_EXCHANGE/TD_END_ROUTING_KEY to TD_AUCTION_END_QUEUE in RabbitMqConfig
            }
        }
    }
}