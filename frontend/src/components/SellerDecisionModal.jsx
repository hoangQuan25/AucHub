// src/components/SellerDecisionModal.jsx
import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient'; // Your configured Axios instance
// Assuming SELLER_DECISION_TYPES is correctly imported from your constants
import { SELLER_DECISION_TYPES } from '../constants/orderConstants';

const SellerDecisionModal = ({ order, isOpen, onClose, onInitiateReopenAuction }) => {
  const [selectedDecision, setSelectedDecision] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);

  const availableDecisionTypes = Object.entries(SELLER_DECISION_TYPES).filter(
    ([apiKey, displayText]) => {
      if (apiKey === 'OFFER_TO_NEXT_BIDDER') {
        
        if (order.paymentOfferAttempt === 1) {
          return !!order.eligibleSecondBidderId; // Show if 2nd bidder exists
        } else if (order.paymentOfferAttempt === 2) {
          return !!order.eligibleThirdBidderId; // Show if 3rd bidder exists
        } else {
          
          return false;
        }
      }
      return true;
    }
  );

  const handleSubmitDecision = async () => {
    const currentSelectedDecisionIsValid = availableDecisionTypes.some(([apiKey]) => apiKey === selectedDecision);

    if (!selectedDecision || !currentSelectedDecisionIsValid) {
      setError("Please select an available decision.");
      // If the previously selectedDecision is no longer in availableDecisionTypes (e.g. due to data change), clear it
      if (selectedDecision && !currentSelectedDecisionIsValid) {
        setSelectedDecision('');
      }
      return;
    }

    setIsProcessing(true);
    if (selectedDecision === 'REOPEN_AUCTION' && onInitiateReopenAuction) {
      // For REOPEN_AUCTION:
      // 1. Directly call the handler passed from the parent page.
      //    This handler will close this modal and open the StartAuctionModal.
      onInitiateReopenAuction(order);
      // setIsProcessing(false); // isProcessing will be reset by useEffect on isOpen when modal closes
    } else {
      // For OTHER decisions (OFFER_TO_NEXT_BIDDER, CANCEL_SALE):
      // Proceed with the API call to update the order status.
      setError(null);
      try {
        await apiClient.post(`/orders/${order.id}/seller-decision`, {
          decisionType: selectedDecision,
        });
        onClose(); // Close modal and trigger refetch in parent
      } catch (err) {
        console.error("Failed to submit seller decision:", err);
        setError(err.response?.data?.message || "Could not submit decision.");
      } finally {
        setIsProcessing(false); // Reset processing for non-reopen cases
      }
    }
  };

  useEffect(() => {
    if (isOpen) {
        // Reset state when modal opens
        setSelectedDecision('');
        setError(null);
        setIsProcessing(false);
    }
  }, [isOpen, order]); 

  useEffect(() => {
    if (isOpen && selectedDecision && !availableDecisionTypes.some(([apiKey]) => apiKey === selectedDecision)) {
        setSelectedDecision(''); // Clear if current selection is not in the filtered list
    }
    
  }, [isOpen, selectedDecision, availableDecisionTypes]);


  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex justify-center items-center p-4">
      <div className="relative mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        <div className="mt-3 text-center">
          <h3 className="text-lg leading-6 font-medium text-gray-900">
            Seller Decision for Order #{order.id.substring(0,8)}
          </h3>
          <div className="mt-2 px-7 py-3">
            <p className="text-sm text-gray-500 mb-4">
              {order.paymentOfferAttempt === 1 && "The initial winner did not complete the payment."}
              {order.paymentOfferAttempt === 2 && "The second bidder did not complete the payment."}
              {order.paymentOfferAttempt >= 3 && "The previously offered bidder did not complete the payment."}
              Please choose an action:
            </p>
            <div className="space-y-2 text-left">
              {availableDecisionTypes.length > 0 ? (
                availableDecisionTypes.map(([apiKey, displayText]) => (
                  <label key={apiKey} className="flex items-center space-x-3 p-3 rounded-md hover:bg-gray-100 cursor-pointer border border-gray-200 has-[:checked]:bg-blue-50 has-[:checked]:border-blue-300">
                    <input
                      type="radio"
                      name="sellerDecision"
                      value={apiKey}
                      checked={selectedDecision === apiKey}
                      onChange={(e) => { setError(null); setSelectedDecision(e.target.value);}}
                      className="form-radio h-4 w-4 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-sm text-gray-700">{displayText}</span>
                  </label>
                ))
              ) : (
                <p className="text-sm text-gray-600 p-3 border border-gray-200 rounded-md bg-gray-50">
                  No specific follow-up actions available (e.g., no further bidders to offer to). You might need to cancel the sale if that's not an automatic option.
                  
                </p>
              )}
            </div>
            {error && <p className="text-xs text-red-500 mt-3 text-center">{error}</p>}
          </div>
          <div className="items-center px-4 py-3 mt-4 bg-gray-50 rounded-b-md">
            <button
              onClick={handleSubmitDecision}
              disabled={isProcessing || !selectedDecision || availableDecisionTypes.length === 0}
              className="px-4 py-2 bg-blue-500 text-white text-base font-medium rounded-md w-full shadow-sm hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:opacity-50 disabled:cursor-not-allowed"
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