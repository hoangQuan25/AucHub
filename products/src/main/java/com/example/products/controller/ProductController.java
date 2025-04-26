// src/main/java/com/example/products/controller/ProductController.java
package com.example.products.controller;

import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private static final String USER_ID_HEADER = "X-User-ID"; // Assuming Gateway adds this

    @PostMapping("new-product")
    public ResponseEntity<ProductDto> createProduct(
            @RequestHeader(USER_ID_HEADER) String sellerId, // Get seller ID from Gateway header
            @Valid @RequestBody CreateProductDto createProductDto) {
        log.info("Received POST /api/products request from seller ID: {}", sellerId);
        ProductDto newProduct = productService.createProduct(sellerId, createProductDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newProduct);
    }

    @GetMapping("/my") // Endpoint for sellers to get their own products
    public ResponseEntity<List<ProductDto>> getMyProducts(
            @RequestHeader(USER_ID_HEADER) String sellerId) { // Get seller ID from Gateway header
        log.info("Received GET /api/products/my request for seller ID: {}", sellerId);
        List<ProductDto> products = productService.getProductsBySeller(sellerId);
        return ResponseEntity.ok(products);
    }

    // TODO: Add endpoints later for GET /api/products/{id}, PUT /api/products/{id}, DELETE /api/products/{id}
    // Make sure to check ownership (sellerId) in update/delete operations!

    // TODO: Add endpoint for GET /api/products (public listing - maybe needs pagination?)
}