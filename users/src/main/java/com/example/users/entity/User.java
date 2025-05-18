// File: com.example.users.entity.User.java
package com.example.users.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", schema = "user_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id; // Keycloak ID

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "is_seller", nullable = false)
    private boolean isSeller = false;

    // --- Single Address Fields (Keep as is for now for simplicity) ---
    // These are important for default shipping/billing if no other address is specified for an order.
    @Column(length = 255)
    private String streetAddress;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String stateProvince;
    @Column(length = 20)
    private String postalCode;
    @Column(length = 100)
    private String country;

    // --- Stripe Payment Method Integration ---
    @Column(name = "stripe_customer_id", length = 255, unique = true) // Store Stripe Customer ID
    private String stripeCustomerId;

    @Column(name = "stripe_default_payment_method_id", length = 255) // Store ID of the default Stripe PaymentMethod
    private String stripeDefaultPaymentMethodId;

    // --- Displayable Card Info (derived from default Stripe PaymentMethod) ---
    // These fields will be populated by your service AFTER a successful Stripe SetupIntent
    // and should not be directly updatable by the user via UpdateUserDto in a free-form way.
    @Column(name = "default_card_brand", length = 50)      // e.g., "Visa", "Mastercard"
    private String defaultCardBrand;
    @Column(name = "default_card_last4", length = 4)
    private String defaultCardLast4;
    @Column(name = "default_card_expiry_month", length = 2) // e.g., "12"
    private String defaultCardExpiryMonth;
    @Column(name = "default_card_expiry_year", length = 4)  // e.g., "2028"
    private String defaultCardExpiryYear;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}