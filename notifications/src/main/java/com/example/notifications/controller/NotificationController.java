package com.example.notifications.controller;

import com.example.notifications.dto.FollowingAuctionSummaryDto;
import com.example.notifications.dto.NotificationDto; // DTO for response
import com.example.notifications.dto.PagedResultDto;
import com.example.notifications.entity.AuctionStatus;
import com.example.notifications.service.NotificationService; // Service Interface
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    // Assuming Gateway provides this header after authentication
    private static final String USER_ID_HEADER = "X-User-ID";

    @GetMapping("/my-notifications")
    public ResponseEntity<Page<NotificationDto>> getUserNotifications(
            @RequestHeader(USER_ID_HEADER) String userId,
            // Default pagination: 15 items per page, sort by creation date descending
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Fetching notifications for user {} with pagination {}", userId, pageable);
        // Add a method to the service interface & implementation to handle this
        Page<NotificationDto> notificationPage = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notificationPage);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(USER_ID_HEADER) String userId
    ) {
        log.debug("Fetching unread count for user {}", userId);
        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(Collections.singletonMap("unreadCount", count));
    }

    @PostMapping("/mark-read") // Mark specific notifications as read
    public ResponseEntity<Void> markNotificationsAsRead(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestBody List<Long> notificationIds // Expect a JSON array of IDs
    ) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            // Or return OK if empty list is not an error
            return ResponseEntity.badRequest().build();
        }
        log.info("Request from user {} to mark notifications as read: {}", userId, notificationIds);
        notificationService.markNotificationsAsRead(userId, notificationIds);
        return ResponseEntity.ok().build(); // Return 200 OK
    }

    @PostMapping("/mark-all-read") // Mark all for the user as read
    public ResponseEntity<Void> markAllNotificationsAsRead(
            @RequestHeader(USER_ID_HEADER) String userId
    ) {
        log.info("Request from user {} to mark all notifications as read", userId);
        notificationService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok().build(); // Return 200 OK
    }

    @GetMapping("/following/ids") // Get IDs of auctions user follows
    public ResponseEntity<Set<UUID>> getFollowedAuctionIds(
            @RequestHeader(USER_ID_HEADER) String userId
    ) {
        log.debug("Request received for followed auction IDs for user {}", userId);
        Set<UUID> ids = notificationService.getFollowedAuctionIds(userId);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/following-auctions") // GET /api/following-auctions
    public ResponseEntity<PagedResultDto<FollowingAuctionSummaryDto>> getFollowingAuctions(
            @RequestHeader(USER_ID_HEADER) String userId,
            // Filters (mirroring MyAuctionsPage)
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "ended", required = false) Boolean ended,
            @RequestParam(value = "categoryIds", required = false) Set<Long> categoryIds,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            // Pagination
            @PageableDefault(size = 12, sort = "endTime", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("CONTROLLER Fetching followed auctions for user {}: status={}, ended={}, cats={}, from={}, page={}",
                userId, status, ended, categoryIds, from, pageable);

        // Input validation: Cannot provide both status and ended=true
        if (Boolean.TRUE.equals(ended) && status != null) {
            log.warn("Both 'status' and 'ended=true' provided for following auctions, ignoring 'status'.");
            status = null;
        }

        Page<FollowingAuctionSummaryDto> pageResult = notificationService.getFollowingAuctions(
                userId, status, ended, categoryIds, from, pageable
        );
        PagedResultDto<FollowingAuctionSummaryDto> responseDto = PagedResultDto.fromPage(pageResult);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/follow/{auctionType}/{auctionId}")
    public ResponseEntity<Void> followAuction(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable String auctionType, // LIVE or TIMED
            @PathVariable UUID auctionId
    ) {
        // Basic validation for auctionType
        if (!"LIVE".equalsIgnoreCase(auctionType) && !"TIMED".equalsIgnoreCase(auctionType)) {
            log.warn("Invalid auctionType received for follow request: {}", auctionType);
            return ResponseEntity.badRequest().build();
        }
        log.info("Request from user {} to follow {} auction {}", userId, auctionType.toUpperCase(), auctionId);
        notificationService.followAuction(userId, auctionId, auctionType.toUpperCase());
        return ResponseEntity.ok().build(); // 200 OK indicates request accepted
    }

    @DeleteMapping("/follow/{auctionId}") // Unfollow just needs auctionId
    public ResponseEntity<Void> unfollowAuction(
            @RequestHeader(USER_ID_HEADER) String userId,
            @PathVariable UUID auctionId
    ) {
        log.info("Request from user {} to unfollow auction {}", userId, auctionId);
        notificationService.unfollowAuction(userId, auctionId);
        return ResponseEntity.noContent().build(); // 204 No Content is appropriate for DELETE success
    }

}