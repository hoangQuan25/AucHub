package com.example.users.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDto {

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    // Add more specific validation like @Pattern if needed
    private String phoneNumber;

    // Address fields (optional update) - add validation as needed
    @Size(max = 255) private String streetAddress;
    @Size(max = 100) private String city;
    @Size(max = 100) private String stateProvince;
    @Size(max = 20) private String postalCode;
    @Size(max = 100) private String country;

    // Payment fields (optional update) - add validation as needed
    @Size(max = 50) private String paymentCardType;
    @Size(min = 4, max = 4) private String paymentLast4Digits;
    @Size(min = 2, max = 2) private String paymentExpiryMonth; // Consider regex for MM
    @Size(min = 4, max = 4) private String paymentExpiryYear; // Consider regex for YYYY
}
