package com.example.liveauctions.websocket; // Your websocket package

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component // Mark as a Spring bean
@RequiredArgsConstructor
@Slf4j
public class AuctionWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    // Pattern to extract auctionId from path like /ws/liveauctions/uuid-goes-here
    private static final Pattern AUCTION_ID_PATTERN = Pattern.compile("/ws/liveauctions/([^/]+)");

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        URI uri = session.getHandshakeInfo().getUri();
        String path = uri.getPath();
        log.debug("WebSocket handshake request for path: {}", path);

        UUID auctionId = extractAuctionId(path);
        if (auctionId == null) {
            log.warn("Could not extract auctionId from WebSocket path: {}", path);
            return session.close(org.springframework.web.reactive.socket.CloseStatus.POLICY_VIOLATION.withReason("Invalid path"));
        }

        // Get the principal Mono
        Mono<Principal> principalMono = session.getHandshakeInfo().getPrincipal();

        // Chain starts with Mono<Principal>, flatMap expects a Principal.
        // The result of flatMap (if executed) is Mono<Void>.
        // switchIfEmpty provides an alternative Mono<Void> if principalMono was empty.
        return principalMono
                .flatMap(principal -> {
                    // --- Authenticated Path ---
                    // This block executes only if principalMono emits a Principal
                    String userId = principal.getName();
                    log.info("WebSocket session {} opened for auction {} by user {}", session.getId(), auctionId, userId);

                    // Register the session (this now happens only if authenticated)
                    sessionManager.addSession(auctionId, session);

                    // Keep connection alive and handle cleanup logic for authenticated session
                    return session.receive()
                            .doOnNext(message -> {
                                // Optional: Handle incoming messages if needed (e.g., pings)
                                // log.trace("Received message on WebSocket session {}: {}", session.getId(), message.getPayloadAsText());
                            })
                            .doOnError(error -> {
                                log.error("Error during WebSocket session {} for auction {}: {}", session.getId(), auctionId, error.getMessage());
                                // Cleanup is handled by doFinally or closeStatus in sessionManager
                            })
                            .doFinally(signalType -> {
                                // Note: removeSession might be called twice (here and in sessionManager's close handler)
                                // but removeSession should handle that gracefully.
                                log.info("WebSocket session {} closing (Signal: {}) for auction {} in flatMap. Removing session.", session.getId(), signalType, auctionId);
                                sessionManager.removeSession(auctionId, session);
                            })
                            .then(); // Ensure flatMap returns Mono<Void>
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // --- Unauthenticated Path ---
                    // This block executes only if principalMono was empty
                    log.warn("Unauthenticated WebSocket connection attempt for auction {}", auctionId);
                    // Close the session immediately and return the Mono<Void> from close()
                    return session.close(org.springframework.web.reactive.socket.CloseStatus.POLICY_VIOLATION.withReason("Authentication required"));
                })); // The result of switchIfEmpty here is also Mono<Void>, matching flatMap's result type
    }

    // Helper to extract UUID from path
    private UUID extractAuctionId(String path) {
        Matcher matcher = AUCTION_ID_PATTERN.matcher(path);
        if (matcher.matches()) {
            try {
                return UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format in WebSocket path: {}", path);
                return null;
            }
        }
        return null;
    }
}