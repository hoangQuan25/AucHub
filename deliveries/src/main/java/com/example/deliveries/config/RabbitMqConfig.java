package com.example.deliveries.config;

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

    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";
    public static final String DELIVERIES_EVENTS_EXCHANGE = "deliveries_events_exchange";
    public static final String DELIVERIES_SCHEDULE_EXCHANGE = "deliveries_schedule_exchange";

    // --- Queues Consumed by THIS Deliveries Service ---
    public static final String ORDER_READY_FOR_SHIPPING_DELIVERY_QUEUE = "q.delivery.order.readyforshipping";
    public static final String DELIVERY_AUTO_COMPLETE_COMMAND_QUEUE = "q.delivery.command.auto_complete";

    // --- Routing Keys Consumed by THIS Deliveries Service ---
    // Must match the routing key used by OrdersService for OrderReadyForShippingEvent
    public static final String ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY = "order.event.ready-for-shipping";


    // --- Routing Keys Published BY THIS Deliveries Service (on DELIVERIES_EVENTS_EXCHANGE) ---
    public static final String DELIVERY_EVENT_CREATED_ROUTING_KEY = "delivery.event.created";
    public static final String DELIVERY_EVENT_SHIPPED_ROUTING_KEY = "delivery.event.shipped";
    public static final String DELIVERY_EVENT_DELIVERED_ROUTING_KEY = "delivery.event.delivered";
    public static final String DELIVERY_EVENT_ISSUE_REPORTED_ROUTING_KEY = "delivery.event.issue.reported";
    public static final String DELIVERY_EVENT_AWAITING_BUYER_CONFIRMATION_ROUTING_KEY = "delivery.event.awaiting.buyer.confirmation";
    public static final String DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY = "delivery.event.receipt.confirmed";
    public static final String DELIVERY_EVENT_RETURN_REQUESTED_ROUTING_KEY = "delivery.event.return.requested";
    public static final String DELIVERY_AUTO_COMPLETE_SCHEDULE_ROUTING_KEY = "delivery.schedule.auto-complete";
    public static final String DELIVERY_EVENT_AUTO_COMPLETED_ROUTING_KEY = "delivery.event.auto.completed"; // For future auto-completion

    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";


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

    @Bean
    CustomExchange deliveriesScheduleExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct"); // Can be direct or topic based on routing needs
        return new CustomExchange(
                DELIVERIES_SCHEDULE_EXCHANGE,
                "x-delayed-message", // Plugin type
                true,  // durable
                false, // autoDelete
                args);
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    // --- Queue Beans ---
    @Bean
    public Queue orderReadyForShippingDeliveryQueue() {
        return QueueBuilder.durable(ORDER_READY_FOR_SHIPPING_DELIVERY_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deliveryAutoCompleteCommandQueue() {
        return QueueBuilder.durable(DELIVERY_AUTO_COMPLETE_COMMAND_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue mainDeadLetterQueue() {
        return QueueBuilder.durable(MAIN_DEAD_LETTER_QUEUE)
                .build();
    }


    // --- Binding Beans ---
    @Bean
    public Binding orderReadyForShippingDeliveryBinding(Queue orderReadyForShippingDeliveryQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderReadyForShippingDeliveryQueue)
                .to(ordersEventsExchange) // Listen on the exchange OrdersService publishes to
                .with(ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY);
    }

    @Bean
    Binding deliveryAutoCompleteScheduleBinding(Queue deliveryAutoCompleteCommandQueue, CustomExchange deliveriesScheduleExchange) {
        return BindingBuilder
                .bind(deliveryAutoCompleteCommandQueue)
                .to(deliveriesScheduleExchange)
                .with(DELIVERY_AUTO_COMPLETE_SCHEDULE_ROUTING_KEY) // Routing key used for scheduling
                .noargs();
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
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