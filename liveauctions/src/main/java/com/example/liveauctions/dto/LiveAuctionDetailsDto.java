package com.example.liveauctions.dto;

import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.client.dto.CategoryDto;
// Assuming ProductCondition enum is accessible, perhaps from a shared module or Products client library
// import com.example.products.entity.ProductCondition;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set; // For categories
import java.util.UUID;

@Value
@Builder(toBuilder = true) // toBuilder=true allows easy modification if needed post-fetch
public class LiveAuctionDetailsDto {

    // --- Core Auction Info (from LiveAuctions Service) ---
    UUID id;
    AuctionStatus status;
    LocalDateTime startTime;
    LocalDateTime endTime;
    LocalDateTime actualEndTime; // If ended
    BigDecimal startPrice;
    BigDecimal reservePrice; // May be null if not set
    boolean reserveMet;
    BigDecimal currentBid;
    BigDecimal currentBidIncrement;
    BigDecimal nextBidAmount; // Calculated: currentBid + currentBidIncrement
    String highestBidderId;
    String highestBidderUsernameSnapshot;
    long timeLeftMs; // Calculated
    String winnerId;
    BigDecimal winningBid;

    // --- Product Info (Partially snapshot, partially fetched via Feign) ---
    Long productId;
    String productTitleSnapshot; // Snapshot
    String productImageUrlSnapshot; // Snapshot (main image)
    // --- Enriched Product Details (Fetched via Feign) ---
    String productDescription; // Fetched
    String productCondition; // Fetched (Enum name as String, or mapped)
    Set<CategoryDto> productCategories; // Fetched
    List<String> productImageUrls; // Fetched (Full list including main image)
    // ---

    // --- Seller Info (Snapshot + potentially fetched) ---
    String sellerId;
    String sellerUsernameSnapshot; // Snapshot
    // --- Potentially enriched seller details if needed ---
    // String sellerFullName; // Fetched via Users service Feign client?

    // --- Bid History (from LiveAuctions Service) ---
    List<BidDto> recentBids;

}