package com.example.payments.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // Exchange where OrdersService publishes RefundRequestedEvent
    // (Assuming OrdersService publishes to its own ORDERS_EVENTS_EXCHANGE)
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";

    // Exchange THIS Payment Service publishes its events to
    public static final String PAYMENTS_EVENTS_EXCHANGE = "payments_events_exchange";

    // --- Queues Consumed by THIS Payments Service ---
    public static final String REFUND_REQUESTED_QUEUE = "q.payment.refund.requested";

    // --- Routing Keys Consumed by THIS Payments Service ---
    // This must match the routing key used by OrdersService when publishing RefundRequestedEvent
    public static final String REFUND_REQUESTED_ROUTING_KEY = "payment.event.refund.requested";


    // --- Routing Keys Published BY THIS Payments Service (on PAYMENTS_EVENTS_EXCHANGE) ---
    public static final String PAYMENT_EVENT_SUCCEEDED_ROUTING_KEY = "payment.event.succeeded"; // Existing
    public static final String PAYMENT_EVENT_FAILED_ROUTING_KEY = "payment.event.failed";       // Existing
    public static final String PAYMENT_EVENT_REFUND_SUCCEEDED_ROUTING_KEY = "payment.event.refund.succeeded"; // New
    public static final String PAYMENT_EVENT_REFUND_FAILED_ROUTING_KEY = "payment.event.refund.failed";       // New

    // --- Exchange Beans ---
    @Bean
    public TopicExchange paymentsEventsExchange() { // For events published BY this service
        return ExchangeBuilder.topicExchange(PAYMENTS_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange ordersEventsExchange() { // For events consumed BY this service (like refund requests)
        // This service needs to know about the exchange to bind a queue to it.
        // It assumes OrdersService defines and publishes to an exchange with this name.
        return ExchangeBuilder.topicExchange(ORDERS_EVENTS_EXCHANGE)
                .durable(true) // Ensure properties match the publisher's definition
                .build();
    }

    // --- Queue Beans ---
    @Bean
    public Queue refundRequestedQueue() {
        return QueueBuilder.durable(REFUND_REQUESTED_QUEUE).build();
    }

    // --- Binding Beans ---
    @Bean
    public Binding refundRequestedBinding(Queue refundRequestedQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(refundRequestedQueue)
                .to(ordersEventsExchange) // Listen on the exchange OrdersService publishes to
                .with(REFUND_REQUESTED_ROUTING_KEY);
    }

    // --- Message Converter & RabbitTemplate (should be same as before) ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory, final MessageConverter messageConverter) { // Ensure converter is injected
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}