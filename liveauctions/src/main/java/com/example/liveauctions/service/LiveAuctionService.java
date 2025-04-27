package com.example.liveauctions.service;

import com.example.liveauctions.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LiveAuctionService {

    /**
     * Creates a new live auction based on the provided details.
     * Fetches necessary product and user details via Feign.
     * Schedules the auction if startTime is in the future.
     *
     * @param sellerId   The ID of the user creating the auction.
     * @param createDto  DTO containing auction creation parameters.
     * @return LiveAuctionDetailsDto of the newly created auction.
     */
    LiveAuctionDetailsDto createAuction(String sellerId, CreateLiveAuctionDto createDto);

    /**
     * Retrieves the full details of a specific auction, enriching with data from other services.
     *
     * @param auctionId The UUID of the auction to retrieve.
     * @return LiveAuctionDetailsDto containing auction details.
     * @throws AuctionNotFoundException if auction with the given ID doesn't exist.
     */
    LiveAuctionDetailsDto getAuctionDetails(UUID auctionId);

    /**
     * Places a bid on an active auction.
     * Validates the bid amount, auction status, and bidder.
     * Updates auction state and persists changes.
     * Handles concurrency control (locking).
     * Triggers state update broadcast (e.g., via event publishing).
     *
     * @param auctionId The UUID of the auction being bid on.
     * @param bidderId  The ID of the user placing the bid.
     * @param bidDto    DTO containing the bid amount.
     * @throws AuctionNotFoundException if auction doesn't exist.
     * @throws InvalidAuctionStateException if auction is not ACTIVE or has ended.
     * @throws InvalidBidException if the bid is invalid (e.g., too low, bidder is seller/highest).
     */
    void placeBid(UUID auctionId, String bidderId, PlaceBidDto bidDto);

    /**
     * Retrieves a paginated list of currently active (or relevant) auctions.
     *
     * @param pageable Pagination and sorting information.
     * @return A Page of LiveAuctionSummaryDto.
     */
    Page<LiveAuctionSummaryDto> getActiveAuctions(Pageable pageable);

    // We'll need internal methods for starting/ending auctions, calculating increments, etc.
    // And potentially methods called by scheduled tasks or message listeners.
}