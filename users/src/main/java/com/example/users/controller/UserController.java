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
    private final PaymentServiceClient paymentServiceClient; // Assuming this is a Feign client
    private final SellerReviewService sellerReviewService; // Assuming this is a service for handling reviews
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

    @PutMapping("/me") // This endpoint now handles all profile updates
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

    @PutMapping("/me/avatar") // Or @PostMapping, PUT is common for updates
    public ResponseEntity<UserDto> updateUserAvatar(
            @RequestHeader(USER_ID_HEADER) String userId,
            @RequestBody AvatarUpdateRequestDto avatarUpdateRequest) { // Expecting JSON like {"avatarUrl": "http://..."}
        log.info("PUT /me/avatar request for user ID: {}", userId);
        if (avatarUpdateRequest == null || avatarUpdateRequest.getAvatarUrl() == null || avatarUpdateRequest.getAvatarUrl().trim().isEmpty()) {
            log.warn("Avatar URL is missing in the request for user ID: {}", userId);
            return ResponseEntity.badRequest().build(); // Or a more descriptive error
        }
        // You might want to add URL validation here if necessary
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
        // Simple check: Make sure the authenticated user is not trying to submit a review as someone else.
        // More complex validation (e.g., can this buyerId review this orderId from this sellerId)
        // is handled in the service layer.
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
                                                     @RequestHeader(value = USER_EMAIL_HEADER, required = false) String email, // For creating Stripe Customer
                                                     @RequestHeader(value = USER_USERNAME_HEADER, required = false) String username) { // For creating Stripe Customer
        log.info("User {} requesting SetupIntent secret", userId);
        try {
            UserDto userDto = userService.getOrCreateUserProfile(userId, username, email); // Ensures user exists, gets current stripeCustomerId
            String existingStripeCustomerId = userDto.getStripeCustomerId();
            String userEmailForStripe = userDto.getEmail(); // Get email from profile
            String userNameForStripe = null; // Construct name if available
            if (userDto.getFirstName() != null || userDto.getLastName() != null) {
                userNameForStripe = (userDto.getFirstName() == null ? "" : userDto.getFirstName()) +
                        (userDto.getLastName() == null ? "" : " " + userDto.getLastName()).trim();
            }


            CreateStripeSetupIntentRequestClientDto setupRequest =
                    new CreateStripeSetupIntentRequestClientDto(userId, userEmailForStripe, userNameForStripe, existingStripeCustomerId);

            log.debug("Calling PaymentsService to create SetupIntent for userId: {}, existingStripeCustomerId: {}", userId, existingStripeCustomerId);
            ResponseEntity<CreateStripeSetupIntentResponseClientDto> paymentsResponse =
                    paymentServiceClient.createStripeSetupIntent(setupRequest);

            if (paymentsResponse.getStatusCode().is2xxSuccessful() && paymentsResponse.getBody() != null) {
                CreateStripeSetupIntentResponseClientDto setupResponseData = paymentsResponse.getBody();
                log.info("Successfully received SetupIntent client_secret for user {}. Stripe Customer ID: {}", userId, setupResponseData.getStripeCustomerId());

                // If PaymentsService created/confirmed a Stripe Customer ID and UsersService didn't have it,
                // or if it was newly created by PaymentsService, save it now.
                if (setupResponseData.getStripeCustomerId() != null &&
                        (existingStripeCustomerId == null || !existingStripeCustomerId.equals(setupResponseData.getStripeCustomerId()))) {
                    // We only have stripeCustomerId here, other card details come after confirm-setup
                    // So, we can update only the stripeCustomerId if it's new/changed from PaymentsService response.
                    // The full card details are saved in the confirm-setup step.
                    userService.saveStripePaymentDetails(
                            userId,
                            setupResponseData.getStripeCustomerId(),
                            null, null, null, null, null // No card details yet
                    );
                    log.info("Updated Stripe Customer ID for user {} to {}", userId, setupResponseData.getStripeCustomerId());
                }
                return ResponseEntity.ok(Collections.singletonMap("clientSecret", setupResponseData.getClientSecret()));
            } else {
                log.error("PaymentsService failed to create SetupIntent for user {}. Status: {}, Body: {}",
                        userId, paymentsResponse.getStatusCode(), paymentsResponse.getBody());
                return ResponseEntity.status(paymentsResponse.getStatusCode())
                        .body("Failed to create SetupIntent via PaymentsService.");
            }
        } catch (Exception e) {
            log.error("Error creating SetupIntent secret for user {}: {}", userId, e.getMessage(), e);
            // Check if it's a FeignException to provide more specific error handling
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
            UserDto userDto = userService.getOrCreateUserProfile(userId, username, email); // Get current Stripe Customer ID if any
            String existingStripeCustomerId = userDto.getStripeCustomerId();

            ConfirmStripePaymentMethodRequestClientDto confirmPmRequest =
                    new ConfirmStripePaymentMethodRequestClientDto(userId, confirmationRequest.getStripePaymentMethodId(), existingStripeCustomerId);

            log.debug("Calling PaymentsService to confirm PaymentMethod for userId: {}, pmId: {}, existingStripeCustomerId: {}",
                    userId, confirmationRequest.getStripePaymentMethodId(), existingStripeCustomerId);
            ResponseEntity<StripePaymentMethodDetailsClientDto> paymentsResponse =
                    paymentServiceClient.confirmAndSaveStripePaymentMethod(confirmPmRequest);

            if (paymentsResponse.getStatusCode().is2xxSuccessful() && paymentsResponse.getBody() != null) {
                StripePaymentMethodDetailsClientDto pmDetails = paymentsResponse.getBody();
                log.info("Successfully received payment method details from PaymentsService for user {}. Stripe Customer ID: {}", userId, pmDetails.getStripeCustomerId());

                userService.saveStripePaymentDetails(
                        userId,
                        pmDetails.getStripeCustomerId(),
                        pmDetails.getStripePaymentMethodId(), // This is the default PM ID
                        pmDetails.getCardBrand(),
                        pmDetails.getLast4(),
                        pmDetails.getExpiryMonth(),
                        pmDetails.getExpiryYear()
                );
                return ResponseEntity.ok(Map.of("message", "Payment method saved successfully.",
                        "stripeCustomerId", pmDetails.getStripeCustomerId(),
                        "defaultPaymentMethodId", pmDetails.getStripePaymentMethodId()));
            } else {
                log.error("PaymentsService failed to confirm payment method for user {}. Status: {}, Body: {}",
                        userId, paymentsResponse.getStatusCode(), paymentsResponse.getBody());
                return ResponseEntity.status(paymentsResponse.getStatusCode())
                        .body("Failed to confirm payment method via PaymentsService.");
            }
        } catch (Exception e) {
            log.error("Error confirming payment method setup for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save payment method.");
        }
    }
}