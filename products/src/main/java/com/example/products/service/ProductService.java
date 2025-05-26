// src/main/java/com/example/products/service/products.java
package com.example.products.service;

import com.example.products.dto.CategoryDto;
import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.dto.UpdateProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    ProductDto createProduct(String sellerId, CreateProductDto dto);
    List<ProductDto> getProductsBySeller(String sellerId);
    List<CategoryDto> getAllCategories();
    Page<ProductDto> getProductsBySellerAndStatus(String sellerId, Boolean isSold, Pageable pageable);
    ProductDto updateProduct(String sellerId, Long productId, UpdateProductDto dto);
    void deleteProduct(String sellerId, Long productId);
    ProductDto getProductById(Long productId);
    ProductDto markProductAsSold(Long productId);
}