package com.example.timedauctions.controller;

import com.example.timedauctions.dto.*;
import com.example.timedauctions.entity.AuctionStatus;
import com.example.timedauctions.service.TimedAuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TimedAuctionController {

    private final TimedAuctionService timedAuctionService;
    private static final String USER_ID_HEADER = "X-User-ID"; // Assume Gateway provides this

    @PostMapping("/timed-auctions") // Maps to POST /api/timed-auctions
    public ResponseEntity<TimedAuctionDetailsDto> createAuction(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody CreateTimedAuctionDto createDto) {
        log.info("Received request to create timed auction from seller: {}", sellerId);
        TimedAuctionDetailsDto createdAuction = timedAuctionService.createAuction(sellerId, createDto);
        // Return 201 Created status
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAuction);
    }

    @GetMapping("/timed-auctions")
    public ResponseEntity<Page<TimedAuctionSummaryDto>> getActiveTimedAuctions(
            @RequestParam(value = "status", defaultValue = "ACTIVE") AuctionStatus status,
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("Fetching timed auctions with status {} and pagination: {}", status, pageable);
        Page<TimedAuctionSummaryDto> auctionPage;
        if (status == AuctionStatus.ACTIVE) {
            auctionPage = timedAuctionService.getActiveAuctions(pageable);
        } else {
            log.warn("Fetching non-ACTIVE status ({}) via /api/timedauctions/timed-auctions endpoint. Consider dedicated endpoints or refining service logic.", status);
            auctionPage = Page.empty(pageable);
        }
        return ResponseEntity.ok(auctionPage);
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<TimedAuctionDetailsDto> getAuctionDetails(
            @PathVariable UUID auctionId) {
        log.info("Received request for details of timed auction: {}", auctionId);
        TimedAuctionDetailsDto details = timedAuctionService.getAuctionDetails(auctionId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{auctionId}/bids") // Maps to POST /api/timed-auctions/{auctionId}/bids
    public ResponseEntity<Void> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String bidderId,
            @Valid @RequestBody PlaceMaxBidDto bidDto) {
        log.info("Received max bid placement request for auction: {} from bidder: {}", auctionId, bidderId);
        timedAuctionService.placeMaxBid(auctionId, bidderId, bidDto);
        // Return 200 OK on success (or potentially return updated auction state?)
        // For now, just return OK. Client will poll for updates.
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{auctionId}/comments") // Maps to POST /api/timed-auctions/{auctionId}/comments
    public ResponseEntity<CommentDto> createComment(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody CreateCommentDto commentDto) {
        log.info("User {} posting comment for auction {}", userId, auctionId);
        CommentDto createdComment = timedAuctionService.createComment(auctionId, userId, commentDto);
        // Return 201 Created status
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @GetMapping("/{auctionId}/comments") // Maps to GET /api/timed-auctions/{auctionId}/comments
    public ResponseEntity<List<CommentDto>> getComments(
            @PathVariable UUID auctionId) {
        log.info("Fetching comments for auction {}", auctionId);
        List<CommentDto> comments = timedAuctionService.getComments(auctionId);
        return ResponseEntity.ok(comments);
    }
}