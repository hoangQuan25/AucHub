// src/main/java/com/example/users/dto/ReviewDto.java (adjust package)
package com.example.users.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private String id;
    private String buyerUsername; // Display username instead of full buyer object for privacy/simplicity
    private String buyerAvatarUrl; // Optional: if you want to show buyer's avatar
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}