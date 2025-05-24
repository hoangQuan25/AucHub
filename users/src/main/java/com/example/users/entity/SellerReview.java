// src/main/java/com/example/users/entity/SellerReview.java (adjust package if your reviews belong to a different domain/microservice context)
package com.example.users.entity; // Or a more appropriate package like com.example.reviews.entity

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_reviews") // Table name for reviews
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SellerReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Or GenerationType.IDENTITY if you prefer auto-increment long
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller; // The user being reviewed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer; // The user who wrote the review

    @Column(name = "order_id", nullable = false) // To link to the specific order
    private String orderId; // Assuming order IDs are strings (e.g., UUIDs from an Order service)

    @Column(nullable = false)
    private Integer rating; // e.g., 1 to 5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Consider adding a unique constraint for (seller_id, buyer_id, order_id)
    // to prevent multiple reviews for the same order by the same buyer.
    // @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "buyer_id", "order_id"}))
}