package com.example.liveauctions.websocket; // Your websocket package

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage; // WebFlux specific
import org.springframework.web.reactive.socket.WebSocketSession; // WebFlux specific
import reactor.core.publisher.Mono; // WebFlux specific

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component // Make it a Spring bean
@Slf4j
public class WebSocketSessionManager {

    // Thread-safe map: Auction ID -> Set of Sessions connected to this instance for that auction
    private final Map<UUID, Set<WebSocketSession>> sessionsByAuction = new ConcurrentHashMap<>();

    /**
     * Registers a new WebSocket session for a given auction ID.
     * Also sets up logic to automatically remove the session upon closure.
     *
     * @param auctionId The ID of the auction the session is related to.
     * @param session   The WebSocketSession to register.
     */
    public void addSession(UUID auctionId, WebSocketSession session) {
        // Use computeIfAbsent for thread-safe creation of the set if it doesn't exist
        // CopyOnWriteArraySet is suitable for scenarios where reads/iterations are more frequent than writes (add/remove)
        // and provides thread safety for iteration during broadcasts.
        Set<WebSocketSession> sessions = sessionsByAuction.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>());
        sessions.add(session);
        log.info("WebSocket session {} added for auction {}", session.getId(), auctionId);

        // --- Crucial: Handle Session Closure ---
        // Register cleanup logic when the session closes for any reason
        session.closeStatus()
                .doFinally(signalType -> { // Executed on complete, error, or cancel
                    removeSession(auctionId, session);
                    log.info("WebSocket session {} cleanup executed (Signal: {}) for auction {}", session.getId(), signalType, auctionId);
                })
                .subscribe(); // Subscribe to activate the close handling
    }

    /**
     * Removes a WebSocket session associated with an auction ID.
     * If the set for an auction becomes empty, the auction ID entry is removed from the map.
     *
     * @param auctionId The ID of the auction.
     * @param session   The WebSocketSession to remove.
     */
    public void removeSession(UUID auctionId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByAuction.get(auctionId);
        if (sessions != null) {
            boolean removed = sessions.remove(session);
            if (removed) {
                log.info("WebSocket session {} removed for auction {}", session.getId(), auctionId);
                // If the set becomes empty, remove the auction entry to free up memory
                if (sessions.isEmpty()) {
                    sessionsByAuction.remove(auctionId);
                    log.info("Auction {} entry removed from session map as it's now empty.", auctionId);
                }
            }
        }
    }

    /**
     * Checks if there are any active WebSocket sessions for a given auction ID
     * managed by this service instance.
     *
     * @param auctionId The ID of the auction.
     * @return true if active sessions exist for this auction on this instance, false otherwise.
     */
    public boolean hasActiveSessions(UUID auctionId) {
        Set<WebSocketSession> sessions = sessionsByAuction.get(auctionId);
        // We should also ideally check if sessions are still open, but for a quick check this is often sufficient.
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Sends a message payload (as JSON string) to all active WebSocket sessions
     * connected to the specified auction ID on this service instance.
     *
     * @param auctionId The ID of the target auction.
     * @param payload   The JSON string payload to send.
     */
    public void broadcastToAuction(UUID auctionId, String payload) {
        Set<WebSocketSession> sessions = sessionsByAuction.get(auctionId);
        if (sessions != null && !sessions.isEmpty()) {
            log.debug("Broadcasting payload to {} sessions for auction {}", sessions.size(), auctionId);
            WebSocketMessage message = sessions.iterator().next().textMessage(payload); // Create message once

            sessions.forEach(session -> {
                if (session.isOpen()) {
                    session.send(Mono.just(message))
                            .doOnError(error -> {
                                // Log error and attempt removal if send fails
                                log.error("Error sending message to WebSocket session {}: {}", session.getId(), error.getMessage());
                                removeSession(auctionId, session);
                            })
                            .subscribe(); // Subscribe to actually send the message
                } else {
                    // Proactively remove sessions found to be closed
                    removeSession(auctionId, session);
                }
            });
        } else {
            log.trace("No sessions found for auction {} on this instance to broadcast to.", auctionId);
        }
    }
}