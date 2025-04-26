// src/main/java/com/example/products/service/productsImpl.java
package com.example.products.service.impl;

import com.example.products.dto.CategoryDto;
import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.entity.Category;
import com.example.products.entity.Product;
import com.example.products.mapper.CategoryMapper;
import com.example.products.mapper.ProductMapper;
import com.example.products.repository.CategoryRepository;
import com.example.products.repository.ProductRepository;
import com.example.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;


    @Override
    @Transactional
    public ProductDto createProduct(String sellerId, CreateProductDto dto) {
        log.info("Creating product '{}' for seller ID: {}", dto.getTitle(), sellerId);

        // 1. Fetch Category entities based on IDs provided in DTO
        Set<Category> categories = new HashSet<>();
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            categories = new HashSet<>(categoryRepository.findAllById(dto.getCategoryIds()));
            if (categories.size() != dto.getCategoryIds().size()) {
                log.warn("Some category IDs provided were not found for seller ID: {}", sellerId);
                // Optionally throw an exception if all IDs must be valid
                // throw new IllegalArgumentException("Invalid category ID(s) provided.");
            }
        }
        if (categories.isEmpty()) {
            log.error("No valid categories found or provided for product creation by seller ID: {}", sellerId);
            throw new IllegalArgumentException("At least one valid category must be assigned.");
        }


        // 2. Map basic product info from DTO (now includes condition)
        Product product = productMapper.createDtoToProduct(dto);

        // 3. Set fields not mapped automatically
        product.setSellerId(sellerId);
        product.setCategories(categories); // Set the fetched category entities

        // 4. Save the product
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return productMapper.toProductDto(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsBySeller(String sellerId) {
        log.debug("Fetching products for seller ID: {}", sellerId);
        List<Product> products = productRepository.findBySellerId(sellerId);
        return productMapper.toProductDtoList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        log.debug("Fetching all categories");
        List<Category> categories = categoryRepository.findAll(); // Fetch all
        // Let frontend build hierarchy from flat list + parentId for simplicity now
        return categoryMapper.toCategoryDtoList(categories);
    }
}