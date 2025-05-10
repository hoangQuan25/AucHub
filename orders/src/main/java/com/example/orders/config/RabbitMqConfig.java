package com.example.orders.config; // Adjust package as needed

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

    // === Exchanges ===
    // For scheduling messages internal to Orders service (e.g., payment timeouts)
    public static final String ORDERS_SCHEDULE_EXCHANGE = "orders_schedule_exchange";
    // For publishing events from Orders service to other services
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";
    public static final String USER_EVENTS_EXCHANGE = "user_events_exchange";
    public static final String NOTIFICATIONS_EXCHANGE_NAME = "notifications_exchange";
    public static final String PAYMENTS_EVENTS_EXCHANGE_NAME = "payments_events_exchange";


    // === Queues ===
    // Queue for receiving payment timeout check commands after a delay
    public static final String ORDER_PAYMENT_TIMEOUT_QUEUE = "order_payment_timeout_queue";
    public static final String ORDERS_AUCTION_ENDED_QUEUE = "orders_auction_ended_queue";
    public static final String ORDERS_PAYMENT_SUCCEEDED_QUEUE = "orders_payment_succeeded_queue";
    public static final String ORDERS_PAYMENT_FAILED_QUEUE = "orders_payment_failed_queue";

    // === Routing Keys ===
    // For commands/messages to the schedule exchange
    public static final String ORDER_PAYMENT_TIMEOUT_SCHEDULE_ROUTING_KEY = "order.schedule.check-payment-timeout";

    // For events published from the Orders service
    public static final String ORDER_EVENT_CREATED_ROUTING_KEY = "order.event.created";
    public static final String ORDER_EVENT_PAYMENT_DUE_ROUTING_KEY = "order.event.payment.due";
    public static final String ORDER_EVENT_AUCTION_REOPEN_REQUESTED_ROUTING_KEY = "order.event.auction.reopen.requested";
    public static final String ORDER_EVENT_PAYMENT_CONFIRMED_ROUTING_KEY = "order.event.payment.confirmed";
    public static final String ORDER_EVENT_SELLER_DECISION_REQUIRED_ROUTING_KEY = "order.event.seller.decision.required";
    public static final String ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY = "order.event.ready-for-shipping";
    public static final String ORDER_EVENT_CANCELLED_ROUTING_KEY = "order.event.cancelled";
    public static final String USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY = "user.event.payment.defaulted";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.event.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.event.failed";
    public static final String AUCTION_ENDED_ROUTING_KEY_PATTERN = "auction.*.ended";

    // === Exchanges ===

    @Bean
    CustomExchange ordersScheduleExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct"); // Route based on routing key after delay
        return new CustomExchange(
                ORDERS_SCHEDULE_EXCHANGE,
                "x-delayed-message", // Plugin type
                true,  // durable
                false, // autoDelete
                args);
    }

    @Bean
    TopicExchange ordersEventsExchange() {
        return new TopicExchange(
                ORDERS_EVENTS_EXCHANGE,
                true,  // durable
                false  // autoDelete
        );
    }

    @Bean
    TopicExchange notificationsExchange() {
        // Declaring it here ensures it exists if Orders service starts first,
        // or simply allows us to refer to it for binding.
        // It should match the definition in your auction services.
        return new TopicExchange(NOTIFICATIONS_EXCHANGE_NAME, true, false);
    }

    @Bean
    TopicExchange paymentsEventsExchange() {
        // Declare the exchange the Payment Service publishes to.
        // Ensures the exchange exists from the perspective of this service for binding.
        return ExchangeBuilder.topicExchange(PAYMENTS_EVENTS_EXCHANGE_NAME)
                .durable(true)
                .build();
    }


    // === Queues ===

    @Bean
    Queue orderPaymentTimeoutQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_TIMEOUT_QUEUE)
                // Optional: Configure Dead Letter Exchange (DLX) for this queue
                // .withArgument("x-dead-letter-exchange", "your_dlx_exchange_name")
                // .withArgument("x-dead-letter-routing-key", "dlx.order_payment_timeout")
                .build();
    }

    @Bean
    Queue ordersAuctionEndedQueue() {
        return QueueBuilder.durable(ORDERS_AUCTION_ENDED_QUEUE)
                // Optional: Configure DLX for this queue
                .build();
    }

    @Bean
    Queue ordersPaymentSucceededQueue() {
        return QueueBuilder.durable(ORDERS_PAYMENT_SUCCEEDED_QUEUE).build();
    }

    @Bean
    Queue ordersPaymentFailedQueue() {
        return QueueBuilder.durable(ORDERS_PAYMENT_FAILED_QUEUE).build();
    }

    // === Bindings ===

    @Bean
    Binding paymentTimeoutBinding(Queue orderPaymentTimeoutQueue, CustomExchange ordersScheduleExchange) {
        return BindingBuilder
                .bind(orderPaymentTimeoutQueue)
                .to(ordersScheduleExchange)
                .with(ORDER_PAYMENT_TIMEOUT_SCHEDULE_ROUTING_KEY)
                .noargs();
    }

    @Bean
    Binding ordersAuctionEndedBinding(Queue ordersAuctionEndedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder
                .bind(ordersAuctionEndedQueue)
                .to(notificationsExchange) // Bind to the common notifications exchange
                .with(AUCTION_ENDED_ROUTING_KEY_PATTERN); // Listen for relevant auction ended events
    }

    @Bean
    Binding ordersPaymentSucceededBinding(Queue ordersPaymentSucceededQueue, TopicExchange paymentsEventsExchange) {
        return BindingBuilder
                .bind(ordersPaymentSucceededQueue)
                .to(paymentsEventsExchange)
                .with(PAYMENT_SUCCEEDED_ROUTING_KEY); // Listen for specific success event
    }

    @Bean
    Binding ordersPaymentFailedBinding(Queue ordersPaymentFailedQueue, TopicExchange paymentsEventsExchange) {
        return BindingBuilder
                .bind(ordersPaymentFailedQueue)
                .to(paymentsEventsExchange)
                .with(PAYMENT_FAILED_ROUTING_KEY); // Listen for specific failure event
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Uses Jackson library (usually included with spring-boot-starter-web)
        // to convert objects to/from JSON.
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // Set the message converter to use JSON
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}