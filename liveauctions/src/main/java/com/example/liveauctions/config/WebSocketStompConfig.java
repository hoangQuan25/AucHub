package com.example.liveauctions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${FRONTEND_ORIGIN_URL}")
    private String allowedOrigin;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws") // The endpoint clients connect to (e.g., ws://localhost:8003/ws)
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigin) // Your frontend origin
                .withSockJS(); // Use SockJS fallback options
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        registry.enableSimpleBroker("/topic", "/queue");

        registry.setApplicationDestinationPrefixes("/app");
    }
}
