package com.example.products.dto;

import com.example.products.entity.ProductCondition; // Import Enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull; // For Enum
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;
import java.util.Set; // Import Set
import org.hibernate.validator.constraints.URL;

@Data
public class CreateProductDto {
    @NotBlank @Size(max = 150) private String title;
    @NotBlank @Size(max = 5000) private String description;

    @NotNull(message = "Condition must be specified")
    private ProductCondition condition; // Add condition (Enum type)

    @NotEmpty @Size(min = 1, max = 10) // Update max images
    private List<@URL @NotBlank String> imageUrls;

    @NotEmpty(message = "At least one category must be selected")
    private Set<Long> categoryIds; // Frontend sends Set/List of selected category IDs
}