// src/main/java/com/example/productservice/repository/ProductRepository.java
package com.example.products.repository;

import com.example.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by the seller's ID (Keycloak ID)
    List<Product> findBySellerId(String sellerId);

    // Optional: Find specific product by ID and Seller ID (for update/delete later)
    // Optional<Product> findByIdAndSellerId(Long id, String sellerId);
}