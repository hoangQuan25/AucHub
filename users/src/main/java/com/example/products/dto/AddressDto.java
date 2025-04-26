package com.example.users.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
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

    // We exclude the 'User user' field to avoid circular references
    // and because it's usually implicit when fetching addresses via a user.
}