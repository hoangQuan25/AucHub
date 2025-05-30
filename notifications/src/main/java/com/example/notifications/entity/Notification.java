package com.example.notifications.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notifications", schema = "notifications_schema", indexes = { // Use dedicated schema
        // Index for fetching user's notifications, prioritizing unread and newest
        @Index(name = "idx_notification_user_read_time", columnList = "userId, isRead, createdAt DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Simple ID for notifications
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false) // Index userId for lookups
    private String userId; // The user this notification is FOR

    @NotNull
    @Column(nullable = false, updatable = false)
    private String type; // e.g., "AUCTION_ENDED", "OUTBID", "COMMENT_REPLY" (Could be Enum if preferred)

    @NotNull
    @Column(nullable = false, length = 500) // Adjust length as needed
    private String message; // The user-facing notification text

    @Column(nullable = false)
    private boolean isRead = false; // Default to unread

    // --- Optional: Links to related entities ---
    @Column(updatable = false)
    private UUID relatedAuctionId;

    @Column(updatable = false)
    private String relatedAuctionType; // "LIVE" or "TIMED"

    @Column(updatable = false)
    private Long relatedCommentId;

    @Column(updatable = false)
    private UUID relatedOrderId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optional: Add an expiry time if implementing auto-deletion
    // private LocalDateTime expiresAt;
}