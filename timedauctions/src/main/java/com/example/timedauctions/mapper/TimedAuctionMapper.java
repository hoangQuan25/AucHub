package com.example.timedauctions.mapper;

import com.example.timedauctions.client.dto.ProductDto;
import com.example.timedauctions.dto.BidDto;
import com.example.timedauctions.dto.CommentDto;
import com.example.timedauctions.dto.TimedAuctionDetailsDto;
import com.example.timedauctions.dto.TimedAuctionSummaryDto; // Add if needed
import com.example.timedauctions.entity.AuctionComment;
import com.example.timedauctions.entity.Bid;
import com.example.timedauctions.entity.TimedAuction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

// Assuming client DTOs are available in appropriate package
import com.example.timedauctions.client.dto.CategoryDto;


@Component
@Slf4j
public class TimedAuctionMapper {

    // --- Bid Mapping ---
    public BidDto mapToBidDto(Bid bid) {
        if (bid == null) return null;
        return BidDto.builder()
                .bidderId(bid.getBidderId())
                .bidderUsernameSnapshot(bid.getBidderUsernameSnapshot())
                .amount(bid.getAmount())
                .bidTime(bid.getBidTime())
                .isAutoBid(bid.isAutoBid()) // Map the new flag
                .build();
    }

    public List<BidDto> mapToBidDtoList(List<Bid> bids) {
        if (bids == null || bids.isEmpty()) return Collections.emptyList();
        return bids.stream().map(this::mapToBidDto).collect(Collectors.toList());
    }

    // --- Summary Mapping (Add if needed) ---
    public TimedAuctionSummaryDto mapToTimedAuctionSummaryDto(TimedAuction auction) {
        if (auction == null) return null;
        BigDecimal displayBid = auction.getCurrentBid() != null ? auction.getCurrentBid() : auction.getStartPrice();
        return TimedAuctionSummaryDto.builder() // Assuming TimedAuctionSummaryDto exists
                .id(auction.getId())
                .productTitleSnapshot(auction.getProductTitleSnapshot())
                .productImageUrlSnapshot(auction.getProductImageUrlSnapshot())
                .currentBid(displayBid)
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .categoryIds(auction.getProductCategoryIdsSnapshot() != null ? new HashSet<>(auction.getProductCategoryIdsSnapshot()) : Collections.emptySet())
                .build();
    }


    // --- Details Mapping ---
    public TimedAuctionDetailsDto mapToTimedAuctionDetailsDto(
            TimedAuction auction,
            ProductDto productDto, // Can be null
            List<BidDto> recentBidDtos,
            long timeLeftMs,
            BigDecimal nextBidAmount // Min amount for next manual bid
    ) {
        if (auction == null) return null;

        TimedAuctionDetailsDto.TimedAuctionDetailsDtoBuilder builder = TimedAuctionDetailsDto.builder();

        // --- Map from TimedAuction entity ---
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
                .currentBid(auction.getCurrentBid()) // VISIBLE bid
                // .currentBidIncrement(auction.getCurrentBidIncrement()) // Maybe not directly needed in DTO?
                .highestBidderId(auction.getHighestBidderId())
                .highestBidderUsernameSnapshot(auction.getHighestBidderUsernameSnapshot())
                .winnerId(auction.getWinnerId())
                .winningBid(auction.getWinningBid());

        // --- Map enriched Product Details ---
        if (productDto != null) {
            builder.productDescription(productDto.getDescription());
            // Assuming Condition is an enum or string in ProductDto
            builder.productCondition(productDto.getCondition() != null ? productDto.getCondition().toString() : null);
            // Assuming Set<CategoryDto> structure matches
            builder.productCategories(productDto.getCategories() != null ? productDto.getCategories() : Collections.emptySet());
            builder.productImageUrls(productDto.getImageUrls() != null ? productDto.getImageUrls() : Collections.emptyList());
        } else {
            builder.productDescription(null);
            builder.productCondition(null);
            builder.productCategories(Collections.emptySet());
            builder.productImageUrls(Collections.emptyList());
        }

        // --- Map Calculated/Passed Values ---
        builder.recentBids(recentBidDtos == null ? Collections.emptyList() : recentBidDtos);
        builder.timeLeftMs(timeLeftMs);
        builder.nextBidAmount(nextBidAmount); // Min next manual bid required

        return builder.build();
    }

    public CommentDto mapToCommentDto(AuctionComment comment) {
        if (comment == null) {
            return null;
        }
        return CommentDto.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .usernameSnapshot(comment.getUsernameSnapshot())
                .commentText(comment.getCommentText())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParentId())
                // replies and replyCount will be populated by the tree-building logic
                .replies(new ArrayList<>()) // Initialize replies list
                .build();
    }

    // Helper to map a flat list of comments into a nested DTO structure (up to one level deep for now)
    public List<CommentDto> mapToNestedCommentDtoList(List<AuctionComment> allComments) {
        if (allComments == null || allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // Map all comments to DTOs first, indexed by ID for easy lookup
        Map<Long, CommentDto> dtoMap = allComments.stream()
                .map(this::mapToCommentDto)
                .collect(Collectors.toMap(CommentDto::getId, dto -> dto));

        // List to hold the final top-level comments
        List<CommentDto> topLevelDtos = new ArrayList<>();

        // Iterate through the original entities to build the tree structure in the DTOs
        for (AuctionComment comment : allComments) {
            CommentDto currentDto = dtoMap.get(comment.getId());
            if (comment.getParentId() == null) {
                // This is a top-level comment
                topLevelDtos.add(currentDto);
            } else {
                // This is a reply, find its parent DTO in the map
                CommentDto parentDto = dtoMap.get(comment.getParentId());
                if (parentDto != null) {
                    // Add the current DTO as a reply to its parent
                    parentDto.getReplies().add(currentDto);
                    // Optionally update parent's reply count here if needed elsewhere
                    // parentDto.setReplyCount(parentDto.getReplies().size());
                } else {
                    // Parent comment not found in the current batch? Log warning or handle.
                    // Could happen if fetching paginated replies separately.
                    // For fetch-all strategy, this shouldn't occur unless data inconsistency.
                    log.warn("Parent comment with ID {} not found for reply {}", comment.getParentId(), comment.getId());
                    // Optionally add orphans to top-level or ignore
                    // topLevelDtos.add(currentDto);
                }
            }
        }

        // Sort top-level comments (e.g., by creation date)
        topLevelDtos.sort(Comparator.comparing(CommentDto::getCreatedAt));

        // Sort replies within each top-level comment
        for (CommentDto dto : topLevelDtos) {
            dto.getReplies().sort(Comparator.comparing(CommentDto::getCreatedAt));
            dto.setReplyCount(dto.getReplies().size()); // Set the count based on direct replies
        }


        return topLevelDtos;
    }
}