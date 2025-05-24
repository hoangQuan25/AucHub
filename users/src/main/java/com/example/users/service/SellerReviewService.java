// src/main/java/com/example/users/service/SellerReviewService.java (adjust package)
package com.example.users.service;

import com.example.users.dto.CreateReviewDto;
import com.example.users.dto.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SellerReviewService {

    ReviewDto createReview(CreateReviewDto createReviewDto, String buyerId);

    Page<ReviewDto> getReviewsForSeller(String sellerId, Pageable pageable);

    Double getAverageRatingForSeller(String sellerId); // For calculation

    Long getReviewCountForSeller(String sellerId); // For calculation
}