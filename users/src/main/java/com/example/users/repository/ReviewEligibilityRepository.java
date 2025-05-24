// package com.example.users.repository;
package com.example.users.repository;

import com.example.users.entity.ReviewEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, String> {
    Optional<ReviewEligibility> findByOrderIdAndBuyerIdAndSellerId(String orderId, String buyerId, String sellerId);
    Optional<ReviewEligibility> findByOrderId(String orderId); // If orderId is globally unique for reviews
}