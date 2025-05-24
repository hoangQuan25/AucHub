package com.example.users.service.impl;

import com.example.users.dto.CreateReviewDto;
import com.example.users.dto.ReviewDto;
import com.example.users.entity.ReviewEligibility;
import com.example.users.entity.SellerReview;
import com.example.users.entity.User;
import com.example.users.exception.OperationNotAllowedException;
import com.example.users.exception.ResourceNotFoundException;
import com.example.users.mapper.SellerReviewMapper;
import com.example.users.repository.ReviewEligibilityRepository;
import com.example.users.repository.SellerReviewRepository;
import com.example.users.repository.UserRepository;
import com.example.users.service.SellerReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerReviewServiceImpl implements SellerReviewService {

    private final SellerReviewRepository sellerReviewRepository;
    private final UserRepository userRepository;
    private final ReviewEligibilityRepository reviewEligibilityRepository; // Added
    private final SellerReviewMapper sellerReviewMapper;

    @Override
    @Transactional
    public ReviewDto createReview(CreateReviewDto createReviewDto, String authenticatedBuyerId) {
        log.info("Attempting to create review by buyer {} for seller {} on order {}",
                authenticatedBuyerId, createReviewDto.getSellerId(), createReviewDto.getOrderId());

        // Find the eligibility record based on the order, buyer, and seller from the DTO
        ReviewEligibility eligibility = reviewEligibilityRepository
                .findByOrderIdAndBuyerIdAndSellerId(
                        createReviewDto.getOrderId(),
                        authenticatedBuyerId, // Ensure the authenticated user is the buyer for this eligibility
                        createReviewDto.getSellerId())
                .orElseThrow(() -> new OperationNotAllowedException(
                        "Not eligible to review this order. Eligibility record not found or details mismatch."));

        if (!eligibility.getBuyerId().equals(authenticatedBuyerId)) {
            throw new OperationNotAllowedException("Authenticated user does not match the buyer for this review eligibility.");
        }

        if (eligibility.isReviewSubmitted()) {
            throw new OperationNotAllowedException("A review has already been submitted for this order.");
        }

        // Fetch User entities for associating with SellerReview
        User seller = userRepository.findById(eligibility.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + eligibility.getSellerId()));
        User buyer = userRepository.findById(eligibility.getBuyerId())
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found with id: " + eligibility.getBuyerId()));

        SellerReview review = new SellerReview();
        review.setSeller(seller);
        review.setBuyer(buyer);
        review.setRating(createReviewDto.getRating());
        review.setComment(createReviewDto.getComment());
        review.setOrderId(eligibility.getOrderId());

        SellerReview savedReview = sellerReviewRepository.save(review);

        // Update eligibility record
        eligibility.setReviewSubmitted(true);
        eligibility.setSellerReview(savedReview); // Link the review to the eligibility record
        reviewEligibilityRepository.save(eligibility);

        log.info("Review created successfully with ID: {} for orderId {}", savedReview.getId(), eligibility.getOrderId());
        return sellerReviewMapper.toReviewDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsForSeller(String sellerId, Pageable pageable) {
        log.debug("Fetching reviews for seller ID: {}", sellerId);
        Page<SellerReview> reviewsPage = sellerReviewRepository.findBySellerId(sellerId, pageable);
        return reviewsPage.map(sellerReviewMapper::toReviewDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingForSeller(String sellerId) {
        return sellerReviewRepository.getAverageRatingBySellerId(sellerId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getReviewCountForSeller(String sellerId) {
        return sellerReviewRepository.countReviewsBySellerId(sellerId);
    }
}