package com.example.liveauctions.client.dto; // Example package within liveauctions

import com.example.liveauctions.client.dto.ProductCondition; // Need enum too

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data // Assuming using Lombok Data, ensure required constructors/getters/setters exist
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