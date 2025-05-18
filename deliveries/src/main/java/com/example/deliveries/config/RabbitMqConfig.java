package com.example.deliveries.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // Exchange from which OrdersService publishes OrderReadyForShippingEvent
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";

    // Exchange THIS Deliveries Service publishes its events to
    public static final String DELIVERIES_EVENTS_EXCHANGE = "deliveries_events_exchange";

    // --- Queues Consumed by THIS Deliveries Service ---
    public static final String ORDER_READY_FOR_SHIPPING_DELIVERY_QUEUE = "q.delivery.order.readyforshipping";

    // --- Routing Keys Consumed by THIS Deliveries Service ---
    // Must match the routing key used by OrdersService for OrderReadyForShippingEvent
    public static final String ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY = "order.event.ready-for-shipping";


    // --- Routing Keys Published BY THIS Deliveries Service (on DELIVERIES_EVENTS_EXCHANGE) ---
    public static final String DELIVERY_EVENT_CREATED_ROUTING_KEY = "delivery.event.created";
    public static final String DELIVERY_EVENT_SHIPPED_ROUTING_KEY = "delivery.event.shipped";
    public static final String DELIVERY_EVENT_DELIVERED_ROUTING_KEY = "delivery.event.delivered";
    public static final String DELIVERY_EVENT_ISSUE_REPORTED_ROUTING_KEY = "delivery.event.issue.reported";


    // --- Exchange Beans ---
    @Bean
    public TopicExchange deliveriesEventsExchange() { // For events published BY this service
        return ExchangeBuilder.topicExchange(DELIVERIES_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange ordersEventsExchange() { // For events consumed BY this service
        // Assumes OrdersService defines and publishes to an exchange with this name.
        return ExchangeBuilder.topicExchange(ORDERS_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    // --- Queue Beans ---
    @Bean
    public Queue orderReadyForShippingDeliveryQueue() {
        return QueueBuilder.durable(ORDER_READY_FOR_SHIPPING_DELIVERY_QUEUE)
                // Optional: Configure DLX for this queue if needed
                // .withArgument("x-dead-letter-exchange", "your_dlx_exchange_name")
                // .withArgument("x-dead-letter-routing-key", "dlx.delivery.order.readyforshipping")
                .build();
    }

    // --- Binding Beans ---
    @Bean
    public Binding orderReadyForShippingDeliveryBinding(Queue orderReadyForShippingDeliveryQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderReadyForShippingDeliveryQueue)
                .to(ordersEventsExchange) // Listen on the exchange OrdersService publishes to
                .with(ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY);
    }

    // --- Message Converter & RabbitTemplate ---
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