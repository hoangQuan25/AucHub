package com.example.users.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDto {

    private Long id; // Include ID for identification on the client

    private String streetAddress;

    private String city;

    private String stateProvince; // Optional

    private String postalCode;

    private String country;

    private boolean isDefault;

}