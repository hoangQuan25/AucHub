// src/components/common/StarRating.jsx
import React from 'react';
import { FaStar, FaStarHalfAlt, FaRegStar } from 'react-icons/fa';

const StarRating = ({ rating, totalReviews, starSize = "text-yellow-500" }) => {
  if (typeof rating !== 'number' || rating < 0 || isNaN(rating)) { // Added NaN check
    if (totalReviews === 0) { // If explicitly 0 reviews, show "No reviews yet"
        return <span className="text-sm text-gray-500">No reviews yet</span>;
    }
    // If rating is invalid but totalReviews is not 0 (or undefined), it might be an error or loading state
    return <span className="text-sm text-gray-500">Rating not available</span>;
  }

  const fullStars = Math.floor(rating);
  const halfStar = rating % 1 >= 0.4 ? 1 : 0; // Adjusted threshold for half star if preferred
  const emptyStars = Math.max(0, 5 - fullStars - halfStar); // Ensure emptyStars is not negative

  return (
    <div className="flex items-center">
      {[...Array(fullStars)].map((_, i) => (
        <FaStar key={`full-${i}`} className={starSize} />
      ))}
      {halfStar === 1 && (
        <FaStarHalfAlt key="half" className={starSize} />
      )}
      {[...Array(emptyStars)].map((_, i) => (
        <FaRegStar key={`empty-${i}`} className={starSize} />
      ))}
      {typeof totalReviews === 'number' && ( // Check if totalReviews is a number
        <span className="ml-2 text-gray-600 text-sm">
          ({rating.toFixed(1)} from {totalReviews} review
          {totalReviews !== 1 ? 's' : ''})
        </span>
      )}
    </div>
  );
};

export default StarRating;