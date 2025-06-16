package com.example.timedauctions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp; // Use UpdateTimestamp to track latest submission

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "timed_auction_proxy_bids", schema = "timed_auction_schema", indexes = {
        @Index(name = "idx_proxy_bid_auction_max", columnList = "timedAuctionId, maxBid DESC, submissionTime ASC"), // For finding winner/runner-up quickly
        @Index(name = "idx_proxy_bid_auction_bidder", columnList = "timedAuctionId, bidderId", unique = true) // Ensure one proxy bid per bidder per auction
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionProxyBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID timedAuctionId; // FK to TimedAuction

    @Column(nullable = false)
    private String bidderId; // User ID

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxBid; // The maximum amount the user is willing to bid

    @UpdateTimestamp // Track the latest time this bidder submitted/updated their max bid
    @Column(nullable = false)
    private LocalDateTime submissionTime;
}