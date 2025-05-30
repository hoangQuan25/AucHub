package com.example.notifications.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class NotificationDto {
    // private String id; // Optional: ID if stored
    private String type; // e.g., "AUCTION_ENDED", "OUTBID", "COMMENT_REPLY"
    private String message; // User-friendly message
    private LocalDateTime timestamp;
    private UUID relatedAuctionId; // Link back to the auction
    private String relatedAuctionType;
    private UUID relatedOrderId;
    private boolean isRead; // Status (if managing read status)
    private Map<String, Object> details; // Optional map for extra structured data
}