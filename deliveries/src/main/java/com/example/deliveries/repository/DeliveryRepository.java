// File: com.example.deliveries.repository.DeliveryRepository.java
package com.example.deliveries.repository;

import com.example.deliveries.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);
    List<Delivery> findByBuyerIdOrderByCreatedAtDesc(String buyerId);
    List<Delivery> findBySellerIdOrderByCreatedAtDesc(String sellerId);
    // Add other query methods as needed, e.g., findByTrackingNumber, findByStatus, etc.
}