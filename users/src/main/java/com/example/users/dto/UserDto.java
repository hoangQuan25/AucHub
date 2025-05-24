// File: com.example.users.dto.UserDto.java
package com.example.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    private boolean isSeller;
    private String sellerDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Address (remains the same for single address)
    private String streetAddress;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;

    // Stripe related (for display)
    private String stripeCustomerId; // Useful for devs, maybe not for users directly
    private String stripeDefaultPaymentMethodId;
    private boolean hasDefaultPaymentMethod; // Derived field, true if stripeDefaultPaymentMethodId is set
    private String defaultCardBrand;
    private String defaultCardLast4;
    private String defaultCardExpiryMonth;
    private String defaultCardExpiryYear;
}