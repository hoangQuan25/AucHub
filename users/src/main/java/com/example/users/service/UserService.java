// src/main/java/com/example/users/service/UserService.java
package com.example.users.service;

import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;
// REMOVE Address/Payment DTO imports and List import

public interface UserService {

    UserDto getOrCreateUserProfile(String userId, String username, String email);

    UserDto updateUserProfile(String userId, UpdateUserDto updateUserDto); // This now updates everything

    void activateSellerRole(String userId);

    // REMOVE all Address and PaymentMethod method signatures
}