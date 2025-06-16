package com.example.liveauctions.listener; // Ensure this matches your package

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier; // For specifying the RedisTemplate bean
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations; // For typed Set operations
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import jakarta.annotation.PostConstruct; // For initializing SetOperations
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AuctionViewerCountListener {

    private static final Logger logger = LoggerFactory.getLogger(AuctionViewerCountListener.class);

    private static final Pattern AUCTION_DATA_TOPIC_PATTERN =
            Pattern.compile("^/topic/auctions/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$");

    private static final Pattern AUCTION_VIEWERS_TOPIC_PATTERN =
            Pattern.compile("^/topic/auctions/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})/viewers$");

    private static final String VIEWER_SET_KEY_PREFIX = "viewers:auction:";
    private static final String SESSION_AUCTIONS_KEY_PREFIX = "session:auctions:";


    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate; // Type remains the same
    private SetOperations<String, String> setOps;

    public AuctionViewerCountListener(SimpMessagingTemplate messagingTemplate,
                                      // Use the new bean name in @Qualifier
                                      @Qualifier("viewerCountRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        logger.info("AuctionViewerCountListener initialized to use Redis for viewer tracking.");
    }

    @PostConstruct
    private void init() {
        setOps = redisTemplate.opsForSet();
    }

    private String getViewerRedisKey(UUID auctionId) {
        return VIEWER_SET_KEY_PREFIX + auctionId.toString();
    }

    private String getSessionAuctionsRedisKey(String sessionId) {
        return SESSION_AUCTIONS_KEY_PREFIX + sessionId;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor sha = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        String userName = Optional.ofNullable(sha.getUser()).map(Principal::getName).orElse("N/A");
        logger.info("WebSocket Session Connected (Redis): ID='{}', User='{}'", sessionId, userName);
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        Message<byte[]> message = event.getMessage();
        SimpMessageHeaderAccessor sha = SimpMessageHeaderAccessor.wrap(message);
        String destination = sha.getDestination();
        String sessionId = sha.getSessionId();
        String stompSubscriptionId = sha.getSubscriptionId();

        if (destination == null || sessionId == null) {
            logger.warn("Subscription event with null destination or session ID. Dest: '{}', SID: '{}', STOMPSubID: '{}'",
                    destination, sessionId, stompSubscriptionId);
            return;
        }

        logger.debug("STOMP SUBSCRIBE (Redis): SID='{}', STOMPSubId='{}', Dest='{}'",
                sessionId, stompSubscriptionId, destination);

        Matcher auctionDataMatcher = AUCTION_DATA_TOPIC_PATTERN.matcher(destination);
        if (auctionDataMatcher.matches()) {
            String auctionIdString = auctionDataMatcher.group(1);
            try {
                UUID auctionId = UUID.fromString(auctionIdString);
                String auctionViewersKey = getViewerRedisKey(auctionId);
                String sessionAuctionsKey = getSessionAuctionsRedisKey(sessionId);

                logger.info("Processing AUCTION DATA subscription (Redis): AuctionId='{}', SID='{}'", auctionIdString, sessionId);

                Long added = setOps.add(auctionViewersKey, sessionId);

                if (added != null && added > 0) {
                    setOps.add(sessionAuctionsKey, auctionId.toString());
                    logger.info("SUCCESS: SID='{}' ADDED to Redis Set '{}'. Broadcasting count.", sessionId, auctionViewersKey);
                    broadcastCount(auctionId, "new_auction_data_subscriber_redis");
                } else {
                    logger.warn("SID='{}' was ALREADY in Redis Set '{}'. Not re-broadcasting unless necessary.", sessionId, auctionViewersKey);
                    setOps.add(sessionAuctionsKey, auctionId.toString());
                }

            } catch (IllegalArgumentException e) {
                logger.error("Failed to parse AuctionId from AUCTION DATA dest (Redis): '{}'. Error: {}", destination, e.getMessage(), e);
            }
            return;
        }

        Matcher viewersTopicMatcher = AUCTION_VIEWERS_TOPIC_PATTERN.matcher(destination);
        if (viewersTopicMatcher.matches()) {
            String auctionIdString = viewersTopicMatcher.group(1);
            try {
                UUID auctionId = UUID.fromString(auctionIdString);
                long currentActualCount = getViewerCountForAuction(auctionId);

                logger.info("Processing VIEWERS topic subscription (Redis): AuctionId='{}', SID='{}'. Current count: {}",
                        auctionIdString, sessionId, currentActualCount);

                String viewersDestination = "/topic/auctions/" + auctionId + "/viewers";
                messagingTemplate.convertAndSend(
                        viewersDestination,
                        Map.of("count", currentActualCount, "type", "initial_count_on_viewers_subscribe_redis")
                );
                logger.info("Sent count '{}' to '{}' (SID='{}' subscribed). Type: initial_count_on_viewers_subscribe_redis",
                        currentActualCount, viewersDestination, sessionId);

            } catch (IllegalArgumentException e) {
                logger.error("Failed to parse AuctionId from VIEWERS dest (Redis): '{}'. Error: {}", destination, e.getMessage(), e);
            }
            return;
        }

        logger.trace("SID='{}' (STOMPSubId='{}') subscribed to unhandled STOMP topic (Redis): {}",
                sessionId, stompSubscriptionId, destination);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Message<byte[]> message = event.getMessage();
        SimpMessageHeaderAccessor sha = SimpMessageHeaderAccessor.wrap(message);
        String sessionId = sha.getSessionId();
        String userName = Optional.ofNullable(sha.getUser()).map(Principal::getName).orElse("N/A");

        if (sessionId == null) {
            logger.warn("Disconnect event with null SID (Redis). Event: {}", event.toString());
            return;
        }

        logger.info("WebSocket Session Disconnected (Redis): SID='{}', User='{}'. Removing from tracked auctions.",
                sessionId, userName);

        String sessionAuctionsKey = getSessionAuctionsRedisKey(sessionId);
        Set<String> auctionIdsWatchedBySession = setOps.members(sessionAuctionsKey);

        if (auctionIdsWatchedBySession != null && !auctionIdsWatchedBySession.isEmpty()) {
            logger.debug("SID='{}' was subscribed to auctions: {}", sessionId, auctionIdsWatchedBySession);
            for (String auctionIdStr : auctionIdsWatchedBySession) {
                try {
                    UUID auctionId = UUID.fromString(auctionIdStr);
                    String auctionViewersKey = getViewerRedisKey(auctionId);
                    Long removed = setOps.remove(auctionViewersKey, sessionId);
                    if (removed != null && removed > 0) {
                        logger.info("SUCCESS: SID='{}' REMOVED from Redis Set '{}' for AuctionId='{}'. Broadcasting count.",
                                sessionId, auctionViewersKey, auctionId);
                        broadcastCount(auctionId, "session_disconnect_redis");
                    } else {
                        logger.warn("SID='{}' NOT FOUND in Redis Set '{}' for AuctionId='{}' during disconnect, though it was tracked in '{}'.",
                                sessionId, auctionViewersKey, auctionId, sessionAuctionsKey);
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Error parsing auctionId '{}' from session tracking set for SID='{}'", auctionIdStr, sessionId, e);
                }
            }
            redisTemplate.delete(sessionAuctionsKey);
            logger.info("Cleaned up session tracking key '{}'", sessionAuctionsKey);
        } else {
            logger.info("SID='{}' was not actively tracked as viewing any auctions. No Redis removals needed from session tracking.", sessionId);
        }
    }

    private void broadcastCount(UUID auctionId, String reason) {
        long count = getViewerCountForAuction(auctionId);
        String destination = "/topic/auctions/" + auctionId + "/viewers";

        logger.info("Broadcasting viewer count (Redis): AuctionId='{}', Count={}, Reason='{}', Dest='{}'",
                auctionId, count, reason, destination);
        messagingTemplate.convertAndSend(
                destination,
                Map.of("count", count, "reasonForBroadcast", reason)
        );
    }

    public long getViewerCountForAuction(UUID auctionId) {
        String auctionViewersKey = getViewerRedisKey(auctionId);
        Long count = setOps.size(auctionViewersKey);
        return count != null ? count : 0L;
    }

    public Set<String> getAuctionViewersSnapshot(UUID auctionId) {
        String auctionViewersKey = getViewerRedisKey(auctionId);
        return setOps.members(auctionViewersKey);
    }
}