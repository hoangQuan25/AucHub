package com.example.notifications.controller;

import com.example.notifications.dto.NotificationDto; // DTO for response
import com.example.notifications.service.NotificationService; // Service Interface
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

}