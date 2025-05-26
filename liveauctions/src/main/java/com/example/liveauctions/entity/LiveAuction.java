package com.example.liveauctions.entity; // Adjust package as needed

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator; // For UUID generation

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "live_auctions", schema = "auction_schema") // Specify schema
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LiveAuction {

    @Id
    @UuidGenerator // Generates UUID
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id; // Using UUID for primary key

    @Column(nullable = false, updatable = false)
    private Long productId; // Reference to Product in Products Service

    @Column(nullable = false, updatable = false)
    private String sellerId; // Reference to User (Keycloak ID)

    // --- Snapshots from Product/User Service (at creation time) ---
    @Column(nullable = false, updatable = false)
    private String productTitleSnapshot;

    @Column(length = 1024, updatable = false) // Match Product entity length
    private String productImageUrlSnapshot; // Main image URL

    @Column(nullable = false, updatable = false)
    private String sellerUsernameSnapshot;

    @ElementCollection
    @CollectionTable(name = "auction_categories",
            joinColumns = @JoinColumn(name = "auction_id"))
    @Column(name = "category_id")
    private Set<Long> productCategoryIdsSnapshot = new HashSet<>();
    // --- End Snapshots ---

    @Column(nullable = false, precision = 19, scale = 2) // Example precision/scale for money
    private BigDecimal startPrice;

    @Column(precision = 19, scale = 2) // Nullable
    private BigDecimal reservePrice;

    @Column(precision = 19, scale = 2) // Nullable initially
    private BigDecimal currentBid;

    @Column(precision = 19, scale = 2) // Stores the calculated increment step
    private BigDecimal currentBidIncrement;

    private String highestBidderId; // Nullable (Reference to User - Keycloak ID)

    private String highestBidderUsernameSnapshot; // Nullable

    @Column(nullable = false)
    private LocalDateTime startTime; // Scheduled or actual start time

    @Column(nullable = false)
    private LocalDateTime endTime; // Calculated planned end time

    private LocalDateTime actualEndTime; // When it actually finished

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status; // Enum: SCHEDULED, ACTIVE, SOLD, RESERVE_NOT_MET, CANCELLED

    @Column(nullable = false)
    private boolean reserveMet = false; // Tracks if reserve price has been met

    // --- Final Outcome ---
    private String winnerId; // Nullable (Reference to User - Keycloak ID)

    @Column(precision = 19, scale = 2) // Nullable
    private BigDecimal winningBid; // Final selling price

    @Column(nullable = false)
    private int bidCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private boolean fastFinishOnReserve;
}
