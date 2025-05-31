package com.example.users.mapper;

import com.example.users.dto.ReviewDto;
import com.example.users.entity.SellerReview;
import org.springframework.stereotype.Component;

@Component
public class SellerReviewMapper {

    public ReviewDto toReviewDto(SellerReview sellerReview) {
        if (sellerReview == null) {
            return null;
        }

        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(sellerReview.getId());
        reviewDto.setBuyerUsername(sellerReview.getBuyer().getUsername());
        reviewDto.setBuyerAvatarUrl(sellerReview.getBuyer().getAvatarUrl()); // Optional
        reviewDto.setRating(sellerReview.getRating());
        reviewDto.setComment(sellerReview.getComment());
        reviewDto.setCreatedAt(sellerReview.getCreatedAt());
        reviewDto.setOrderId(sellerReview.getOrderId()); // Assuming SellerReview has an Order reference

        return reviewDto;
    }
}
