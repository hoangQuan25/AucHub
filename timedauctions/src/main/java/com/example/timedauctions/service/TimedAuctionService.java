package com.example.timedauctions.service;

import com.example.timedauctions.dto.*;
import com.example.timedauctions.entity.AuctionStatus; // If filtering by status needed
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TimedAuctionService {

    TimedAuctionDetailsDto createAuction(String sellerId, CreateTimedAuctionDto createDto);

    TimedAuctionDetailsDto getAuctionDetails(UUID auctionId);

    void placeMaxBid(UUID auctionId, String bidderId, PlaceMaxBidDto bidDto);

    CommentDto createComment(UUID auctionId, String userId, CreateCommentDto commentDto);

    List<CommentDto> getComments(UUID auctionId);

    Page<TimedAuctionSummaryDto> getActiveAuctions(Pageable pageable);

    void cancelAuction(UUID auctionId, String sellerId);

    MyMaxBidDto getMyMaxBidForAuction(UUID auctionId, String bidderId);

    void endAuctionEarly(UUID auctionId, String sellerId);

    Page<TimedAuctionSummaryDto> getSellerAuctions(
            String sellerId,
            AuctionStatus status, // Explicit status filter
            Boolean ended,      // Flag for all ended types
            Set<Long> categoryIds,
            LocalDateTime from,
            Pageable pageable
    );

    List<TimedAuctionSummaryDto> getAuctionSummariesByIds(Set<UUID> auctionIds);

    Page<TimedAuctionSummaryDto> searchAuctions(
            String query,
            Set<Long> categoryIds,
            AuctionStatus status,
            Boolean ended,
            LocalDateTime from,
            Pageable pageable
    );

    CommentDto editComment(UUID auctionId, Long commentId, String userId, UpdateCommentDto updateDto);

    void deleteComment(UUID auctionId, Long commentId, String userId);

}