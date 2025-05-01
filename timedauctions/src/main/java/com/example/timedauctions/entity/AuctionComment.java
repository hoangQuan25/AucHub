package com.example.timedauctions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp; // If comments can be edited

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timed_auction_comments", schema = "timed_auction_schema", indexes = {
        // Index for fetching comments/replies efficiently
        @Index(name = "idx_comment_auction_parent_time", columnList = "timedAuctionId, parentId, createdAt ASC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID timedAuctionId; // FK to TimedAuction

    @Column(name = "parentId") // Explicit column name
    private Long parentId; // ID of the comment this is a reply to (null for top-level)

    @Column(nullable = false)
    private String userId; // User who wrote the comment

    @Column(nullable = false)
    private String usernameSnapshot; // Snapshot of username

    @Lob
    @Column(nullable = false)
    private String commentText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Optional: if comments can be edited
    // @UpdateTimestamp
    // private LocalDateTime updatedAt;
}