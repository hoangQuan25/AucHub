package com.example.products.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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

    @Bean
    Queue productServiceOrderCompletedQueue() {
        return new Queue(PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE, true);
    }

    @Bean
    TopicExchange ordersEventsExchange() { // Declare the exchange it listens to
        return new TopicExchange(ORDERS_EVENTS_EXCHANGE);
    }

    @Bean
    Binding bindingProductServiceOrderCompleted(Queue productServiceOrderCompletedQueue, TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(productServiceOrderCompletedQueue)
                .to(ordersEventsExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
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
