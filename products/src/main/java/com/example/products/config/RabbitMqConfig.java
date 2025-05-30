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
    public static final String ORDERS_EVENTS_EXCHANGE = "orders_events_exchange"; // Must match the publisher's exchange name
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.event.completed"; // Must match the publisher's routing key
    public static final String PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE = "product_service_order_completed_queue";

    // --- Dead Letter Exchange and Queue ---
    public static final String MAIN_DLX_EXCHANGE = "dlx.main_exchange"; // Dead Letter Exchange
    public static final String MAIN_DEAD_LETTER_QUEUE = "q.main_dead_letter_queue"; // General Dead Letter Queue
    public static final String MAIN_DLQ_ROUTING_KEY = "dlq.main.key";

    @Bean
    TopicExchange ordersEventsExchange() { // Declare the exchange it listens to
        return new TopicExchange(ORDERS_EVENTS_EXCHANGE);
    }

    @Bean
    public DirectExchange mainDlxExchange() {
        return ExchangeBuilder.directExchange(MAIN_DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    Queue productServiceOrderCompletedQueue() {
        return QueueBuilder.durable(PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE)
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
    Binding bindingProductServiceOrderCompleted(Queue productServiceOrderCompletedQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(productServiceOrderCompletedQueue)
                .to(ordersEventsExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding mainDeadLetterBinding(Queue mainDeadLetterQueue, DirectExchange mainDlxExchange) {
        return BindingBuilder.bind(mainDeadLetterQueue)
                .to(mainDlxExchange)
                .with(MAIN_DLQ_ROUTING_KEY); // Bind the DLQ with the specific routing key
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
