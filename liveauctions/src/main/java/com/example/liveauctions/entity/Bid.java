package com.example.liveauctions.entity; // Adjust package as needed

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bids", schema = "auction_schema", indexes = {
        @Index(name = "idx_bid_auction_time", columnList = "liveAuctionId, bidTime DESC") // Index for fetching history
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Simple sequence for bids is fine
    private Long id;

    @Column(nullable = false)
    private UUID liveAuctionId; // Foreign key reference to LiveAuction

    @Column(nullable = false)
    private String bidderId; // Reference to User (Keycloak ID)

    @Column(nullable = false)
    private String bidderUsernameSnapshot; // Snapshot for display

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime bidTime;
}