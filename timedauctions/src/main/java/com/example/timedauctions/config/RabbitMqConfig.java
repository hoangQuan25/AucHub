package com.example.timedauctions.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    // Using different names to avoid clashes if running both services against same RabbitMQ
    public static final String TD_AUCTION_SCHEDULE_EXCHANGE = "td_auction_schedule_exchange";
    public static final String TD_AUCTION_COMMAND_EXCHANGE = "td_auction_command_exchange";
    // Events exchange might be shared or separate
    public static final String TD_AUCTION_EVENTS_EXCHANGE = "td_auction_events_exchange";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications_exchange";

    public static final String TD_AUCTION_START_QUEUE = "td_auction_start_queue";
    // dk exec rabbitmq rabbitmqctl purge_queue td_auction_start_queue
    public static final String TD_AUCTION_END_QUEUE = "td_auction_end_queue";
    public static final String TD_AUCTION_CANCEL_QUEUE  = "td_auction_cancel_queue";
    public static final String TD_AUCTION_HAMMER_QUEUE = "td_auction_hammer_queue";
    // No hammer queue needed? Or add if required:


    // Define specific routing keys for timed auctions
    public static final String TD_START_ROUTING_KEY = "td.auction.command.start";
    public static final String TD_END_ROUTING_KEY = "td.auction.command.end";
    public static final String TD_CANCEL_ROUTING_KEY = "td.auction.command.cancel";
    public static final String TD_HAMMER_ROUTING_KEY = "td.auction.command.hammer";

    // For notifications
    public static final String AUCTION_ENDED_ROUTING_KEY = "auction.timed.ended";
    public static final String AUCTION_OUTBID_ROUTING_KEY = "auction.timed.outbid";
    public static final String COMMENT_REPLIED_ROUTING_KEY = "comment.timed.replied";
    public static final String AUCTION_TIMED_REOPENED_ORDER_CREATED_ROUTING_KEY = "auction.timed.reopened_order.created";

    public static final String AUCTION_ENDED_ROUTING_KEY_PREFIX = "auction.";
    public static final String AUCTION_STARTED_ROUTING_KEY_PREFIX = "auction.";

    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";


    // --- Exchanges ---
    @Bean
    CustomExchange tdAuctionScheduleExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(TD_AUCTION_SCHEDULE_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    DirectExchange tdAuctionCommandExchange() {
        return new DirectExchange(TD_AUCTION_COMMAND_EXCHANGE);
    }

    @Bean
    TopicExchange tdAuctionEventsExchange() {
        // If publishing internal events
        return new TopicExchange(TD_AUCTION_EVENTS_EXCHANGE);
    }

    @Bean
    TopicExchange notificationsExchange() {
        // Declare the topic exchange used for publishing notification events
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }


    // --- Queues ---
    @Bean
    Queue tdAuctionStartQueue() {
        return QueueBuilder.durable(TD_AUCTION_START_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue tdAuctionEndQueue() {
        return QueueBuilder.durable(TD_AUCTION_END_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue tdAuctionCancelQueue() {
        return QueueBuilder.durable(TD_AUCTION_CANCEL_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue tdAuctionHammerQueue() { // For ending early
        return QueueBuilder.durable(TD_AUCTION_HAMMER_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue mainDeadLetterQueue() {
        return QueueBuilder.durable(MAIN_DEAD_LETTER_QUEUE)
                .build();
    }

    // --- Bindings ---
    @Bean
    Binding tdStartBinding(Queue tdAuctionStartQueue, CustomExchange tdAuctionScheduleExchange) {
        return BindingBuilder.bind(tdAuctionStartQueue)
                .to(tdAuctionScheduleExchange)
                .with(TD_START_ROUTING_KEY)
                .noargs();
    }

    @Bean
    Binding tdEndBinding(Queue tdAuctionEndQueue, CustomExchange tdAuctionScheduleExchange) {
        return BindingBuilder.bind(tdAuctionEndQueue)
                .to(tdAuctionScheduleExchange)
                .with(TD_END_ROUTING_KEY)
                .noargs();
    }

    @Bean
    Binding tdCancelBinding(Queue tdAuctionCancelQueue, DirectExchange tdAuctionCommandExchange) {
        return BindingBuilder.bind(tdAuctionCancelQueue)
                .to(tdAuctionCommandExchange)
                .with(TD_CANCEL_ROUTING_KEY);
    }

    @Bean
    Binding tdHammerBinding(Queue tdAuctionHammerQueue, DirectExchange tdAuctionCommandExchange) { // For ending early
        return BindingBuilder.bind(tdAuctionHammerQueue)
                .to(tdAuctionCommandExchange)
                .with(TD_HAMMER_ROUTING_KEY);
    }

    // Bind end queue to SCHEDULE exchange (for delayed messages)
    @Bean
    Binding tdEndBindingDelayed(Queue tdAuctionEndQueue, CustomExchange tdAuctionScheduleExchange) {
        return BindingBuilder.bind(tdAuctionEndQueue)
                .to(tdAuctionScheduleExchange)
                .with(TD_END_ROUTING_KEY)
                .noargs();
    }

    // Bind end queue to COMMAND exchange (for immediate messages if scheduled time is past)
    @Bean
    Binding tdEndBindingImmediate(Queue tdAuctionEndQueue, DirectExchange tdAuctionCommandExchange) {
        return BindingBuilder.bind(tdAuctionEndQueue)
                .to(tdAuctionCommandExchange)
                .with(TD_END_ROUTING_KEY); // Reuse same routing key
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter); // Use the bean
        return rabbitTemplate;
    }
}