// src/main/java/com/example/productservice/dto/UpdateProductDto.java
package com.example.products.dto;

import com.example.products.entity.ProductCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateProductDto {

    @Size(max = 150, message = "Title max length is 150")
    private String title;

    @Size(max = 5000, message = "Description too long")
    private String description;

    @NotNull(message = "Condition must be specified")
    private ProductCondition condition;

    @Size(min = 1, max = 10, message = "Must provide between 1 and 10 image URLs")
    @NotEmpty(message = "At least one image URL is required")
    private List<@URL(message = "Each image URL must be valid") @NotBlank String> imageUrls;

    @NotEmpty(message = "At least one category must be selected")
    private Set<Long> categoryIds;
}