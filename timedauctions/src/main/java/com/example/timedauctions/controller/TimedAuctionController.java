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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TimedAuctionController {

    private final TimedAuctionService timedAuctionService;
    private static final String USER_ID_HEADER = "X-User-ID";

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

    @GetMapping("/my-auctions") // Maps to GET /api/timed-auctions/my-auctions
    public ResponseEntity<Page<TimedAuctionSummaryDto>> getSellerAuctions(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            // Status filter (optional)
            @RequestParam(value = "status", required = false) AuctionStatus status,
            // Special 'ended' flag (optional)
            @RequestParam(value = "ended", required = false) Boolean ended,
            // Category filter (optional)
            @RequestParam(value = "categoryIds", required = false) Set<Long> categoryIds,
            // Time filter (optional)
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            // Pagination and default sort (e.g., end time descending for past auctions)
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        log.info("Seller {} fetching their TIMED auctions (status={}, ended={}, cats={}, from={}, page={})",
                sellerId, status, ended, categoryIds, from, pageable);

        // Input validation: Cannot provide both status and ended=true
        if (Boolean.TRUE.equals(ended) && status != null) {
            // Consider throwing a BadRequestException or ignoring 'status'
            log.warn("Both 'status' and 'ended=true' provided for seller auctions, ignoring 'status'.");
            status = null;
        }


        Page<TimedAuctionSummaryDto> page = timedAuctionService.getSellerAuctions(
                sellerId, status, ended, categoryIds, from, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/seller/{sellerId}/timed-auctions") // e.g., /api/timed-auctions/seller/{sellerId}
    public ResponseEntity<Page<TimedAuctionSummaryDto>> getPublicTimedAuctionsBySeller(
            @PathVariable String sellerId, // Seller's public ID or username (if service supports resolving username)
            // Re-use existing filter parameters
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "ended", required = false) Boolean ended,
            @RequestParam(value = "categoryIds", required = false) Set<Long> categoryIds,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly, // New param to easily get active/scheduled
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Fetching public TIMED auctions for sellerId: {} (status={}, ended={}, cats={}, from={}, activeOnly={}, page={})",
                sellerId, status, ended, categoryIds, from, activeOnly, pageable);

        // Refine filtering logic for public view if needed
        AuctionStatus effectiveStatus = status;
        Boolean effectiveEnded = ended;

        if (activeOnly) { // If client requests activeOnly, override other status/ended filters
            effectiveStatus = AuctionStatus.ACTIVE; // Or combine ACTIVE and SCHEDULED if that's what "activeOnly" means
            effectiveEnded = false; // Explicitly not ended
        } else {
            if (Boolean.TRUE.equals(effectiveEnded) && effectiveStatus != null) {
                log.warn("Both 'status' and 'ended=true' provided for public seller auctions, ignoring 'status'.");
                effectiveStatus = null;
            }
        }

        Page<TimedAuctionSummaryDto> page = timedAuctionService.getSellerAuctions(
                sellerId,
                effectiveStatus,
                effectiveEnded,
                categoryIds,
                from,
                pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/search") // Maps to GET /api/timed-auctions/search (via Gateway)
    public ResponseEntity<Page<TimedAuctionSummaryDto>> searchTimedAuctions(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "categoryIds", required = false) Set<Long> categoryIds,
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "ended", required = false) Boolean ended, // To specifically get ended auctions
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @PageableDefault(size = 12, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable // Default sort, adjust as needed
    ) {
        log.info("Searching timed auctions with query='{}', categories={}, status={}, ended={}, from={}, page={}",
                query, categoryIds, status, ended, from, pageable);

        Page<TimedAuctionSummaryDto> auctionPage = timedAuctionService.searchAuctions(
                query, categoryIds, status, ended, from, pageable
        );
        return ResponseEntity.ok(auctionPage);
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<TimedAuctionDetailsDto> getAuctionDetails(
            @PathVariable UUID auctionId) {
        log.info("Received request for details of timed auction: {}", auctionId);
        TimedAuctionDetailsDto details = timedAuctionService.getAuctionDetails(auctionId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/{auctionId}/my-max-bid")
    public ResponseEntity<MyMaxBidDto> getMyMaxBid(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String bidderId) { // Authenticated user's ID
        log.info("Received request for user {}'s max bid on auction {}", bidderId, auctionId);
        MyMaxBidDto myMaxBidDto = timedAuctionService.getMyMaxBidForAuction(auctionId, bidderId);
        return ResponseEntity.ok(myMaxBidDto);
    }

    @PostMapping("/{auctionId}/cancel") // Maps to POST /api/timed-auctions/{auctionId}/cancel
    public ResponseEntity<Void> cancelAuction(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String sellerId) {
        log.info("User {} requesting cancellation for auction {}", sellerId, auctionId);
        timedAuctionService.cancelAuction(auctionId, sellerId);
        // Return 200 OK, actual change happens asynchronously
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{auctionId}/hammer") // Maps to POST /api/timed-auctions/{auctionId}/hammer
    public ResponseEntity<Void> endAuctionEarly( // Renamed to match service
                                                 @PathVariable UUID auctionId,
                                                 @RequestHeader(USER_ID_HEADER) String sellerId) {
        log.info("User {} requesting early end (hammer) for auction {}", sellerId, auctionId);
        timedAuctionService.endAuctionEarly(auctionId, sellerId);
        // Return 200 OK, actual change happens asynchronously
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{auctionId}/bids") // Maps to POST /api/timed-auctions/{auctionId}/bids
    public ResponseEntity<Void> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String bidderId,
            @Valid @RequestBody PlaceMaxBidDto bidDto) {
        log.info("Received max bid placement request for auction: {} from bidder: {}", auctionId, bidderId);
        timedAuctionService.placeMaxBid(auctionId, bidderId, bidDto);
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

    @GetMapping("/batch-summary")
    public ResponseEntity<List<TimedAuctionSummaryDto>> getAuctionSummariesByIds(
            @RequestParam("ids") Set<UUID> auctionIds
    ) {
        log.info("Request received for timed auction summaries by IDs: {}", auctionIds);
        if (auctionIds == null || auctionIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<TimedAuctionSummaryDto> summaries = timedAuctionService.getAuctionSummariesByIds(auctionIds);
        return ResponseEntity.ok(summaries);
    }

    @PutMapping("/{auctionId}/comments/{commentId}")
    public ResponseEntity<CommentDto> editComment(
            @PathVariable UUID auctionId,
            @PathVariable Long commentId,
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody UpdateCommentDto updateDto) {
        log.info("User {} requesting to edit comment {} for auction {}", userId, commentId, auctionId);
        CommentDto updatedComment = timedAuctionService.editComment(auctionId, commentId, userId, updateDto);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{auctionId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID auctionId,
            @PathVariable Long commentId,
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("User {} requesting to delete comment {} for auction {}", userId, commentId, auctionId);
        timedAuctionService.deleteComment(auctionId, commentId, userId);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content là chuẩn cho việc xóa thành công
    }
}