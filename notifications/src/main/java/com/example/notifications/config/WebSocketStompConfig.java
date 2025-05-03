package com.example.notifications.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Enables WebSocket message handling
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint clients will connect to for WebSocket handshakes with THIS service
        // Needs to be proxied by the Gateway
        registry.addEndpoint("/ws/notifications") // Different path from auction service WS
                // TODO: Add authentication interceptor if needed (e.g., extract user from token/header)
                // .addInterceptors(new UserHandshakeInterceptor())
                .setAllowedOrigins("http://localhost:5173") // Your frontend origin(s)
                .withSockJS(); // Use SockJS fallback options
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixes for messages sent FROM server TO client
        // Use /user for user-specific messages (Spring targets specific sessions)
        // Use /queue as an alternative convention for point-to-point
        // Use /topic for broadcasts (less likely needed for direct notifications)
        registry.enableSimpleBroker("/queue", "/user"); // Enable broker for user destinations

        // Prefix for messages sent FROM client TO server (e.g., if client sends a "mark as read" message)
        registry.setApplicationDestinationPrefixes("/app");

        // Configure user destination prefix (used by SimpMessagingTemplate.convertAndSendToUser)
        registry.setUserDestinationPrefix("/user");
    }
}