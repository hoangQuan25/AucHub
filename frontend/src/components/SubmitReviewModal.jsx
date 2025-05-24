// src/components/SubmitReviewModal.jsx
import React, { useState } from 'react';
import { FaStar } from 'react-icons/fa';
import apiClient from '../api/apiClient'; // Adjust path as needed

function SubmitReviewModal({ isOpen, onClose, orderId, sellerId, sellerName, onSuccess, onError }) {
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const handleStarClick = (value) => {
    setRating(value);
  };

  const handleStarHover = (value) => {
    setHoverRating(value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rating === 0) {
      setSubmitError('Please select a star rating.');
      return;
    }
    setIsSubmitting(true);
    setSubmitError('');

    const payload = {
      orderId: orderId,
      sellerId: sellerId,
      rating: rating,
      comment: comment.trim(),
    };

    try {
      const response = await apiClient.post('/users/submit-review', payload); // Your backend endpoint
      onSuccess(response.data); // Pass created review data back
      handleClose();
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to submit review.';
      setSubmitError(errorMessage);
      if (onError) onError(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (isSubmitting) return;
    setRating(0);
    setComment('');
    setHoverRating(0);
    setSubmitError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-md w-full">
        <div className="flex justify-between items-center mb-4 border-b pb-2">
          <h3 className="text-xl font-bold">Leave a Review for {sellerName || 'Seller'}</h3>
          <button onClick={handleClose} className="text-gray-500 hover:text-gray-800 text-2xl font-bold">&times;</button>
        </div>

        <form onSubmit={handleSubmit}>
          {submitError && <p className="text-red-500 text-sm mb-3">{submitError}</p>}

          <div className="mb-4">
            <label className="block mb-1 font-medium text-sm text-gray-700">Your Rating:</label>
            <div className="flex items-center space-x-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <FaStar
                  key={star}
                  size={28}
                  className={`cursor-pointer transition-colors ${
                    (hoverRating || rating) >= star ? 'text-yellow-500' : 'text-gray-300'
                  }`}
                  onClick={() => handleStarClick(star)}
                  onMouseEnter={() => handleStarHover(star)}
                  onMouseLeave={() => handleStarHover(0)}
                />
              ))}
            </div>
          </div>

          <div className="mb-6">
            <label htmlFor="comment" className="block mb-1 font-medium text-sm text-gray-700">
              Your Comment (Optional):
            </label>
            <textarea
              id="comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={4}
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="Share your experience with this seller..."
              maxLength={2000}
            />
          </div>

          <div className="flex justify-end space-x-3">
            <button
              type="button"
              onClick={handleClose}
              disabled={isSubmitting}
              className="px-4 py-2 bg-gray-200 hover:bg-gray-300 rounded text-gray-700 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || rating === 0}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded text-white disabled:opacity-50"
            >
              {isSubmitting ? 'Submitting...' : 'Submit Review'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default SubmitReviewModal;