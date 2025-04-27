package com.example.liveauctions.controller;

import com.example.liveauctions.dto.*;
import com.example.liveauctions.service.LiveAuctionService; // To be created
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault; // For default pagination
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LiveAuctionController {

    private final LiveAuctionService liveAuctionService; // Inject the service
    private static final String USER_ID_HEADER = "X-User-ID"; // Standard header name

    // Endpoint to Create a New Auction
    // Gateway enforces SELLER role
    @PostMapping("/new-auction")
    public ResponseEntity<LiveAuctionDetailsDto> createAuction(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody CreateLiveAuctionDto createDto) {
        log.info("Received request to create auction from seller: {}", sellerId);
        // Delegate to service, passing sellerId and DTO
        LiveAuctionDetailsDto createdAuction = liveAuctionService.createAuction(sellerId, createDto);
        // Return 201 Created status with details of the created auction
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAuction);
    }

    // Endpoint to Get Details of a Specific Auction
    // Gateway enforces authenticated() - TBC if needs stricter role? Likely not.
    @GetMapping("/{auctionId}/details")
    public ResponseEntity<LiveAuctionDetailsDto> getAuctionDetails(
            @PathVariable UUID auctionId) {
        log.info("Received request for details of auction: {}", auctionId);
        LiveAuctionDetailsDto details = liveAuctionService.getAuctionDetails(auctionId);
        return ResponseEntity.ok(details); // Return 200 OK with details
    }

    // Endpoint for a User to Place a Bid on an Auction
    // Gateway enforces authenticated()
    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<Void> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String bidderId,
            @Valid @RequestBody PlaceBidDto bidDto) {
        log.info("Received bid placement request for auction: {} from bidder: {}", auctionId, bidderId);
        liveAuctionService.placeBid(auctionId, bidderId, bidDto);
        // Return 200 OK on successful validation and initial processing.
        // Actual state update confirmation happens via WebSocket.
        return ResponseEntity.ok().build();
    }

    // Endpoint to Get a List of Active (or relevant) Auctions
    // Gateway enforces permitAll() - TBC
    @GetMapping
    public ResponseEntity<Page<LiveAuctionSummaryDto>> getActiveAuctions(
            @PageableDefault(size = 12, sort = "endTime") Pageable pageable) { // Default page size 12, sort by end time
        log.info("Received request for active auctions list with pagination: {}", pageable);
        Page<LiveAuctionSummaryDto> auctionPage = liveAuctionService.getActiveAuctions(pageable);
        return ResponseEntity.ok(auctionPage);
    }

    // --- Other Potential Endpoints (Consider later) ---
    // GET /my-bids (Get auctions the user has bid on)
    // GET /my-wins (Get auctions the user has won)
    // POST /{auctionId}/cancel (If sellers/admins can cancel)
    // ... etc.

}