package com.example.liveauctions.config; // Your config package

import com.example.liveauctions.websocket.AuctionWebSocketHandler; // Your handler
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final AuctionWebSocketHandler auctionWebSocketHandler; // Inject your handler

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        // Map URL paths to WebSocketHandler beans
        Map<String, WebSocketHandler> map = new HashMap<>();
        // Handle paths like /ws/liveauctions/uuid-goes-here...
        // The pattern ensures it matches paths with a UUID segment
        map.put("/ws/liveauctions/**", auctionWebSocketHandler);

        // Order ensures this mapping is checked before other potential mappings
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1); // High priority
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    // Required adapter to bridge HTTP handling to WebSocketHandler
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}