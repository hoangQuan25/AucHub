package com.example.liveauctions.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling, backed by a message broker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Defines the HTTP URL that clients will connect to for the WebSocket handshake.
        // SockJS is used as a fallback for browsers that don't support WebSocket natively.
        // Endpoint matches the one clients will connect to.
        // Allow connections from the frontend origin.
        registry.addEndpoint("/ws") // The endpoint clients connect to (e.g., ws://localhost:8003/ws)
                .setAllowedOrigins("http://localhost:5173") // Your frontend origin
                .withSockJS(); // Use SockJS fallback options
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Configures the message broker.

        // 1. Prefix for destinations handled by the broker (topics/queues)
        // Clients will subscribe to destinations starting with "/topic" or "/queue"
        registry.enableSimpleBroker("/topic", "/queue"); // Use built-in simple broker

        // 2. Prefix for messages bound for @MessageMapping annotated methods (if any)
        // If clients send messages directly to the server app (not just subscribe)
        // registry.setApplicationDestinationPrefixes("/app");

        // 3. Optional: Configure user destination prefix (for user-specific messages)
        // Used by SimpMessagingTemplate.convertAndSendToUser
        // Default is "/user" - often doesn't need changing.
        // registry.setUserDestinationPrefix("/user");
    }
}
