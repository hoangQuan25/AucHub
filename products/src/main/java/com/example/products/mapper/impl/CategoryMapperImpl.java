// src/main/java/com/example/productservice/mapper/CategoryMapperManualImpl.java
package com.example.products.mapper.impl;

import com.example.products.dto.CategoryDto;
import com.example.products.entity.Category;
import com.example.products.mapper.CategoryMapper;
import org.springframework.stereotype.Component; // Import Component

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component // Make it a Spring Bean
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setParentId(category.getParentId());
        // Add other fields if Category entity/DTO expands
        return dto;
    }

    @Override
    public List<CategoryDto> toCategoryDtoList(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        return categories.stream()
                .map(this::toCategoryDto) // Use the single DTO mapping method
                .collect(Collectors.toList());
    }

    @Override
    public Set<CategoryDto> toCategoryDtoSet(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptySet();
        }
        return categories.stream()
                .map(this::toCategoryDto) // Use the single DTO mapping method
                .collect(Collectors.toSet());
    }
}