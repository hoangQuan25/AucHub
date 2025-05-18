// File: com.example.users.mapper.impl.UserMapperImpl.java
package com.example.users.mapper.impl;

// ... imports ...
import com.example.users.dto.UserBasicInfoDto;
import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;
import com.example.users.entity.User;
import com.example.users.mapper.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toUserDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .isSeller(user.isSeller())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                // Address
                .streetAddress(user.getStreetAddress())
                .city(user.getCity())
                .stateProvince(user.getStateProvince())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                // Stripe & Default Card Info
                .stripeCustomerId(user.getStripeCustomerId())
                .stripeDefaultPaymentMethodId(user.getStripeDefaultPaymentMethodId())
                .hasDefaultPaymentMethod(user.getStripeDefaultPaymentMethodId() != null) // Derived
                .defaultCardBrand(user.getDefaultCardBrand())
                .defaultCardLast4(user.getDefaultCardLast4())
                .defaultCardExpiryMonth(user.getDefaultCardExpiryMonth())
                .defaultCardExpiryYear(user.getDefaultCardExpiryYear())
                .build();
    }

    @Override
    public void updateUserFromDto(UpdateUserDto dto, User user) {
        if (dto == null || user == null) return;

        // Update basic info
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());

        // Update address info
        if (dto.getStreetAddress() != null) user.setStreetAddress(dto.getStreetAddress());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getStateProvince() != null) user.setStateProvince(dto.getStateProvince());
        if (dto.getPostalCode() != null) user.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) user.setCountry(dto.getCountry());

        // DO NOT update Stripe or default card display fields from this DTO.
        // Those are managed by the payment method setup flow.
    }

    @Override
    public UserBasicInfoDto toUserBasicInfoDto(User user) {
        if (user == null) {
            return null;
        }
        return UserBasicInfoDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber()) // Added
                // Address fields
                .streetAddress(user.getStreetAddress())
                .city(user.getCity())
                .stateProvince(user.getStateProvince())
                .postalCode(user.getPostalCode())
                .country(user.getCountry())
                // Stripe related identifiers
                .stripeCustomerId(user.getStripeCustomerId())
                .stripeDefaultPaymentMethodId(user.getStripeDefaultPaymentMethodId())
                // Displayable default card info
                .defaultCardBrand(user.getDefaultCardBrand())
                .defaultCardLast4(user.getDefaultCardLast4())
                .build();
    }
}