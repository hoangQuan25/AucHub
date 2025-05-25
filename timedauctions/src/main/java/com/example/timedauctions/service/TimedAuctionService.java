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

    /**
     * Creates a new timed auction.
     * @param sellerId The ID of the user creating the auction.
     * @param createDto DTO containing creation details.
     * @return DTO representing the created auction's details.
     */
    TimedAuctionDetailsDto createAuction(String sellerId, CreateTimedAuctionDto createDto);

    /**
     * Retrieves the detailed view of a specific timed auction.
     * @param auctionId The ID of the auction to retrieve.
     * @return DTO containing detailed auction information.
     * @throws com.example.timedauctions.exception.AuctionNotFoundException if auction not found.
     */
    TimedAuctionDetailsDto getAuctionDetails(UUID auctionId);

    /**
     * Places or updates a maximum proxy bid for a user on an auction.
     * @param auctionId The ID of the auction.
     * @param bidderId The ID of the user placing the bid.
     * @param bidDto DTO containing the maximum bid amount.
     * @throws com.example.timedauctions.exception.AuctionNotFoundException if auction not found.
     * @throws com.example.timedauctions.exception.InvalidAuctionStateException if auction is not active or ended.
     * @throws com.example.timedauctions.exception.InvalidBidException for various bid validation errors.
     */
    void placeMaxBid(UUID auctionId, String bidderId, PlaceMaxBidDto bidDto);

    /**
     * Creates a new comment or reply on a timed auction.
     * @param auctionId The ID of the auction being commented on.
     * @param userId The ID of the user posting the comment.
     * @param commentDto DTO containing comment text and optional parentId.
     * @return The created CommentDto.
     */
    CommentDto createComment(UUID auctionId, String userId, CreateCommentDto commentDto);

    /**
     * Retrieves comments for a specific auction.
     * Currently fetches all comments and builds a nested structure one level deep.
     * Consider pagination for top-level comments if performance becomes an issue.
     * @param auctionId The ID of the auction.
     * @return A list of top-level CommentDto objects, each potentially containing direct replies.
     */
    List<CommentDto> getComments(UUID auctionId);

    /**
     * Retrieves a paginated list of active timed auctions.
     * @param pageable Pagination information.
     * @return A page of TimedAuctionSummaryDto objects representing active auctions.
     */
    Page<TimedAuctionSummaryDto> getActiveAuctions(Pageable pageable);

    /**
     * Initiates the cancellation of a timed auction by the seller.
     * @param auctionId The ID of the auction to cancel.
     * @param sellerId The ID of the user attempting to cancel.
     */
    void cancelAuction(UUID auctionId, String sellerId);

    /**
     * Initiates an early end ("hammer down") for a timed auction by the seller.
     * Requires bids to be present. Reserve price status might not be strictly required.
     * @param auctionId The ID of the auction to end early.
     * @param sellerId The ID of the user attempting to end the auction.
     */
    void endAuctionEarly(UUID auctionId, String sellerId);

    /**
     * Retrieves a paginated list of auctions for a specific seller.
     * Supports filtering by auction status and ended state.
     * @param sellerId The ID of the seller whose auctions to retrieve.
     * @param status Optional filter for auction status (e.g., ACTIVE, SOLD, etc.).
     * @param ended Optional flag to filter by ended state (true for all ended types).
     * @param categoryIds Optional set of category IDs to filter auctions by categories.
     * @param from Optional start date-time to filter auctions created after this time.
     * @param pageable Pagination information.
     * @return A page of TimedAuctionSummaryDto objects representing the seller's auctions.
     */
    Page<TimedAuctionSummaryDto> getSellerAuctions(
            String sellerId,
            AuctionStatus status, // Explicit status filter
            Boolean ended,      // Flag for all ended types
            Set<Long> categoryIds,
            LocalDateTime from,
            Pageable pageable
    );

    /**
     * Fetches summary details for a list of auction IDs.
     * @param auctionIds Set of UUIDs representing the auction IDs.
     * @return List of TimedAuctionSummaryDto objects for the specified auction IDs.
     */
    List<TimedAuctionSummaryDto> getAuctionSummariesByIds(Set<UUID> auctionIds);

    /**
     * Searches for timed auctions based on various filters.
     * Supports searching by query string, category IDs, auction status, ended state, and date range.
     * @param query Search query string (e.g., product name or description).
     * @param categoryIds Set of category IDs to filter auctions by categories.
     * @param status Optional filter for auction status (e.g., ACTIVE, SOLD, etc.).
     * @param ended Optional flag to filter by ended state (true for all ended types).
     * @param from Optional start date-time to filter auctions created after this time.
     * @param pageable Pagination information.
     * @return A page of TimedAuctionSummaryDto objects matching the search criteria.
     */
    Page<TimedAuctionSummaryDto> searchAuctions(
            String query,
            Set<Long> categoryIds,
            AuctionStatus status,
            Boolean ended,
            LocalDateTime from,
            Pageable pageable
    );

}