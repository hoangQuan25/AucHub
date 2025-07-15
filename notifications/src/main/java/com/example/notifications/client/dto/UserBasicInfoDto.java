package com.example.notifications.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoDto {
    private String id;
    private String username;
    private String email;
    private String avatarUrl;
    private String phoneNumber; // Included

    private String streetAddress;
    private String city;
    private String stateProvince; // Can be nullable if address structure varies
    private String postalCode;
    private String country;

    private String stripeCustomerId;             // Stripe's Customer ID (cus_xxx)
    private String stripeDefaultPaymentMethodId; // Stripe's PaymentMethod ID (pm_xxx) for the default card

    private String defaultCardBrand; // e.g., "Visa", "Mastercard"
    private String defaultCardLast4; // e.g., "4242"

    private boolean emailNotificationsEnabled;
}
