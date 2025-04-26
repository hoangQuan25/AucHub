package com.example.products.dto;

import lombok.Data;
@Data
public class CategoryDto {
    private Long id;
    private String name;
    private Long parentId; // Include parent ID for frontend tree building
}