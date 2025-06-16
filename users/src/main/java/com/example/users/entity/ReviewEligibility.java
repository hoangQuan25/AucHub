// Assuming this is in your User service's entity package e.g., com.example.users.entity
package com.example.users.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_eligibilities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "buyer_id", "seller_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id", nullable = false, unique = true) // Assuming one review per order overall
    private String orderId; // From the Delivery event (UUID usually)

    @Column(name = "buyer_id", nullable = false)
    private String buyerId; // User ID of the buyer

    @Column(name = "seller_id", nullable = false)
    private String sellerId; // User ID of the seller

    @Column(name = "eligible_from_timestamp", nullable = false)
    private LocalDateTime eligibleFromTimestamp; // When the delivery was completed

    @Column(name = "review_submitted", nullable = false)
    @Builder.Default
    private boolean reviewSubmitted = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_review_id", unique = true) // Link to the actual review if submitted
    private SellerReview sellerReview;

    @CreationTimestamp // For tracking when this eligibility record was created
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}