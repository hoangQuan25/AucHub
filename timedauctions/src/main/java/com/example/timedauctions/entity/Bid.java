package com.example.timedauctions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// Use different schema/table, update index name
@Table(name = "timed_bids", schema = "timed_auction_schema", indexes = {
        @Index(name = "idx_timed_bid_auction_time", columnList = "timedAuctionId, bidTime DESC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID timedAuctionId; // FK to TimedAuction

    @Column(nullable = false)
    private String bidderId; // User ID who placed the bid (or whose proxy placed it)

    @Column(nullable = false)
    private String bidderUsernameSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // The visible bid amount recorded

    @Column(nullable = false)
    private boolean isAutoBid = false; // Flag to indicate if system placed this visible bid

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime bidTime;
}