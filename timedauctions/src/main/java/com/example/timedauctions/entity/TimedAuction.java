package com.example.timedauctions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "timed_auctions", schema = "timed_auction_schema") // Use a different schema/table
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimedAuction {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private String sellerId;

    // --- Snapshots ---
    @Column(nullable = false, updatable = false)
    private String productTitleSnapshot;

    @Column(length = 1024, updatable = false)
    private String productImageUrlSnapshot;

    @Column(nullable = false, updatable = false)
    private String sellerUsernameSnapshot;

    @ElementCollection(fetch = FetchType.EAGER) // Eager fetch might be ok if categories are always needed
    @CollectionTable(name = "timed_auction_categories",
            schema = "timed_auction_schema",
            joinColumns = @JoinColumn(name = "auction_id"))
    @Column(name = "category_id")
    private Set<Long> productCategoryIdsSnapshot = new HashSet<>();
    // --- End Snapshots ---

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal startPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal reservePrice;

    // Represents the CURRENT VISIBLE bid amount
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBid;

    // Calculated increment needed ABOVE the current visible bid
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBidIncrement;

    // User ID of the current leading bidder (whose proxy bid resulted in currentBid)
    private String highestBidderId;

    // Username snapshot of the current leading bidder
    private String highestBidderUsernameSnapshot;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime; // Planned end time (can be extended by soft-close)

    private LocalDateTime actualEndTime; // When it actually finished

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(nullable = false)
    private boolean reserveMet = false;

    // --- Final Outcome ---
    private String winnerId;

    @Column(precision = 19, scale = 2)
    private BigDecimal winningBid;

    @Column(nullable = false)
    private int bidCount = 0; // Total number of bids placed

    // Flag for timed auction specific behaviour if needed (e.g., different rules)
    // private String auctionType = "TIMED"; // Or dedicated field

    // Soft-close configuration might be stored per-auction or globally
    private boolean softCloseEnabled = true; // Example default

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}