package com.example.products.controller;

import com.example.products.dto.CategoryDto;
import com.example.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final ProductService productService;

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        log.info("Received GET /api/products/categories request");
        return ResponseEntity.ok(productService.getAllCategories());
    }
}