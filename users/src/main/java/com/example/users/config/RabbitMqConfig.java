// package com.example.users.config; // Or your config package for RabbitMQ in User service
package com.example.users.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig { // Renamed to avoid conflict if in same project

    // Exchange published by DeliveriesService
    public static final String DELIVERIES_EVENTS_EXCHANGE = "deliveries_events_exchange"; // Must match DeliveriesService

    // Queues Consumed by THIS User Service (for review eligibility)
    public static final String DELIVERY_RECEIPT_CONFIRMED_FOR_REVIEW_QUEUE = "q.user.delivery.receipt_confirmed";
    public static final String DELIVERY_AUTO_COMPLETED_FOR_REVIEW_QUEUE = "q.user.delivery.auto_completed";

    // Routing Keys published by DeliveriesService that we are interested in
    // These MUST match the ones used in DeliveriesService RabbitMqConfig
    public static final String DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY = "delivery.event.receipt.confirmed";
    public static final String DELIVERY_EVENT_AUTO_COMPLETED_ROUTING_KEY = "delivery.event.auto.completed";

    @Bean
    public TopicExchange deliveriesEventsExchangeConsumer() { // Bean to define the exchange we consume from
        return ExchangeBuilder.topicExchange(DELIVERIES_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deliveryReceiptConfirmedQueue() {
        return QueueBuilder.durable(DELIVERY_RECEIPT_CONFIRMED_FOR_REVIEW_QUEUE).build();
    }

    @Bean
    public Queue deliveryAutoCompletedQueue() {
        return QueueBuilder.durable(DELIVERY_AUTO_COMPLETED_FOR_REVIEW_QUEUE).build();
    }

    @Bean
    public Binding deliveryReceiptConfirmedBinding(Queue deliveryReceiptConfirmedQueue, TopicExchange deliveriesEventsExchangeConsumer) {
        return BindingBuilder.bind(deliveryReceiptConfirmedQueue)
                .to(deliveriesEventsExchangeConsumer)
                .with(DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryAutoCompletedBinding(Queue deliveryAutoCompletedQueue, TopicExchange deliveriesEventsExchangeConsumer) {
        return BindingBuilder.bind(deliveryAutoCompletedQueue)
                .to(deliveriesEventsExchangeConsumer)
                .with(DELIVERY_EVENT_AUTO_COMPLETED_ROUTING_KEY);
    }

    // Message converter (can be shared if RabbitTemplate is also in this service)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}