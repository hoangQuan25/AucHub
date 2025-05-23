// File: com.example.users.dto.UserBasicInfoDto.java
package com.example.orders.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserBasicInfoDto {
    private String id;
    private String username;
    private String email;
    private String phoneNumber; // Included

    // Full Address fields (for DeliveriesService and others)
    private String streetAddress;
    private String city;
    private String stateProvince; // Can be nullable if address structure varies
    private String postalCode;
    private String country;

    // Stripe-related identifiers (for PaymentsService to use saved payment methods in the future)
    private String stripeCustomerId;             // Stripe's Customer ID (cus_xxx)
    private String stripeDefaultPaymentMethodId; // Stripe's PaymentMethod ID (pm_xxx) for the default card

    // Basic displayable details of the default payment method (useful for context)
    private String defaultCardBrand; // e.g., "Visa", "Mastercard"
    private String defaultCardLast4; // e.g., "4242"

    // firstName and lastName are intentionally excluded as per your request.
}