// src/main/java/com/example/users/controller/UserController.java
package com.example.users.controller;

import com.example.users.client.PaymentServiceClient;
import com.example.users.client.dto.ConfirmStripePaymentMethodRequestClientDto;
import com.example.users.client.dto.CreateStripeSetupIntentRequestClientDto;
import com.example.users.client.dto.CreateStripeSetupIntentResponseClientDto;
import com.example.users.client.dto.StripePaymentMethodDetailsClientDto;
import com.example.users.dto.*;
import com.example.users.service.SellerReviewService;
import com.example.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // Keep
import org.springframework.web.bind.annotation.*; // Keep

import java.util.Collections;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/") // Keep or change based on Gateway rewrite
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final SellerReviewService sellerReviewService;
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_USERNAME_HEADER = "X-User-Username";

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUserProfile(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestHeader(USER_USERNAME_HEADER) String username,
            @RequestHeader(USER_EMAIL_HEADER) String email) {
        log.info("GET /me request for user ID: {}", userId);
        UserDto userProfile = userService.getOrCreateUserProfile(userId, username, email);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateUserProfile(
            @RequestHeader(USER_ID_HEADER) String userId,
            @Valid @RequestBody UpdateUserDto updateUserDto) { // Use the updated DTO
        log.info("PUT /me request for user ID: {}", userId);
        UserDto updatedUserProfile = userService.updateUserProfile(userId, updateUserDto);
        return ResponseEntity.ok(updatedUserProfile);
    }

    @PostMapping("/me/activate-seller")
    public ResponseEntity<Void> activateSellerRole(
            @RequestHeader(USER_ID_HEADER) String userId) {
        log.info("POST /me/activate-seller request for user ID: {}", userId);
        userService.activateSellerRole(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserDto> updateUserAvatar(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestBody AvatarUpdateRequestDto avatarUpdateRequest) { // Expecting JSON like {"avatarUrl": "http://..."}
        log.info("PUT /me/avatar request for user ID: {}", userId);
        if (avatarUpdateRequest == null || avatarUpdateRequest.getAvatarUrl() == null || avatarUpdateRequest.getAvatarUrl().trim().isEmpty()) {
            log.warn("Avatar URL is missing in the request for user ID: {}", userId);
            return ResponseEntity.badRequest().build(); // Or a more descriptive error
        }
        UserDto updatedUserProfile = userService.updateAvatarUrl(userId, avatarUpdateRequest.getAvatarUrl());
        return ResponseEntity.ok(updatedUserProfile);
    }

    @GetMapping("/sellers/{identifier}") // Using "identifier" to accept ID or username
    public ResponseEntity<PublicSellerProfileDto> getPublicSellerProfile(
            @PathVariable("identifier") String identifier) {
        log.info("GET /sellers/{} public profile request", identifier);
        PublicSellerProfileDto sellerProfile = userService.getPublicSellerProfile(identifier);
        return ResponseEntity.ok(sellerProfile);
    }

    @PostMapping("/submit-review") // Changed to a more RESTful name
    public ResponseEntity<ReviewDto> createReview(
            @RequestHeader(USER_ID_HEADER) String buyerId, // Get buyer ID from header (set by API Gateway/Auth service)
            @Valid @RequestBody CreateReviewDto createReviewDto) {
        log.info("POST /reviews request from buyer ID: {}", buyerId);
        ReviewDto createdReview = sellerReviewService.createReview(createReviewDto, buyerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ReviewDto>> getReviewsForSeller(
            @PathVariable String sellerId,
            @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable) {
        log.info("GET /reviews/seller/{} request", sellerId);
        Page<ReviewDto> reviews = sellerReviewService.getReviewsForSeller(sellerId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/batch")
    public ResponseEntity<Map<String, UserBasicInfoDto>> getUsersBasicInfoByIds(
            @RequestParam("ids") List<String> userIds) { // Pass IDs as query parameter
        log.info("GET /api/v1/users/batch request for IDs: {}", userIds);
        if (userIds == null || userIds.isEmpty() || userIds.size() > 100) { // Add size limit
            return ResponseEntity.badRequest().build();
        }
        Map<String, UserBasicInfoDto> usersInfo = userService.getUsersBasicInfo(userIds); // Implement this in UserService
        return ResponseEntity.ok(usersInfo);
    }

    @GetMapping("/{userId}/ban-status")
    public ResponseEntity<UserBanStatusDto> getBanStatus(@PathVariable String userId) {
        log.debug("Checking ban status for user ID: {}", userId);
        UserBanStatusDto banStatus = userService.getUserBanStatus(userId);
        return ResponseEntity.ok(banStatus);
    }

    @PostMapping("/me/payment-method/setup-intent-secret")
    public ResponseEntity<?> createSetupIntentSecret(@RequestHeader(USER_ID_HEADER) String userId,
                                                     @RequestHeader(value = USER_EMAIL_HEADER, required = false) String email,
                                                     @RequestHeader(value = USER_USERNAME_HEADER, required = false) String username) {
        log.info("User {} requesting SetupIntent secret", userId);
        try {
            StripeSetupIntentSecretDto result = userService.createSetupIntentSecret(userId, email, username);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating SetupIntent secret for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create SetupIntent.");
        }
    }

    @PostMapping("/me/payment-method/confirm-setup")
    public ResponseEntity<?> confirmPaymentMethodSetup(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestHeader(value = USER_EMAIL_HEADER, required = false) String email,
            @RequestHeader(value = USER_USERNAME_HEADER, required = false) String username,
            @Valid @RequestBody StripeSetupConfirmationRequestDto confirmationRequest) {
        log.info("User {} confirming payment method setup with PaymentMethod ID: {}", userId, confirmationRequest.getStripePaymentMethodId());
        try {
            StripePaymentMethodConfirmationResultDto result = userService.confirmPaymentMethodSetup(userId, email, username, confirmationRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error confirming payment method setup for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save payment method.");
        }
    }
}