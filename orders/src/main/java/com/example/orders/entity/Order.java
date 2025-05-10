package com.example.orders.entity; // In your Orders service

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order_service_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID auctionId;

    @Column(nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private String productTitleSnapshot;

    @Column(length = 1024, updatable = false)
    private String productImageUrlSnapshot;

    @Column(nullable = false, updatable = false)
    private String sellerId;

    @Column(nullable = false, updatable = false)
    private String sellerUsernameSnapshot;

    // --- Auction Type ---
    @Column(nullable = false, updatable = false, length = 10) // e.g., "LIVE", "TIMED"
    private String auctionType; // NEW FIELD

    // --- Initial Winner Details ---
    @Column(nullable = false, updatable = false)
    private String initialWinnerId;

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal initialWinningBidAmount;

    @Column(updatable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    // --- Potential Next Bidders (from the event) ---
    @Column(updatable = false)
    private BigDecimal reservePriceSnapshot;

    @Column(updatable = false)
    private String eligibleSecondBidderId;
    @Column(updatable = false, precision = 19, scale = 2)
    private BigDecimal eligibleSecondBidAmount;

    @Column(updatable = false)
    private String eligibleThirdBidderId;
    @Column(updatable = false, precision = 19, scale = 2)
    private BigDecimal eligibleThirdBidAmount;

    // --- Current State ---
    @Column(nullable = false)
    private String currentBidderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentAmountDue;

    @Column(nullable = false)
    private LocalDateTime paymentDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private int paymentOfferAttempt;

    private String paymentTransactionRef;

    @Enumerated(EnumType.STRING)
    private SellerDecisionType sellerDecision;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}