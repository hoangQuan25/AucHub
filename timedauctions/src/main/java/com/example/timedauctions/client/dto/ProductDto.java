package com.example.timedauctions.client.dto;

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
    private ProductCondition condition; // Make sure this enum is defined/copied
    private List<String> imageUrls;
    private Set<CategoryDto> categories; // Make sure CategoryDto is defined/copied
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}