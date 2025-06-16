package com.example.notifications.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auction_followers", schema = "notifications_schema",
        // Ensure a user can only follow an auction once
        uniqueConstraints = { @UniqueConstraint(columnNames = {"userId", "auctionId"}) },
        indexes = {
                @Index(name = "idx_follower_user", columnList = "userId"),
                @Index(name = "idx_follower_auction", columnList = "auctionId")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionFollower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private String userId;

    @NotNull
    @Column(nullable = false, updatable = false)
    private UUID auctionId;

    @NotNull
    @Column(nullable = false, updatable = false, length = 20)
    private String auctionType; // e.g., "LIVE", "TIMED"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime followedAt;
}