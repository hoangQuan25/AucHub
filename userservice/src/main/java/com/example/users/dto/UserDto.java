package com.example.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor   // Added for convenience
@AllArgsConstructor  // Added for convenience
@Builder             // Added for convenience
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private boolean isSeller;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String streetAddress;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String paymentCardType;
    private String paymentLast4Digits;
    private String paymentExpiryMonth;
    private String paymentExpiryYear;
}