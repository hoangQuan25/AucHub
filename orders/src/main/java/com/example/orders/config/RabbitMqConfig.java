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
    public static final String DELIVERIES_EVENTS_EXCHANGE = "deliveries_events_exchange";
    public static final String TIMED_AUCTIONS_EVENTS_EXCHANGE_NAME = "td_auction_events_exchange";
    public static final String LIVE_AUCTIONS_EVENTS_EXCHANGE_NAME = "auction_events_exchange";


    // === Queues ===
    // Queue for receiving payment timeout check commands after a delay
    public static final String ORDER_PAYMENT_TIMEOUT_QUEUE = "order_payment_timeout_queue";
    public static final String ORDERS_AUCTION_ENDED_QUEUE = "orders_auction_ended_queue";
    public static final String ORDERS_PAYMENT_SUCCEEDED_QUEUE = "orders_payment_succeeded_queue";
    public static final String ORDERS_PAYMENT_FAILED_QUEUE = "orders_payment_failed_queue";
    public static final String ORDERS_DELIVERY_RECEIPT_CONFIRMED_QUEUE = "orders_delivery_receipt_confirmed_queue";
    public static final String ORDERS_FINALIZE_REOPENED_TIMED_AUCTION_QUEUE = "orders_finalize_reopened_timed_auction_queue";
    public static final String ORDERS_FINALIZE_REOPENED_LIVE_AUCTION_QUEUE = "orders_finalize_reopened_live_auction_queue";
    // === Routing Keys ===
    // For commands/messages to the schedule exchange
    public static final String ORDER_PAYMENT_TIMEOUT_SCHEDULE_ROUTING_KEY = "order.schedule.check-payment-timeout";

    // For events published from the Orders service
    public static final String ORDER_EVENT_CREATED_ROUTING_KEY = "order.event.created";
    public static final String ORDER_EVENT_PAYMENT_DUE_ROUTING_KEY = "order.event.payment.due";
    public static final String ORDER_EVENT_AUCTION_REOPEN_REQUESTED_ROUTING_KEY = "order.event.auction.reopen.requested";
    public static final String ORDER_EVENT_PAYMENT_CONFIRMED_ROUTING_KEY = "order.event.payment.confirmed";
    public static final String ORDER_EVENT_SELLER_DECISION_REQUIRED_ROUTING_KEY = "order.event.seller.decision.required";
    public static final String ORDER_EVENT_AWAITING_FULFILLMENT_CONFIRMATION_ROUTING_KEY = "order.event.awaiting.fulfillment.confirmation";
    public static final String PAYMENT_EVENT_REFUND_REQUESTED_ROUTING_KEY = "payment.event.refund.requested";
    public static final String ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY = "order.event.ready-for-shipping";
    public static final String ORDER_EVENT_CANCELLED_ROUTING_KEY = "order.event.cancelled";
    public static final String USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY = "user.event.payment.defaulted";
    public static final String PAYMENT_SUCCEEDED_ROUTING_KEY = "payment.event.succeeded";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.event.failed";
    public static final String AUCTION_ENDED_ROUTING_KEY_PATTERN = "auction.*.ended";
    public static final String DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY = "delivery.event.receipt.confirmed";
    public static final String TIMED_AUCTION_REOPENED_ORDER_CREATED_ROUTING_KEY = "auction.timed.reopened_order.created";
    public static final String LIVE_AUCTION_REOPENED_ORDER_CREATED_ROUTING_KEY = "auction.live.reopened_order.created";
    public static final String ORDER_EVENT_COMPLETED_ROUTING_KEY = "order.event.completed";

    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";


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
    TopicExchange userEventsExchange() {
        // Declare the exchange for user-related events
        return new TopicExchange(
                USER_EVENTS_EXCHANGE,
                true,  // durable
                false  // autoDelete
        );
    }

    @Bean
    TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE_NAME, true, false);
    }

    @Bean
    TopicExchange paymentsEventsExchange() {
        return ExchangeBuilder.topicExchange(PAYMENTS_EVENTS_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    TopicExchange deliveriesEventsExchange() {
        // Durable, non-auto-delete. Assumes Deliveries service also declares it this way.
        return new TopicExchange(DELIVERIES_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange timedAuctionsEventsExchange() {
        return new TopicExchange(TIMED_AUCTIONS_EVENTS_EXCHANGE_NAME, true, false);
    }

    @Bean
    TopicExchange liveAuctionsEventsExchange() {
        return new TopicExchange(LIVE_AUCTIONS_EVENTS_EXCHANGE_NAME, true, false);
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }



    // === Queues ===

    @Bean
    Queue orderPaymentTimeoutQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersAuctionEndedQueue() {
        return QueueBuilder.durable(ORDERS_AUCTION_ENDED_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersPaymentSucceededQueue() {
        return QueueBuilder.durable(ORDERS_PAYMENT_SUCCEEDED_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersPaymentFailedQueue() {
        return QueueBuilder.durable(ORDERS_PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersDeliveryReceiptConfirmedQueue() {
        return QueueBuilder.durable(ORDERS_DELIVERY_RECEIPT_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersFinalizeReopenedTimedAuctionQueue() {
        return QueueBuilder.durable(ORDERS_FINALIZE_REOPENED_TIMED_AUCTION_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue ordersFinalizeReopenedLiveAuctionQueue() {
        return QueueBuilder.durable(ORDERS_FINALIZE_REOPENED_LIVE_AUCTION_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue mainDeadLetterQueue() {
        return QueueBuilder.durable(MAIN_DEAD_LETTER_QUEUE)
                .build();
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
    Binding ordersDeliveryReceiptConfirmedBinding(Queue ordersDeliveryReceiptConfirmedQueue, TopicExchange deliveriesEventsExchange) {
        return BindingBuilder
                .bind(ordersDeliveryReceiptConfirmedQueue)
                .to(deliveriesEventsExchange)
                .with(DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    Binding ordersFinalizeReopenedTimedAuctionBinding(
            Queue ordersFinalizeReopenedTimedAuctionQueue,
            TopicExchange timedAuctionsEventsExchange) { // Use the correctly named exchange bean
        return BindingBuilder
                .bind(ordersFinalizeReopenedTimedAuctionQueue)
                .to(timedAuctionsEventsExchange)
                .with(TIMED_AUCTION_REOPENED_ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding ordersFinalizeReopenedLiveAuctionBinding(
            Queue ordersFinalizeReopenedLiveAuctionQueue,
            TopicExchange liveAuctionsEventsExchange) { // Use the correctly named exchange bean
        return BindingBuilder
                .bind(ordersFinalizeReopenedLiveAuctionQueue)
                .to(liveAuctionsEventsExchange)
                .with(LIVE_AUCTION_REOPENED_ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
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