package com.example.timedauctions.dto;

import com.example.timedauctions.entity.AuctionStatus; // Adjust import
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.List; // For images/bids/comments later

// Add relevant client DTOs if needed (e.g., CategoryDto)
import com.example.timedauctions.client.dto.CategoryDto;


@Data
@Builder
public class TimedAuctionDetailsDto {
    // --- Core Auction Info ---
    private UUID id;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // Planned end time
    private LocalDateTime actualEndTime; // Actual end time
    private BigDecimal startPrice;
    private BigDecimal reservePrice;
    private boolean reserveMet;

    // --- Product Info (Snapshots + Enriched) ---
    private Long productId;
    private String productTitleSnapshot;
    private String productImageUrlSnapshot; // Main image
    private String productDescription; // Enriched
    private String productCondition; // Enriched
    private Set<CategoryDto> productCategories; // Enriched (Use client DTO)
    private List<String> productImageUrls; // Enriched (All images)

    // --- Seller Info ---
    private String sellerId;
    private String sellerUsernameSnapshot;

    // --- Bidding Info ---
    private BigDecimal currentBid; // VISIBLE current bid
    private String highestBidderId;
    private String highestBidderUsernameSnapshot;
    private BigDecimal nextBidAmount; // Calculated minimum next manual bid needed

    private int bidCount; // Total number of bids placed

    // --- Timing ---
    private long timeLeftMs; // Calculated time remaining

    // --- Outcome ---
    private String winnerId;
    private BigDecimal winningBid;

    // --- Data for UI (fetched separately or included) ---
    private List<BidDto> recentBids; // History of VISIBLE bids
}