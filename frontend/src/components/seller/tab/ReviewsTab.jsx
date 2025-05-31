import React, { useEffect, useState } from "react";
import { FaUserCircle } from "react-icons/fa";
import StarRating from "../../common/StarRating";
import PaginationControls from "../../PaginationControls";
import apiClient from "../../../api/apiClient"; // Adjust path as needed

const ReviewsTab = ({
  reviews,
  isLoadingReviews,
  reviewsError,
  reviewPage,
  reviewTotalPages,
  reviewPageSize,
  handleReviewPageChange,
  sellerUsername,
}) => {
  const [orderInfoMap, setOrderInfoMap] = useState({});

  useEffect(() => {
    // Fetch order info for all reviews on this page
    const fetchOrders = async () => {
      const newMap = {};
      await Promise.all(
        reviews.map(async (review) => {
          if (review.orderId) {
            try {
              const resp = await apiClient.get(`/orders/${review.orderId}`);
              newMap[review.orderId] = resp.data;
            } catch (e) {
              // Optionally handle error
              newMap[review.orderId] = null;
            }
          }
        })
      );
      setOrderInfoMap(newMap);
      console.log("Order info map updated:", newMap);
    };
    if (reviews && reviews.length > 0) {
      fetchOrders();
    }
  }, [reviews]);

  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-semibold mb-6 text-gray-800">
        Buyer Reviews & Ratings for {sellerUsername || "this Seller"}
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
          {reviews.map((review) => {
            const order = orderInfoMap[review.orderId];
            return (
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
                  <StarRating rating={review.rating} />
                </div>
                {review.comment && (
                  <p className="ml-10 text-gray-600 text-sm whitespace-pre-line">
                    {review.comment}
                  </p>
                )}
                {/* Order Info */}
                {order && order.items && order.items.length > 0 && (
                  <div className="ml-10 mt-2 flex items-center gap-3 bg-gray-50 rounded p-2 border border-gray-100">
                    {order.items[0].imageUrl && (
                      <img
                        src={order.items[0].imageUrl}
                        alt={order.items[0].title}
                        className="w-12 h-12 object-cover rounded border"
                      />
                    )}
                    <span className="text-sm text-gray-700 font-medium">
                      {order.items[0].title}
                    </span>
                  </div>
                )}
              </div>
            );
          })}
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
