// src/main/java/com/example/users/service/UserService.java
package com.example.users.service;

import com.example.users.dto.*;

import java.util.List;
import java.util.Map;

public interface UserService {

    UserDto getOrCreateUserProfile(String userId, String username, String email);

    UserDto updateUserProfile(String userId, UpdateUserDto updateUserDto); // This now updates everything

    UserDto updateAvatarUrl(String userId, String avatarUrl);

    void activateSellerRole(String userId);

    Map<String, UserBasicInfoDto> getUsersBasicInfo(List<String> userIds);

    PublicSellerProfileDto getPublicSellerProfile(String userIdOrUsername);

    StripeSetupIntentSecretDto createSetupIntentSecret(String userId, String email, String username);
    StripePaymentMethodConfirmationResultDto confirmPaymentMethodSetup(String userId, String email,
                                                                       String username, StripeSetupConfirmationRequestDto confirmationRequest);

    void saveStripePaymentDetails(
            String userId,
            String stripeCustomerId,
            String stripeDefaultPaymentMethodId,
            String cardBrand,
            String last4,
            String expMonth,
            String expYear
    );

    void processFirstWinnerPaymentDefault(String userId);

    UserBanStatusDto getUserBanStatus(String userId);
}