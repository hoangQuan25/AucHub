// src/main/java/com/example/productservice/mapper/ProductMapperManualImpl.java
package com.example.products.mapper.impl;

import com.example.products.dto.CreateProductDto;
import com.example.products.dto.ProductDto;
import com.example.products.entity.Product;
import com.example.products.mapper.CategoryMapper;
import com.example.products.mapper.ProductMapper;
import lombok.RequiredArgsConstructor; // Use Lombok for constructor injection
import org.springframework.stereotype.Component; // Import Component

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component // Make it a Spring Bean
@RequiredArgsConstructor // Lombok for injecting CategoryMapper
public class ProductMapperImpl implements ProductMapper {

    private final CategoryMapper categoryMapper; // Inject the CategoryMapper bean

    @Override
    public ProductDto toProductDto(Product product) {
        if (product == null) {
            return null;
        }
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setDescription(product.getDescription());
        dto.setSellerId(product.getSellerId());
        dto.setCondition(product.getCondition()); // Map condition
        dto.setImageUrls(product.getImageUrls() != null ? List.copyOf(product.getImageUrls()) : Collections.emptyList()); // Map image URLs (create copy)
        dto.setCategories(categoryMapper.toCategoryDtoSet(product.getCategories())); // Use CategoryMapper for nested mapping
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    @Override
    public List<ProductDto> toProductDtoList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream()
                .map(this::toProductDto)
                .collect(Collectors.toList());
    }

    @Override
    public Product createDtoToProduct(CreateProductDto dto) {
        if (dto == null) {
            return null;
        }
        Product product = new Product();
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setCondition(dto.getCondition()); // Map condition
        // Note: Intentionally DO NOT map id, sellerId, categories, createdAt, updatedAt
        // The service layer will handle setting sellerId and categories. DB handles timestamps/ID.
        // Image URLs are directly set on the entity field by the service
        product.setImageUrls(dto.getImageUrls() != null ? new ArrayList<>(dto.getImageUrls()) : new ArrayList<>());

        return product;
    }
}