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
@Table(name = "users", schema = "user_schema") // Specify schema
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    // Assuming Keycloak ID (UUID) is the primary key & provided externally
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

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
    private boolean isSeller = false; // Default to false

    // --- Single Address Fields (Nullable) ---
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

    // --- Single Payment Method Fields (Nullable, Mock Data) ---
    @Column(length = 50)
    private String paymentCardType; // e.g., "Visa", "Mastercard"
    @Column(length = 4)
    private String paymentLast4Digits;
    @Column(length = 2)
    private String paymentExpiryMonth; // e.g., "12"
    @Column(length = 4)
    private String paymentExpiryYear; // e.g., "2028"

    // --- Timestamps ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}