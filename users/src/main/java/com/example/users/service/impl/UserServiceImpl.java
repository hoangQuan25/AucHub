package com.example.users.service.impl;

import com.example.users.dto.PublicSellerProfileDto;
import com.example.users.dto.UpdateUserDto;
import com.example.users.dto.UserBasicInfoDto;
import com.example.users.dto.UserDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils; // Keep this

import java.util.Collections; // Keep this
import java.util.List;       // Keep this
import java.util.Map;        // Keep this
import java.util.stream.Collectors; // Keep this


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminService keycloakAdminService;
    private final SellerReviewService sellerReviewService; // Assuming this is needed for public profile

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


    // --- NEW METHOD IMPLEMENTATION ---
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
}