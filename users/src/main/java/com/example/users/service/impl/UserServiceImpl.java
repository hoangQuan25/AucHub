package com.example.users.service.impl;

import com.example.users.dto.UpdateUserDto;
import com.example.users.dto.UserBasicInfoDto;
import com.example.users.dto.UserDto;
import com.example.users.entity.User;
import com.example.users.exception.UserNotFoundException;
import com.example.users.mapper.UserMapper;
import com.example.users.repository.UserRepository;
import com.example.users.service.KeycloakAdminService;
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

    @Override
    @Transactional
    public UserDto updateUserProfile(String userId, UpdateUserDto updateUserDto) {
        // This method now correctly focuses on user-editable fields (name, phone, address)
        // as payment card display fields were removed from UpdateUserDto.
        log.debug("Updating profile (name, phone, address) for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        userMapper.updateUserFromDto(updateUserDto, user); // Mapper only updates fields present in UpdateUserDto

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);
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