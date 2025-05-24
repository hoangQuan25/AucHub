// src/main/java/com/example/users/repository/SellerReviewRepository.java (adjust package)
package com.example.users.repository;

import com.example.users.entity.SellerReview;
import com.example.users.entity.User; // Import User if using it in queries
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerReviewRepository extends JpaRepository<SellerReview, String> {

    Page<SellerReview> findBySellerId(String sellerId, Pageable pageable);

    // Check if a buyer has already reviewed a specific order for a specific seller
    Optional<SellerReview> findBySellerAndBuyerAndOrderId(User seller, User buyer, String orderId);
    // Or if you store IDs:
    // Optional<SellerReview> findBySellerIdAndBuyerIdAndOrderId(String sellerId, String buyerId, String orderId);


    // --- Methods for calculating average rating and count ---

    @Query("SELECT AVG(sr.rating) FROM SellerReview sr WHERE sr.seller.id = :sellerId")
    Optional<Double> getAverageRatingBySellerId(@Param("sellerId") String sellerId);

    @Query("SELECT COUNT(sr) FROM SellerReview sr WHERE sr.seller.id = :sellerId")
    Long countReviewsBySellerId(@Param("sellerId") String sellerId);

}