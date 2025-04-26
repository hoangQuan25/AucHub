package com.example.products.dto;

import com.example.products.entity.ProductCondition;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class ProductDto {
    private Long id;
    private String title;
    private String description;
    private String sellerId;
    private ProductCondition condition; // Add condition
    private List<String> imageUrls;
    private Set<CategoryDto> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}