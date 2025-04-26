// src/main/java/com/example/products/service/products.java
package com.example.products.service;

import com.example.products.dto.CategoryDto;
import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.dto.UpdateProductDto;

import java.util.List;

public interface ProductService {
    ProductDto createProduct(String sellerId, CreateProductDto dto);
    List<ProductDto> getProductsBySeller(String sellerId);
    List<CategoryDto> getAllCategories();
    ProductDto updateProduct(String sellerId, Long productId, UpdateProductDto dto);
    void deleteProduct(String sellerId, Long productId);
}