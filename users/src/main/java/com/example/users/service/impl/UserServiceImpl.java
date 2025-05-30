package com.example.users.service.impl;

import com.example.users.dto.*;
import com.example.users.dto.event.UserBannedEventDto;
import com.example.users.entity.User;
import com.example.users.exception.ResourceNotFoundException;
import com.example.users.exception.UserNotFoundException;
import com.example.users.mapper.UserMapper;
import com.example.users.repository.UserRepository;
import com.example.users.service.KeycloakAdminService;
import com.example.users.service.SellerReviewService;
import com.example.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation; // Keep this if getOrCreateUserProfile uses it
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils; // Keep this

import java.time.LocalDateTime;
import java.util.Collections; // Keep this
import java.util.List;       // Keep this
import java.util.Map;        // Keep this
import java.util.UUID;
import java.util.stream.Collectors; // Keep this


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminService keycloakAdminService;
    private final SellerReviewService sellerReviewService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public UserDto getOrCreateUserProfile(String userId, String username, String email) {
        // ... (existing implementation is fine - new User() will have null Stripe fields initially)
        log.info("Attempting to fetch or create profile for user ID: {}", userId);
        return userRepository.findById(userId)
                .map(existingUser -> {
                    log.info("Found existing user profile for ID: {}", userId);
                    return userMapper.toUserDto(existingUser);
                })
                .orElseGet(() -> {
                    log.info("User profile not found locally for ID: {}. Fetching from Keycloak and creating.", userId);
                    try {
                        UserRepresentation keycloakUserRep = keycloakAdminService.getKeycloakUserById(userId); // Renamed for clarity
                        User newUser = new User();
                        newUser.setId(keycloakUserRep.getId());
                        newUser.setUsername(keycloakUserRep.getUsername());
                        newUser.setEmail(keycloakUserRep.getEmail());
                        newUser.setFirstName(keycloakUserRep.getFirstName());
                        newUser.setLastName(keycloakUserRep.getLastName());
                        // Address, Stripe, and Payment display fields will be null by default
                        newUser.setSeller(false); // Default seller status

                        User savedUser = userRepository.save(newUser);
                        log.info("Successfully created and saved new user profile for ID: {}", userId);
                        return userMapper.toUserDto(savedUser);
                    } catch (Exception e) {
                        log.error("Failed to create user profile for ID {} after fetching from Keycloak: {}", userId, e.getMessage(), e);
                        throw new RuntimeException("Failed to create user profile for ID: " + userId, e);
                    }
                });
    }

    // com.example.users.service.impl.UserServiceImpl.java
    @Override
    @Transactional
    public UserDto updateUserProfile(String userId, UpdateUserDto updateUserDto) {
        log.debug("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        userMapper.updateUserFromDto(updateUserDto, user); // This will map firstName, lastName, address, etc.

        // Explicitly handle sellerDescription
        if (user.isSeller()) {

            if (updateUserDto.getSellerDescription() != null) { // Check if the field was provided in the request
                user.setSellerDescription(updateUserDto.getSellerDescription());
            }
        } else {

            if (updateUserDto.getSellerDescription() != null) {
                log.warn("Attempt to set seller description for non-seller user ID: {}. Ignoring.", userId);
            }
            user.setSellerDescription(null); // Ensure it's cleared if they are not a seller
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);
        return userMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public UserDto updateAvatarUrl(String userId, String avatarUrl) {
        log.info("Updating avatar URL for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId + " while updating avatar."));

        user.setAvatarUrl(avatarUrl);
        User updatedUser = userRepository.save(user);
        log.info("Avatar URL updated successfully for user ID: {}", userId);
        return userMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void activateSellerRole(String userId) {
        // ... (existing implementation is fine) ...
        log.debug("Activating seller role for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.isSeller()) {
            log.warn("User ID: {} is already a seller. No action taken.", userId);
            return;
        }
        keycloakAdminService.addSellerRoleToUser(userId);
        user.setSeller(true);
        userRepository.save(user);
        log.info("Internal seller status updated successfully for user ID: {}", userId);
    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, UserBasicInfoDto> getUsersBasicInfo(List<String> userIds) {
        // ... (existing implementation is fine, UserMapper now maps more fields to UserBasicInfoDto) ...
        if (CollectionUtils.isEmpty(userIds)) {
            log.debug("Received empty list of user IDs for basic info lookup.");
            return Collections.emptyMap();
        }
        log.info("Fetching basic info for {} user IDs.", userIds.size());
        List<User> users = userRepository.findAllById(userIds);

        if (users.isEmpty()) {
            log.warn("No users found in DB for provided IDs: {}", userIds);
            return Collections.emptyMap();
        }
        Map<String, UserBasicInfoDto> resultMap = users.stream()
                .map(userMapper::toUserBasicInfoDto)
                .filter(dto -> dto != null && dto.getId() != null)
                .collect(Collectors.toMap(
                        UserBasicInfoDto::getId,
                        dto -> dto
                ));
        log.debug("Returning basic info map for {} users.", resultMap.size());
        return resultMap;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicSellerProfileDto getPublicSellerProfile(String userIdOrUsername) {
        log.debug("Fetching public seller profile for identifier: {}", userIdOrUsername);
        User user = userRepository.findByUsername(userIdOrUsername)
                .orElseGet(() -> userRepository.findById(userIdOrUsername)
                        .orElseThrow(() -> new ResourceNotFoundException("Seller not found with identifier: " + userIdOrUsername)));

        if (!user.isSeller()) {
            throw new ResourceNotFoundException("User " + userIdOrUsername + " is not a registered seller.");
        }

        PublicSellerProfileDto dto = userMapper.toPublicSellerProfileDto(user);

        // Fetch and set average rating and review count
        Double avgRating = sellerReviewService.getAverageRatingForSeller(user.getId());
        Long revCount = sellerReviewService.getReviewCountForSeller(user.getId());

        dto.setAverageRating(avgRating); // Will be null if no reviews
        dto.setReviewCount(revCount != null ? revCount : 0L);


        return dto;
    }


    @Override
    @Transactional
    public void saveStripePaymentDetails(String userId, String stripeCustomerId, String stripeDefaultPaymentMethodId,
                                         String cardBrand, String last4, String expMonth, String expYear) {
        log.info("Saving Stripe payment details for user ID: {}. Stripe Customer ID: {}, PM ID: {}",
                userId, stripeCustomerId, stripeDefaultPaymentMethodId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId + " while trying to save Stripe details."));

        user.setStripeCustomerId(stripeCustomerId);
        user.setStripeDefaultPaymentMethodId(stripeDefaultPaymentMethodId);
        user.setDefaultCardBrand(cardBrand);
        user.setDefaultCardLast4(last4);
        user.setDefaultCardExpiryMonth(expMonth);
        user.setDefaultCardExpiryYear(expYear);

        userRepository.save(user);
        log.info("Successfully saved Stripe payment details for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void processFirstWinnerPaymentDefault(String userId) {
        log.info("Processing first winner payment default for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for payment default processing: {}", userId);
                    // If user doesn't exist, they can't have defaulted.
                    // This could be a sign of an issue, but for now, we can't ban a non-existent user.
                    return new UserNotFoundException("User not found: " + userId);
                });

        // Check if currently banned
        if (user.getBanEndsAt() != null && LocalDateTime.now().isBefore(user.getBanEndsAt())) {
            log.info("User {} is currently banned until {}. Default still counted.", userId, user.getBanEndsAt());
            // Policy: Current ban remains, default is still counted. No immediate re-banning while already banned.
        }

        // Has a previous ban period ended?
        boolean previousBanExpired = user.getCurrentBanLevel() > 0 &&
                (user.getBanEndsAt() == null || LocalDateTime.now().isAfter(user.getBanEndsAt()));

        user.setFirstWinnerPaymentDefaultCount(user.getFirstWinnerPaymentDefaultCount() + 1);
        log.info("User {} default count incremented to: {}", userId, user.getFirstWinnerPaymentDefaultCount());

        boolean newBanApplied = false;
        String banDurationMessage = "";

        if (previousBanExpired) { // Defaulted *after* a previous ban ended.
            log.info("User {} defaulted after a previous ban period expired (current ban level {}). Applying 1-month ban.", userId, user.getCurrentBanLevel());
            user.setBanEndsAt(LocalDateTime.now().plusMonths(1));
            user.setCurrentBanLevel(Math.max(user.getCurrentBanLevel(), 2)); // Escalate to level 2 (month) or keep if already higher
            newBanApplied = true;
            banDurationMessage = "1 month";
        } else if (user.getCurrentBanLevel() == 0 && user.getFirstWinnerPaymentDefaultCount() >= 3) {
            // First time reaching 3 defaults, and no current or past ban level recorded.
            log.info("User {} reached {} defaults (no prior ban level). Applying 1-week ban.", userId, user.getFirstWinnerPaymentDefaultCount());
            user.setBanEndsAt(LocalDateTime.now().plusWeeks(1));
            user.setCurrentBanLevel(1); // Set to level 1 (week)
            newBanApplied = true;
            banDurationMessage = "1 week";
        }

        User savedUser = userRepository.save(user);

        if (newBanApplied) {
            log.info("User {} has been banned for {} until {}. Ban level: {}. Total defaults: {}",
                    userId, banDurationMessage, savedUser.getBanEndsAt(), savedUser.getCurrentBanLevel(), savedUser.getFirstWinnerPaymentDefaultCount());

            // Publish UserBannedEvent
            UserBannedEventDto bannedEvent = UserBannedEventDto.builder()
                    .eventId(UUID.randomUUID())
                    .eventTimestamp(LocalDateTime.now())
                    .userId(savedUser.getId())
                    .banEndsAt(savedUser.getBanEndsAt())
                    .banLevel(savedUser.getCurrentBanLevel())
                    .totalDefaults(savedUser.getFirstWinnerPaymentDefaultCount())
                    .build();
            try {
                // Use constants defined in UsersService's RabbitMqConfig
                rabbitTemplate.convertAndSend(
                        com.example.users.config.RabbitMqConfig.USER_EVENTS_EXCHANGE, // Ensure this constant exists
                        com.example.users.config.RabbitMqConfig.USER_EVENT_BANNED_ROUTING_KEY, // Ensure this constant exists
                        bannedEvent);
                log.info("Published UserBannedEvent for user {}", savedUser.getId());
            } catch (Exception e) {
                log.error("Error publishing UserBannedEvent for user {}: {}", savedUser.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserBanStatusDto getUserBanStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        boolean isBanned = user.getBanEndsAt() != null && LocalDateTime.now().isBefore(user.getBanEndsAt());
        return new UserBanStatusDto(isBanned, isBanned ? user.getBanEndsAt() : null);
    }
}