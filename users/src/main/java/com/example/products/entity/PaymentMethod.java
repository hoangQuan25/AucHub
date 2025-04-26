package com.example.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods", schema = "user_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50) // e.g., "Visa", "Mastercard"
    private String cardType;

    @Column(nullable = false, length = 4)
    private String last4Digits;

    @Column(nullable = false, length = 2) // e.g., "12"
    private String expiryMonth;

    @Column(nullable = false, length = 4) // e.g., "2028"
    private String expiryYear;

    @Column(nullable = false)
    private boolean isDefault = false;

    // Reminder: DO NOT store full card numbers, CVV, etc.
    // For Stripe later, you'd store Stripe Customer ID on User entity
    // and Stripe PaymentMethod ID here instead of card details.
}