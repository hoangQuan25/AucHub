package com.example.products.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    // --- Exchanges ---
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";
    // This is the exchange where `liveauctions` publishes its `...ended` events
    public static final String AUCTION_EVENTS_EXCHANGE = "notifications_exchange";
    // This is the new exchange for locking a product
    public static final String PRODUCT_LIFECYCLE_EXCHANGE = "product_lifecycle_exchange";

    // --- Queues for Products Service ---
    public static final String PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE = "q.products.order_completed";
    public static final String PRODUCT_SERVICE_ORDER_CANCELLED_QUEUE = "q.products.order_cancelled";
    public static final String PRODUCT_SERVICE_AUCTION_ENDED_QUEUE = "q.products.auction_ended";
    public static final String PRODUCT_SERVICE_PRODUCT_LOCKED_QUEUE = "q.products.product_locked";

    // --- Routing Keys ---
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.event.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.event.cancelled";
    public static final String AUCTION_ENDED_ROUTING_KEY_PATTERN = "auction.*.ended";
    public static final String PRODUCT_LOCKED_ROUTING_KEY = "product.event.locked";

    // --- Dead Letter ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange";
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue";
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";


    // === Exchange Beans ===
    @Bean
    TopicExchange ordersEventsExchange() { return new TopicExchange(ORDERS_EVENTS_EXCHANGE); }

    @Bean
    TopicExchange auctionEventsExchange() { return new TopicExchange(AUCTION_EVENTS_EXCHANGE); }

    @Bean
    TopicExchange productLifecycleExchange() { return new TopicExchange(PRODUCT_LIFECYCLE_EXCHANGE); }

    @Bean
    public DirectExchange mainDlxExchange() { return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE).durable(true).build(); }


    // === Queue Beans ===
    private Queue buildDurableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean Queue productServiceOrderCompletedQueue() { return buildDurableQueue(PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE); }
    @Bean Queue productServiceOrderCancelledQueue() { return buildDurableQueue(PRODUCT_SERVICE_ORDER_CANCELLED_QUEUE); }
    @Bean Queue productServiceAuctionEndedQueue() { return buildDurableQueue(PRODUCT_SERVICE_AUCTION_ENDED_QUEUE); }
    @Bean Queue productServiceProductLockedQueue() { return buildDurableQueue(PRODUCT_SERVICE_PRODUCT_LOCKED_QUEUE); }
    @Bean public Queue mainDeadLetterQueue() { return QueueBuilder.durable(MAIN_DEAD_LETTER_QUEUE).build(); }


    // === Binding Beans ===
    @Bean
    Binding bindingOrderCompleted(Queue productServiceOrderCompletedQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(productServiceOrderCompletedQueue).to(ordersEventsExchange).with(ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    Binding bindingOrderCancelled(Queue productServiceOrderCancelledQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(productServiceOrderCancelledQueue).to(ordersEventsExchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Binding bindingAuctionEnded(Queue productServiceAuctionEndedQueue, TopicExchange auctionEventsExchange) {
        return BindingBuilder.bind(productServiceAuctionEndedQueue).to(auctionEventsExchange).with(AUCTION_ENDED_ROUTING_KEY_PATTERN);
    }

    @Bean
    Binding bindingProductLocked(Queue productServiceProductLockedQueue, TopicExchange productLifecycleExchange) {
        return BindingBuilder.bind(productServiceProductLockedQueue).to(productLifecycleExchange).with(PRODUCT_LOCKED_ROUTING_KEY);
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue).to(mainDlxExchange).with(MAIN_DLQ_ROUTING_KEY);
    }


    // === Converter and Template Beans (Unchanged) ===
    @Bean public MessageConverter jsonMessageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
