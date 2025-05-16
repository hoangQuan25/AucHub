// src/components/SellerDecisionModal.jsx
import React, { useState } from 'react';
import apiClient from '../api/apiClient'; // Your configured Axios instance
import { SELLER_DECISION_TYPES } from '../constants/orderConstants'; // You'll need to define this

// Define in src/constants/orderConstants.js or similar
// export const SELLER_DECISION_TYPES = {
//   OFFER_TO_NEXT_BIDDER: 'Offer to Next Bidder',
//   REOPEN_AUCTION: 'Re-open Auction',
//   CANCEL_SALE: 'Cancel Sale (No Winner)',
// };
// export const SELLER_DECISION_API_MAP = {
//   OFFER_TO_NEXT_BIDDER: 'OFFER_TO_NEXT_BIDDER',
//   REOPEN_AUCTION: 'REOPEN_AUCTION',
//   CANCEL_SALE: 'CANCEL_SALE',
// };


const SellerDecisionModal = ({ order, isOpen, onClose }) => {
  const [selectedDecision, setSelectedDecision] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmitDecision = async () => {
    if (!selectedDecision) {
      setError("Please select a decision.");
      return;
    }
    setIsProcessing(true);
    setError(null);
    try {
      // Ensure your OrderService's SellerDecisionDto expects 'decisionType'
      await apiClient.post(`/orders/${order.id}/seller-decision`, {
        decisionType: selectedDecision, // This should match the enum value your backend expects
      });
      alert("Decision submitted successfully!");
      onClose(); // Close modal and trigger refetch in parent
    } catch (err) {
      console.error("Failed to submit seller decision:", err);
      setError(err.response?.data?.message || "Could not submit decision.");
    } finally {
      setIsProcessing(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex justify-center items-center">
      <div className="relative mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        <div className="mt-3 text-center">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Seller Decision for Order #{order.id.substring(0,8)}
          </h3>
          <div className="mt-2 px-7 py-3">
            <p className="text-sm text-gray-500 mb-4">
              The winner did not complete the payment. Please choose an action:
            </p>
            <div className="space-y-2 text-left">
              {Object.entries(SELLER_DECISION_TYPES).map(([apiKey, displayText]) => (
                <label key={apiKey} className="flex items-center space-x-2 p-2 rounded hover:bg-gray-100 cursor-pointer">
                  <input
                    type="radio"
                    name="sellerDecision"
                    value={apiKey} // This should be the value backend expects e.g. OFFER_TO_NEXT_BIDDER
                    checked={selectedDecision === apiKey}
                    onChange={(e) => setSelectedDecision(e.target.value)}
                    className="form-radio h-4 w-4 text-blue-600"
                  />
                  <span className="text-sm text-gray-700">{displayText}</span>
                </label>
              ))}
            </div>
            {error && <p className="text-xs text-red-500 mt-3">{error}</p>}
          </div>
          <div className="items-center px-4 py-3 mt-4 bg-gray-50 rounded-b-md">
            <button
              onClick={handleSubmitDecision}
              disabled={isProcessing || !selectedDecision}
              className="px-4 py-2 bg-blue-500 text-white text-base font-medium rounded-md w-full shadow-sm hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:opacity-50"
            >
              {isProcessing ? "Processing..." : "Submit Decision"}
            </button>
            <button
              onClick={() => { if(!isProcessing) onClose();}}
              className="mt-2 px-4 py-2 bg-gray-200 text-gray-700 text-base font-medium rounded-md w-full shadow-sm hover:bg-gray-300 focus:outline-none focus:ring-2 focus:ring-gray-300"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SellerDecisionModal;