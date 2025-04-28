package com.example.liveauctions.controller; // Or your controller package

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // To access authenticated user info
import java.util.Map;

/**
 * Simple controller for testing basic connectivity and authentication
 * to the LiveAuctions service via the API Gateway.
 */
@RestController
@RequestMapping("/test") // Base path for test endpoints
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private static final String USER_ID_HEADER = "X-User-ID"; // Standard header name from Gateway filter

    /**
     * Simple GET endpoint, requires no authentication (should be permitAll in Gateway).
     * Checks if the service is reachable.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("GET /api/liveauctions/test/ping received");
        return ResponseEntity.ok("Pong from LiveAuctions Service!");
    }

    /**
     * Simple POST endpoint that requires authentication (e.g., SELLER role in Gateway).
     * Echoes back the request body and confirms the authenticated user ID.
     *
     * @param requestBody The JSON body sent in the request.
     * @param principal   The authenticated principal (injected by Spring Security).
     * @param userIdHeader The User ID header added by the Gateway filter.
     * @return A confirmation message including the user ID and the echoed body.
     */
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echoPost(
            @RequestBody(required = false) Map<String, Object> requestBody, // Make body optional for testing
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader, // Get header from Gateway
            Principal principal // Get authenticated user from SecurityContext
    ) {
        log.info("POST /api/liveauctions/test/echo received");

        String authenticatedUserId = (principal != null) ? principal.getName() : "N/A (Unauthenticated?)";
        String sourceUserId = (userIdHeader != null) ? userIdHeader : "N/A (Header Missing?)";

        log.info("Authenticated Principal User ID: {}", authenticatedUserId);
        log.info("User ID from Header: {}", sourceUserId);

        if (principal == null || userIdHeader == null) {
            log.warn("Request to /echo received without full authentication details.");
            // Decide how to handle - maybe return 401/403 if expected?
            // For testing, let's just report it.
        }

        Map<String, Object> response = Map.of(
                "message", "Echo from LiveAuctions Service!",
                "authenticatedUserId", authenticatedUserId,
                "userIdFromHeader", sourceUserId,
                "receivedBody", requestBody != null ? requestBody : "No body received"
        );
        return ResponseEntity.ok(response);
    }
}
