// src/main/java/com/example/users/controller/UserController.java
package com.example.users.controller;

import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;
import com.example.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity; // Keep
import org.springframework.web.bind.annotation.*; // Keep
import java.util.Map; // Keep for test endpoint

@RestController
@RequestMapping("/") // Keep or change based on Gateway rewrite
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
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
}