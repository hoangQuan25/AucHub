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

    // --- Exchange Names ---
    public static final String NOTIFICATIONS_EXCHANGE = "notifications_exchange"; // Existing: For auction/comment events published TO this service
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange";     // New: For events published BY Orders Service
    public static final String USER_EVENTS_EXCHANGE = "user_events_exchange";         // New: For user-specific events published BY Orders Service (or other services)

    // --- Queue Names for Auction/Comment Notifications (Existing) ---
    public static final String AUCTION_STARTED_QUEUE = "q.notification.auction.started";
    public static final String AUCTION_ENDED_QUEUE = "q.notification.auction.ended";
    public static final String AUCTION_OUTBID_QUEUE = "q.notification.auction.outbid";
    public static final String COMMENT_REPLIED_QUEUE = "q.notification.comment.replied";

    // --- Routing Keys for Auction/Comment Notifications (Existing) ---
    public static final String AUCTION_STARTED_ROUTING_KEY = "auction.*.started";
    public static final String AUCTION_ENDED_ROUTING_KEY = "auction.*.ended";
    public static final String AUCTION_OUTBID_ROUTING_KEY_WILDCARD = "auction.*.outbid";
    public static final String COMMENT_REPLIED_ROUTING_KEY = "comment.*.replied";


    // --- Queue Names for Order & User Event Notifications (New) ---
    public static final String ORDER_CREATED_QUEUE = "q.notification.order.created";
    public static final String ORDER_PAYMENT_DUE_QUEUE = "q.notification.order.payment.due";
    public static final String ORDER_SELLER_DECISION_REQUIRED_QUEUE = "q.notification.order.seller.decision.required";
    public static final String ORDER_READY_FOR_SHIPPING_QUEUE = "q.notification.order.ready.for.shipping";
    public static final String ORDER_CANCELLED_QUEUE = "q.notification.order.cancelled";
    public static final String USER_PAYMENT_DEFAULTED_QUEUE = "q.notification.user.payment.defaulted"; // From USER_EVENTS_EXCHANGE

    // --- Routing Keys for Order & User Event Notifications (New - must match publisher in OrderService) ---
    public static final String ORDER_EVENT_CREATED_ROUTING_KEY = "order.event.created";
    public static final String ORDER_EVENT_PAYMENT_DUE_ROUTING_KEY = "order.event.payment.due";
    public static final String ORDER_EVENT_SELLER_DECISION_REQUIRED_ROUTING_KEY = "order.event.seller.decision.required";
    public static final String ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY = "order.event.ready-for-shipping";
    public static final String ORDER_EVENT_CANCELLED_ROUTING_KEY = "order.event.cancelled";
    public static final String USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY = "user.event.payment.defaulted";


    // --- Exchange Beans ---
    @Bean
    TopicExchange notificationsExchange() { // Existing
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange ordersEventsExchange() { // New: Declare it so we can bind to it
        // Durable, non-auto-delete. Assumes Orders service also declares it this way.
        return new TopicExchange(ORDERS_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange userEventsExchange() { // New: Declare it
        return new TopicExchange(USER_EVENTS_EXCHANGE, true, false);
    }

    // --- Queue Beans (Existing) ---
    @Bean
    Queue auctionStartedQueue() {
        return QueueBuilder.durable(AUCTION_STARTED_QUEUE).build();
    }

    @Bean
    Queue auctionEndedQueue() {
        return QueueBuilder.durable(AUCTION_ENDED_QUEUE).build();
    }

    @Bean
    Queue auctionOutbidQueue() {
        return QueueBuilder.durable(AUCTION_OUTBID_QUEUE).build();
    }

    @Bean
    Queue commentRepliedQueue() {
        return QueueBuilder.durable(COMMENT_REPLIED_QUEUE).build();
    }

    // --- Queue Beans (New for Order/User Events) ---
    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }

    @Bean
    Queue orderPaymentDueQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_DUE_QUEUE).build();
    }

    @Bean
    Queue sellerDecisionRequiredQueue() {
        return QueueBuilder.durable(ORDER_SELLER_DECISION_REQUIRED_QUEUE).build();
    }

    @Bean
    Queue orderReadyForShippingQueue() {
        return QueueBuilder.durable(ORDER_READY_FOR_SHIPPING_QUEUE).build();
    }

    @Bean
    Queue orderCancelledQueue() {
        return QueueBuilder.durable(ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    Queue userPaymentDefaultedQueue() {
        return QueueBuilder.durable(USER_PAYMENT_DEFAULTED_QUEUE).build();
    }


    // --- Binding Beans (Existing) ---
    @Bean
    Binding startedBinding(Queue auctionStartedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(auctionStartedQueue)
                .to(notificationsExchange)
                .with(AUCTION_STARTED_ROUTING_KEY);
    }

    @Bean
    Binding endedBinding(Queue auctionEndedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(auctionEndedQueue)
                .to(notificationsExchange)
                .with(AUCTION_ENDED_ROUTING_KEY);
    }

    @Bean
    Binding outbidBinding(Queue auctionOutbidQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(auctionOutbidQueue)
                .to(notificationsExchange)
                .with(AUCTION_OUTBID_ROUTING_KEY_WILDCARD);
    }

    @Bean
    Binding repliedBinding(Queue commentRepliedQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(commentRepliedQueue)
                .to(notificationsExchange)
                .with(COMMENT_REPLIED_ROUTING_KEY);
    }

    // --- Binding Beans (New for Order/User Events) ---

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue)
                .to(ordersEventsExchange)
                .with(ORDER_EVENT_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding orderPaymentDueBinding(Queue orderPaymentDueQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderPaymentDueQueue)
                .to(ordersEventsExchange)
                .with(ORDER_EVENT_PAYMENT_DUE_ROUTING_KEY);
    }

    @Bean
    Binding sellerDecisionRequiredBinding(Queue sellerDecisionRequiredQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(sellerDecisionRequiredQueue)
                .to(ordersEventsExchange)
                .with(ORDER_EVENT_SELLER_DECISION_REQUIRED_ROUTING_KEY);
    }

    @Bean
    Binding orderReadyForShippingBinding(Queue orderReadyForShippingQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderReadyForShippingQueue)
                .to(ordersEventsExchange)
                .with(ORDER_EVENT_READY_FOR_SHIPPING_ROUTING_KEY);
    }

    @Bean
    Binding orderCancelledBinding(Queue orderCancelledQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderCancelledQueue)
                .to(ordersEventsExchange)
                .with(ORDER_EVENT_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Binding userPaymentDefaultedBinding(Queue userPaymentDefaultedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userPaymentDefaultedQueue)
                .to(userEventsExchange) // Bind to the USER_EVENTS_EXCHANGE
                .with(USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY);
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