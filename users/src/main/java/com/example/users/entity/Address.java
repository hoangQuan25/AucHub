package com.example.users.entity;

import jakarta.persistence.*;
import lombok.*; // Use Lombok getters/setters etc.

@Entity
@Table(name = "addresses", schema = "user_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Many addresses to one user
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String streetAddress;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100) // Optional state/province
    private String stateProvince;

    @Column(nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false)
    private boolean isDefault = false;

    // Consider adding created/updated timestamps if needed
}