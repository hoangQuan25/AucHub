package com.example.notifications.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // --- Exchange Name (Must match the publisher's exchange) ---
    public static final String NOTIFICATIONS_EXCHANGE = "notifications_exchange";

    // --- Queue Names for this Listener Service ---
    public static final String AUCTION_STARTED_QUEUE = "q.notification.auction.started";
    public static final String AUCTION_ENDED_QUEUE = "q.notification.auction.ended";
    public static final String AUCTION_OUTBID_QUEUE = "q.notification.auction.outbid";
    public static final String COMMENT_REPLIED_QUEUE = "q.notification.comment.replied";

    // --- Routing Keys to Bind (Must match the publisher's keys) ---
    public static final String AUCTION_STARTED_ROUTING_KEY = "auction.*.started";
    public static final String AUCTION_ENDED_ROUTING_KEY = "auction.*.ended"; // Listen to both live and timed
    public static final String AUCTION_OUTBID_ROUTING_KEY = "auction.*.outbid"; // Listen to both for now? Or specific? Let's use timed only for now: "auction.timed.outbid"
    //public static final String AUCTION_OUTBID_ROUTING_KEY = "auction.timed.outbid"; // Timed only
    public static final String AUCTION_OUTBID_ROUTING_KEY_WILDCARD = "auction.*.outbid"; // Listen to both live/timed

    public static final String COMMENT_REPLIED_ROUTING_KEY = "comment.*.replied"; // Listen to both live/timed comment replies if live adds it later

    // --- Exchange Bean ---
    @Bean
    TopicExchange notificationsExchange() {
        // Declare the exchange - idempotent operation (won't hurt if publisher also declares it)
        return new TopicExchange(NOTIFICATIONS_EXCHANGE);
    }

    @Bean
    Queue auctionStartedQueue() {
        return QueueBuilder.durable(AUCTION_STARTED_QUEUE)
                .build();
    }

    // --- Queue Beans ---
    @Bean
    Queue auctionEndedQueue() {
        // Durable queue to process ended auction notifications
        return QueueBuilder.durable(AUCTION_ENDED_QUEUE).build();
    }

    @Bean
    Queue auctionOutbidQueue() {
        // Durable queue to process outbid notifications
        return QueueBuilder.durable(AUCTION_OUTBID_QUEUE).build();
    }

    @Bean
    Queue commentRepliedQueue() {
        // Durable queue to process comment reply notifications
        return QueueBuilder.durable(COMMENT_REPLIED_QUEUE).build();
    }

    // --- Binding Beans ---

    @Bean
    Binding startedBinding(Queue auctionStartedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(auctionStartedQueue)
                .to(notificationsExchange)
                .with(AUCTION_STARTED_ROUTING_KEY); // Match *.started keys
    }

    @Bean
    Binding endedBinding(Queue auctionEndedQueue, TopicExchange notificationsExchange) {
        // Bind the queue to the exchange with the routing key pattern
        return BindingBuilder.bind(auctionEndedQueue)
                .to(notificationsExchange)
                .with(AUCTION_ENDED_ROUTING_KEY); // Match *.ended keys
    }

    @Bean
    Binding outbidBinding(Queue auctionOutbidQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(auctionOutbidQueue)
                .to(notificationsExchange)
                .with(AUCTION_OUTBID_ROUTING_KEY_WILDCARD); // Match *.outbid keys
    }

    @Bean
    Binding repliedBinding(Queue commentRepliedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(commentRepliedQueue)
                .to(notificationsExchange)
                .with(COMMENT_REPLIED_ROUTING_KEY); // Match *.replied keys
    }

    // --- Message Converter (Essential for DTO conversion) ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

     @Bean
     public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
         final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
         rabbitTemplate.setMessageConverter(messageConverter);
         return rabbitTemplate;
     }
}