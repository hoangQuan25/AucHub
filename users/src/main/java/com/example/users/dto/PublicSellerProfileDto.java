// src/main/java/com/example/users/dto/PublicSellerProfileDto.java
package com.example.users.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicSellerProfileDto {
    private String id; // User ID of the seller
    private String username;
    private String avatarUrl;
    private String sellerDescription;
    private LocalDateTime memberSince; // Corresponds to User's createdAt

     private Double averageRating;
     private Long reviewCount;
}