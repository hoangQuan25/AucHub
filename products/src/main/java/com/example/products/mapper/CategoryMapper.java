// src/main/java/com/example/productservice/mapper/CategoryMapper.java
package com.example.products.mapper;

import com.example.products.dto.CategoryDto;
import com.example.products.entity.Category;
import java.util.List;
import java.util.Set;

public interface CategoryMapper {

    CategoryDto toCategoryDto(Category category);

    List<CategoryDto> toCategoryDtoList(List<Category> categories);

    Set<CategoryDto> toCategoryDtoSet(Set<Category> categories);

    // We don't need a method to map DTO back to Category for current use case
}