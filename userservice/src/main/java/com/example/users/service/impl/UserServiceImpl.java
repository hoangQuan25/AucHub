// src/main/java/com/example/users/service/impl/UserServiceImpl.java
package com.example.users.service.impl;

// REMOVE imports for Address/Payment entities, DTOs, mappers, repos, List
import com.example.users.dto.UpdateUserDto;
import com.example.users.dto.UserDto;
import com.example.users.entity.User;
import com.example.users.exception.UserNotFoundException;
import com.example.users.mapper.UserMapper;
import com.example.users.repository.UserRepository;
import com.example.users.service.KeycloakAdminService;
import com.example.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakAdminService keycloakAdminService;
    // REMOVE AddressRepository, PaymentMethodRepository, AddressMapper, PaymentMethodMapper injections

    @Override
    @Transactional
    public UserDto getOrCreateUserProfile(String userId, String username, String email) {
        log.info("Attempting to fetch or create profile for user ID: {}", userId);

        // Logic remains the same - new User() will have null address/payment fields initially
        return userRepository.findById(userId)
                .map(existingUser -> {
                    log.info("Found existing user profile for ID: {}", userId);
                    return userMapper.toUserDto(existingUser);
                })
                .orElseGet(() -> {
                    log.info("User profile not found locally for ID: {}. Fetching from Keycloak and creating.", userId);
                    try {
                        UserRepresentation keycloakUser = keycloakAdminService.getKeycloakUserById(userId);
                        log.debug("Fetched Keycloak UserRepresentation: ID={}, Username={}, Email={}, FirstName={}, LastName={}",
                                keycloakUser.getId(), keycloakUser.getUsername(), keycloakUser.getEmail(),
                                keycloakUser.getFirstName(), keycloakUser.getLastName());

                        User newUser = new User();
                        newUser.setId(keycloakUser.getId());
                        newUser.setUsername(keycloakUser.getUsername());
                        newUser.setEmail(keycloakUser.getEmail());
                        newUser.setFirstName(keycloakUser.getFirstName());
                        newUser.setLastName(keycloakUser.getLastName());
                        // Address and Payment fields will be null by default
                        newUser.setSeller(false);

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
        log.debug("Updating profile for user ID: {}", userId);
        // Find the existing user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Use the updated mapper to apply changes from DTO to Entity
        userMapper.updateUserFromDto(updateUserDto, user);

        // Save the updated user
        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);
        // Map back to DTO to return
        return userMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void activateSellerRole(String userId) {
        // This logic remains the same
        log.debug("Activating seller role for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.isSeller()) {
            log.warn("User ID: {} is already a seller. No action taken.", userId);
            return;
        }
        keycloakAdminService.addSellerRoleToUser(userId);
        log.info("KeycloakAdminService successfully processed role addition for user ID: {}", userId);
        user.setSeller(true);
        userRepository.save(user);
        log.info("Internal seller status updated successfully for user ID: {}", userId);
    }

    // REMOVE all implementations for Address and PaymentMethod CRUD methods
}