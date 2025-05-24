// src/main/java/com/example/users/dto/CreateReviewDto.java (adjust package)
package com.example.users.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewDto {

    @NotBlank(message = "Seller ID cannot be blank")
    private String sellerId;

    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    @NotNull(message = "Rating cannot be null")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment; // Comment can be optional
}