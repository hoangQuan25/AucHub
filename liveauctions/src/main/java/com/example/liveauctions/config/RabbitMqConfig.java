package com.example.liveauctions.config; // Or your config package

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String AUCTION_SCHEDULE_EXCHANGE = "auction_schedule_exchange";
    public static final String AUCTION_START_QUEUE = "auction_start_queue";
    public static final String AUCTION_END_QUEUE = "auction_end_queue";
    public static final String START_ROUTING_KEY = "auction.command.start";
    public static final String END_ROUTING_KEY = "auction.command.end";

    // The main exchange for real-time auction state updates (used later by WebSocket broadcaster)
    public static final String AUCTION_EVENTS_EXCHANGE = "auction_events_exchange";
    public static final String UPDATE_ROUTING_KEY_PREFIX = "auction.update."; // e.g., auction.update.uuid

    @Bean
    TopicExchange auctionEventsExchange() {
        return new TopicExchange(AUCTION_EVENTS_EXCHANGE);
    }


    // --- Configuration for Delayed Scheduling ---

    @Bean
    CustomExchange auctionScheduleExchange() {
        // Declare a custom exchange of type 'x-delayed-message'
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct"); // Route based on routing key after delay
        // Use ExchangeBuilder for cleaner syntax if preferred
        return new CustomExchange(AUCTION_SCHEDULE_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    Queue auctionStartQueue() {
        // Durable queue to receive start commands after delay
        return QueueBuilder.durable(AUCTION_START_QUEUE).build();
    }

    @Bean
    Queue auctionEndQueue() {
        // Durable queue to receive end commands after delay
        return QueueBuilder.durable(AUCTION_END_QUEUE).build();
    }

    @Bean
    Binding startBinding(Queue auctionStartQueue, CustomExchange auctionScheduleExchange) {
        return BindingBuilder.bind(auctionStartQueue)
                .to(auctionScheduleExchange)
                .with(START_ROUTING_KEY) // Route messages with this key after delay
                .noargs();
    }

    @Bean
    Binding endBinding(Queue auctionEndQueue, CustomExchange auctionScheduleExchange) {
        return BindingBuilder.bind(auctionEndQueue)
                .to(auctionScheduleExchange)
                .with(END_ROUTING_KEY) // Route messages with this key after delay
                .noargs();
    }

    // Note: The WebSocket listener queues bound to AUCTION_EVENTS_EXCHANGE
    // are typically declared dynamically/anonymously via @RabbitListener annotations
    // as shown previously, so no specific beans are usually needed here for those.
}