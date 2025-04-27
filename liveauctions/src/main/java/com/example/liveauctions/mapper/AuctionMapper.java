package com.example.liveauctions.mapper; // Or a suitable package

import com.example.liveauctions.client.dto.ProductDto; // DTO from Products service
import com.example.liveauctions.dto.*; // Your auction DTOs
import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.entity.LiveAuction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections; // For empty list
import java.util.List;
import java.util.stream.Collectors;

@Component // Make it a Spring bean so it can be injected
public class AuctionMapper {

    // --- Mapping for Bid ---
    public BidDto mapToBidDto(Bid bid) {
        if (bid == null) {
            return null;
        }
        return BidDto.builder()
                .bidderId(bid.getBidderId())
                .bidderUsernameSnapshot(bid.getBidderUsernameSnapshot())
                .amount(bid.getAmount())
                .bidTime(bid.getBidTime())
                .build();
    }

    public List<BidDto> mapToBidDtoList(List<Bid> bids) {
        if (bids == null || bids.isEmpty()) {
            return Collections.emptyList();
        }
        return bids.stream()
                .map(this::mapToBidDto) // Reference the instance method
                .collect(Collectors.toList());
    }

    // --- Mapping for Auction Summary ---
    public LiveAuctionSummaryDto mapToLiveAuctionSummaryDto(LiveAuction auction) {
        if (auction == null) {
            return null;
        }
        // Determine bid display (current or start price)
        BigDecimal displayBid = auction.getCurrentBid() != null ? auction.getCurrentBid() : auction.getStartPrice();

        return LiveAuctionSummaryDto.builder()
                .id(auction.getId())
                .productTitleSnapshot(auction.getProductTitleSnapshot())
                .productImageUrlSnapshot(auction.getProductImageUrlSnapshot())
                .currentBid(displayBid) // Show current bid if available, else start price
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                // .sellerUsernameSnapshot(auction.getSellerUsernameSnapshot()) // Uncomment if needed
                .build();
    }

    // --- Mapping for Auction Details (incorporating enrichment) ---
    public LiveAuctionDetailsDto mapToLiveAuctionDetailsDto(
            LiveAuction auction,
            ProductDto productDto, // Can be null if Feign call failed
            List<BidDto> recentBidDtos, // Already mapped BidDtos
            long timeLeftMs,
            BigDecimal nextBidAmount
    ) {
        if (auction == null) {
            return null;
        }

        LiveAuctionDetailsDto.LiveAuctionDetailsDtoBuilder builder = LiveAuctionDetailsDto.builder();

        // Map from LiveAuction entity
        builder.id(auction.getId())
                .status(auction.getStatus())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .actualEndTime(auction.getActualEndTime())
                .startPrice(auction.getStartPrice())
                .reservePrice(auction.getReservePrice())
                .reserveMet(auction.isReserveMet())
                .productId(auction.getProductId())
                .productTitleSnapshot(auction.getProductTitleSnapshot())
                .productImageUrlSnapshot(auction.getProductImageUrlSnapshot())
                .sellerId(auction.getSellerId())
                .sellerUsernameSnapshot(auction.getSellerUsernameSnapshot())
                .currentBid(auction.getCurrentBid())
                .currentBidIncrement(auction.getCurrentBidIncrement())
                .highestBidderId(auction.getHighestBidderId())
                .highestBidderUsernameSnapshot(auction.getHighestBidderUsernameSnapshot())
                .winnerId(auction.getWinnerId())
                .winningBid(auction.getWinningBid());

        // Map enriched fields from ProductDto *if available*
        if (productDto != null) {
            builder.productDescription(productDto.getDescription());
            builder.productCondition(productDto.getCondition()); // Assumes enum type matches
            // Assuming ProductDto already has Set<CategoryDto> matching our definition
            builder.productCategories(productDto.getCategories());
            // Map other product fields if needed
        } else {
            // Optionally set defaults or leave null if productDto is null
            builder.productDescription(null); // Or "Details unavailable"
            builder.productCondition(null);
            builder.productCategories(Collections.emptySet());
        }


        // Map calculated/passed-in values
        builder.recentBids(recentBidDtos == null ? Collections.emptyList() : recentBidDtos);
        builder.timeLeftMs(timeLeftMs);
        builder.nextBidAmount(nextBidAmount);

        return builder.build();
    }

    // If CategoryDto mapping was needed (assuming ProductDto has different structure):
    /*
    private CategoryDto mapToCategoryDto(SourceCategoryDto sourceCategory) {
        if (sourceCategory == null) return null;
        return CategoryDto.builder()
            .id(sourceCategory.getId())
            .name(sourceCategory.getName())
            .build();
    }

    private Set<CategoryDto> mapToCategoryDtoSet(Set<SourceCategoryDto> sourceCategories) {
        if (sourceCategories == null || sourceCategories.isEmpty()) {
             return Collections.emptySet();
         }
         return sourceCategories.stream()
                 .map(this::mapToCategoryDto)
                 .collect(Collectors.toSet());
    }
    */

}