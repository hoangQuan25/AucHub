// File: com.example.users.dto.UpdateUserDto.java
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
    private String phoneNumber;

    private String sellerDescription;

    // Address fields (user can update these)
    @Size(max = 255) private String streetAddress;
    @Size(max = 100) private String city;
    @Size(max = 100) private String stateProvince;
    @Size(max = 20) private String postalCode;
    @Size(max = 100) private String country;
}