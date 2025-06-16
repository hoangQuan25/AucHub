// src/main/java/com/example/productservice/repository/ProductRepository.java
package com.example.products.repository;

import com.example.products.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerId(String sellerId);

    Page<Product> findBySellerId(String sellerId, Pageable pageable);

    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}