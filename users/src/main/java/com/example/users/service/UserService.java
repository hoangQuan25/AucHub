// src/main/java/com/example/users/service/UserService.java
package com.example.users.service;

import com.example.users.dto.PublicSellerProfileDto;
import com.example.users.dto.UserBasicInfoDto;
import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;

import java.util.List;
import java.util.Map;
// REMOVE Address/Payment DTO imports and List import

public interface UserService {

    UserDto getOrCreateUserProfile(String userId, String username, String email);

    UserDto updateUserProfile(String userId, UpdateUserDto updateUserDto); // This now updates everything

    UserDto updateAvatarUrl(String userId, String avatarUrl);

    void activateSellerRole(String userId);

    Map<String, UserBasicInfoDto> getUsersBasicInfo(List<String> userIds);

    PublicSellerProfileDto getPublicSellerProfile(String userIdOrUsername);

    void saveStripePaymentDetails(
            String userId,
            String stripeCustomerId,
            String stripeDefaultPaymentMethodId,
            String cardBrand,
            String last4,
            String expMonth,
            String expYear
    );
}