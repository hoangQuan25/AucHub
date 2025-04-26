// src/main/java/com/example/users/mapper/UserMapperManualImpl.java
package com.example.users.mapper.impl;

import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;
import com.example.users.entity.User;
import com.example.users.mapper.UserMapper;
import org.springframework.stereotype.Component;
import java.util.Objects; // Import Objects

@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toUserDto(User user) {
        if (user == null) return null;

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setSeller(user.isSeller());

        // Map new direct fields
        dto.setStreetAddress(user.getStreetAddress());
        dto.setCity(user.getCity());
        dto.setStateProvince(user.getStateProvince());
        dto.setPostalCode(user.getPostalCode());
        dto.setCountry(user.getCountry());
        dto.setPaymentCardType(user.getPaymentCardType());
        dto.setPaymentLast4Digits(user.getPaymentLast4Digits());
        dto.setPaymentExpiryMonth(user.getPaymentExpiryMonth());
        dto.setPaymentExpiryYear(user.getPaymentExpiryYear());

        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    @Override
    public void updateUserFromDto(UpdateUserDto dto, User user) {
        if (dto == null || user == null) return;

        // Update basic info (only if DTO field is not null)
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());

        // Update address info (only if DTO field is not null)
        if (dto.getStreetAddress() != null) user.setStreetAddress(dto.getStreetAddress());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getStateProvince() != null) user.setStateProvince(dto.getStateProvince());
        if (dto.getPostalCode() != null) user.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) user.setCountry(dto.getCountry());

        // Update payment info (only if DTO field is not null)
        if (dto.getPaymentCardType() != null) user.setPaymentCardType(dto.getPaymentCardType());
        if (dto.getPaymentLast4Digits() != null) user.setPaymentLast4Digits(dto.getPaymentLast4Digits());
        if (dto.getPaymentExpiryMonth() != null) user.setPaymentExpiryMonth(dto.getPaymentExpiryMonth());
        if (dto.getPaymentExpiryYear() != null) user.setPaymentExpiryYear(dto.getPaymentExpiryYear());

        // Note: This simple null check means you cannot clear a field by sending null.
        // If you need that, adjust the logic (e.g., check if the key exists in the request).
    }
}