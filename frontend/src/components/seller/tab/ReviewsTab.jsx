// src/components/seller/tabs/ReviewsTab.jsx
import React from 'react';
import { FaUserCircle } from 'react-icons/fa';
import StarRating from '../../common/StarRating'; // Adjust path as needed
import PaginationControls from '../../PaginationControls'; // Adjust path as needed

const ReviewsTab = ({
  reviews,
  isLoadingReviews,
  reviewsError,
  reviewPage,
  reviewTotalPages,
  reviewPageSize, // Renamed from REVIEW_PAGE_SIZE for clarity as prop
  handleReviewPageChange,
  sellerUsername // Added sellerUsername for the heading
}) => {
  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-semibold mb-6 text-gray-800">
        Buyer Reviews & Ratings for {sellerUsername || 'this Seller'}
      </h2>
      {isLoadingReviews && (
        <div className="text-center p-4">Loading reviews...</div>
      )}
      {reviewsError && (
        <div className="text-center p-4 text-red-500 bg-red-50 rounded">
          {reviewsError}
        </div>
      )}
      {!isLoadingReviews && !reviewsError && reviews.length === 0 && (
        <p className="text-gray-500">This seller has no reviews yet.</p>
      )}
      {!isLoadingReviews && !reviewsError && reviews.length > 0 && (
        <div className="space-y-6">
          {reviews.map((review) => (
            <div
              key={review.id}
              className="border-b pb-4 last:border-b-0 last:pb-0"
            >
              <div className="flex items-center mb-2">
                {review.buyerAvatarUrl ? (
                  <img
                    src={review.buyerAvatarUrl}
                    alt={review.buyerUsername}
                    className="w-8 h-8 rounded-full mr-2 object-cover"
                  />
                ) : (
                  <FaUserCircle className="w-8 h-8 text-gray-400 mr-2" />
                )}
                <span className="font-semibold text-gray-700">
                  {review.buyerUsername || "Anonymous Buyer"}
                </span>
                <span className="text-xs text-gray-500 ml-auto">
                  {new Date(review.createdAt).toLocaleDateString()}
                </span>
              </div>
              <div className="ml-10 mb-1">
                <StarRating rating={review.rating} /> {/* totalReviews not typically shown per review */}
              </div>
              {review.comment && (
                <p className="ml-10 text-gray-600 text-sm whitespace-pre-line">
                  {review.comment}
                </p>
              )}
            </div>
          ))}
          {reviewTotalPages > 1 && (
            <PaginationControls
              pagination={{
                page: reviewPage,
                totalPages: reviewTotalPages,
                size: reviewPageSize,
              }}
              onPageChange={handleReviewPageChange}
              isLoading={isLoadingReviews}
            />
          )}
        </div>
      )}
    </div>
  );
};

export default ReviewsTab;