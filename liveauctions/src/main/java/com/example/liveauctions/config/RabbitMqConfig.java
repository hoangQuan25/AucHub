package com.example.liveauctions.config; // Or your config package

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

    public static final String AUCTION_SCHEDULE_EXCHANGE = "auction_schedule_exchange";
    public static final String AUCTION_EVENTS_EXCHANGE = "auction_events_exchange";
    public static final String AUCTION_COMMAND_EXCHANGE = "auction_command_exchange";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications_exchange";

    public static final String AUCTION_START_QUEUE = "auction_start_queue";
    public static final String AUCTION_END_QUEUE = "auction_end_queue";
    public static final String AUCTION_HAMMER_QUEUE     = "auction_hammer_queue";
    public static final String AUCTION_CANCEL_QUEUE  = "auction_cancel_queue";

    public static final String START_ROUTING_KEY = "auction.command.start";
    public static final String END_ROUTING_KEY = "auction.command.end";
    public static final String HAMMER_ROUTING_KEY = "auction.command.hammer";
    public static final String CANCEL_ROUTING_KEY = "auction.command.cancel";
    public static final String AUCTION_LIVE_ENDED_ROUTING_KEY = "auction.live.ended";
    public static final String AUCTION_LIVE_REOPENED_ORDER_CREATED_ROUTING_KEY = "auction.live.reopened_order.created";


    public static final String UPDATE_ROUTING_KEY_PREFIX = "auction.update."; // e.g., auction.update.uuid
    public static final String AUCTION_ROUTING_KEY_PREFIX = "auction.";

    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";

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
    DirectExchange auctionCommandExchange() {
        return new DirectExchange(AUCTION_COMMAND_EXCHANGE);
    }

    @Bean
    TopicExchange notificationsExchange() {
        // Declare the topic exchange for notifications (idempotent)
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue auctionStartQueue() {
        // Durable queue to receive start commands after delay
        return QueueBuilder.durable(AUCTION_START_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue auctionEndQueue() {
        // Durable queue to receive end commands after delay
        return QueueBuilder.durable(AUCTION_END_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue auctionHammerQueue() {
        return QueueBuilder.durable(AUCTION_HAMMER_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue auctionCancelQueue() {
        return QueueBuilder.durable(AUCTION_CANCEL_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue mainDeadLetterQueue() {
        return QueueBuilder.durable(MAIN_DEAD_LETTER_QUEUE)
                .build();
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

    @Bean
    Binding hammerBinding(Queue auctionHammerQueue, DirectExchange auctionCommandExchange) {
        return BindingBuilder.bind(auctionHammerQueue)
                .to(auctionCommandExchange)
                .with(HAMMER_ROUTING_KEY);
    }

    @Bean
    Binding cancelBinding(Queue auctionCancelQueue,
                                DirectExchange auctionCommandExchange) {
        return BindingBuilder.bind(auctionCancelQueue)
                .to(auctionCommandExchange)
                .with(CANCEL_ROUTING_KEY);
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
    }

    /**
     * Defines the Jackson JSON message converter to be used.
     * @return MessageConverter bean using Jackson JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // Uses Jackson library (usually included with spring-boot-starter-web)
        // to convert objects to/from JSON.
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates a customized RabbitTemplate that uses the JSON message converter.
     * Spring will inject this template where RabbitTemplate is autowired.
     * @param connectionFactory The RabbitMQ connection factory (auto-injected).
     * @return Customized RabbitTemplate bean.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // Set the message converter to use JSON
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
    // as shown previously, so no specific beans are usually needed here for those.
}