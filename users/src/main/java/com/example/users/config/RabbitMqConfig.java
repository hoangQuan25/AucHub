// package com.example.users.config; // Or your config package for RabbitMQ in User service
package com.example.users.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig { // Renamed to avoid conflict if in same project

    // Exchange published by DeliveriesService
    public static final String DELIVERIES_EVENTS_EXCHANGE = "deliveries_events_exchange"; // Must match DeliveriesService
    public static final String USER_EVENTS_EXCHANGE = "user_events_exchange";

    // Queues Consumed by THIS User Service (for review eligibility)
    public static final String DELIVERY_RECEIPT_CONFIRMED_FOR_REVIEW_QUEUE = "q.user.delivery.receipt_confirmed";
    public static final String DELIVERY_AUTO_COMPLETED_FOR_REVIEW_QUEUE = "q.user.delivery.auto_completed";
    public static final String USER_PAYMENT_DEFAULTED_QUEUE = "q.user.user_payment_defaulted";

    public static final String DELIVERY_EVENT_RECEIPT_CONFIRMED_ROUTING_KEY = "delivery.event.receipt.confirmed";
    public static final String DELIVERY_EVENT_AUTO_COMPLETED_ROUTING_KEY = "delivery.event.auto.completed";
    public static final String USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY = "user.event.payment.defaulted";
    public static final String USER_EVENT_BANNED_ROUTING_KEY = "user.event.banned";


    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";

    @Bean
    public TopicExchange deliveriesEventsExchangeConsumer() { // Bean to define the exchange we consume from
        return ExchangeBuilder.topicExchange(DELIVERIES_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange userEventsExchange() {
        return ExchangeBuilder.topicExchange(USER_EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deliveryReceiptConfirmedQueue() {
        return QueueBuilder.durable(DELIVERY_RECEIPT_CONFIRMED_FOR_REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deliveryAutoCompletedQueue() {
        return QueueBuilder.durable(DELIVERY_AUTO_COMPLETED_FOR_REVIEW_QUEUE)
                .withArgument("x-dead-letter-exchange", MAIN_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue userPaymentDefaultedQueue() {
        return QueueBuilder.durable(USER_PAYMENT_DEFAULTED_QUEUE)
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

    @Bean
    public Binding userPaymentDefaultedBinding(Queue userPaymentDefaultedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder.bind(userPaymentDefaultedQueue)
                .to(userEventsExchange)
                .with(USER_EVENT_PAYMENT_DEFAULTED_ROUTING_KEY);
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
    }

    // Message converter (can be shared if RabbitTemplate is also in this service)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter); // Use the bean
        return rabbitTemplate;
    }
}