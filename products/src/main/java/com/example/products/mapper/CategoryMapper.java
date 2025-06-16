// src/main/java/com/example/productservice/mapper/CategoryMapperManualImpl.java
package com.example.products.mapper;

import com.example.products.dto.CategoryDto;
import com.example.products.entity.Category;
import org.springframework.stereotype.Component; // Import Component

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component // Make it a Spring Bean
public class CategoryMapper {

    public CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        return dto;
    }

    public List<CategoryDto> toCategoryDtoList(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(this::toCategoryDto) // Use the single DTO mapping method
                .collect(Collectors.toList());
    }

    public Set<CategoryDto> toCategoryDtoSet(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptySet();
        }
        return categories.stream()
                .map(this::toCategoryDto) // Use the single DTO mapping method
                .collect(Collectors.toSet());
    }
}