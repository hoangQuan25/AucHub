package com.example.liveauctions.controller;

import com.example.liveauctions.dto.*;
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.service.LiveAuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault; // Use this again
import org.springframework.data.domain.Sort; // Keep Sort import
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// No Mono/Reactor imports needed here

import java.security.Principal; // Keep Principal if needed for direct access (though header is preferred)
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LiveAuctionController {

    private final LiveAuctionService liveAuctionService;
    private static final String USER_ID_HEADER = "X-User-ID";

    @PostMapping("/new-auction")
    public ResponseEntity<LiveAuctionDetailsDto> createAuction(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @Valid @RequestBody CreateLiveAuctionDto createDto) {
        log.info("Received request to create auction from seller: {}", sellerId);
        // Direct call to service method
        LiveAuctionDetailsDto createdAuction = liveAuctionService.createAuction(sellerId, createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAuction);
    }

    @GetMapping("/{auctionId}/details")
    public ResponseEntity<LiveAuctionDetailsDto> getAuctionDetails(
            @PathVariable UUID auctionId) {
        log.info("Received request for details of auction: {}", auctionId);
        // Direct call to service method
        LiveAuctionDetailsDto details = liveAuctionService.getAuctionDetails(auctionId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<Void> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader(USER_ID_HEADER) String bidderId,
            @Valid @RequestBody PlaceBidDto bidDto) {
        log.info("Received bid placement request for auction: {} from bidder: {}", auctionId, bidderId);
        // Direct call to service method
        liveAuctionService.placeBid(auctionId, bidderId, bidDto);
        return ResponseEntity.ok().build(); // Return 200 OK
    }

    // --- REVERTED getActiveAuctions to use Pageable injection ---
    @GetMapping("/live-auctions")
    public ResponseEntity<Page<LiveAuctionSummaryDto>> getActiveAuctions(
            // Use @PageableDefault for default size and sort
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Received request for active auctions list with pagination: {}", pageable);
        // Direct call to service method with automatically resolved Pageable
        Page<LiveAuctionSummaryDto> auctionPage = liveAuctionService.getActiveAuctions(pageable);
        return ResponseEntity.ok(auctionPage);
    }
    // --- END REVERT ---

    // LiveAuctionController.java   (add just after getActiveAuctions)
    @GetMapping("/my-auctions")
    public ResponseEntity<Page<LiveAuctionSummaryDto>> getSellerAuctions(
            @RequestHeader(USER_ID_HEADER) String sellerId,          // comes from Gateway filter
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "categoryIds", required = false) Set<Long> categoryIds,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Seller {} fetching their auctions (status={}, cats={}, from={}, page={})",
                sellerId, status, categoryIds, from, pageable);
        Page<LiveAuctionSummaryDto> page =
                liveAuctionService.getSellerAuctions(sellerId, status, categoryIds, from, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{auctionId}/hammer")
    public ResponseEntity<Void> hammerDown(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @PathVariable UUID auctionId) {
        liveAuctionService.hammerDownNow(auctionId, sellerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{auctionId}/cancel")
    public ResponseEntity<Void> cancel(
            @RequestHeader(USER_ID_HEADER) String sellerId,
            @PathVariable UUID auctionId) {
        liveAuctionService.cancelAuction(auctionId, sellerId);
        return ResponseEntity.ok().build();
    }


}
