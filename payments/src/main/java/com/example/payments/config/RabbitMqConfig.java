package com.example.payments.config; // In your Payment Service

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder; // For a fluent way to build
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENTS_EVENTS_EXCHANGE = "payments_events_exchange";

    // Routing keys for events published BY this Payment Service
    public static final String PAYMENT_EVENT_SUCCEEDED_ROUTING_KEY = "payment.event.succeeded";
    public static final String PAYMENT_EVENT_FAILED_ROUTING_KEY = "payment.event.failed";

    @Bean
    public TopicExchange paymentsEventsExchange() {
        return ExchangeBuilder.topicExchange(PAYMENTS_EVENTS_EXCHANGE)
                .durable(true)
                .build();
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