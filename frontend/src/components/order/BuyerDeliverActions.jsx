// src/components/order/BuyerDeliveryActions.jsx
import React from 'react';
import CountdownTimer from '../CountdownTimer'; // Make sure this path is correct

function BuyerDeliveryActions({
  deliveryDetails,
  onConfirmReceipt,
  onRequestReturn,
  isLoadingConfirm,
  isLoadingReturn,
}) {
  if (!deliveryDetails || deliveryDetails.deliveryStatus !== 'AWAITING_BUYER_CONFIRMATION') {
    return null;
  }

  let confirmationDeadline = null;
  let deadlineHasPassed = false;
  const now = new Date().getTime();

  if (deliveryDetails.deliveredAt) {
    const deliveredDate = new Date(deliveryDetails.deliveredAt);
    // Create a new Date object for the deadline to avoid modifying deliveredDate
    confirmationDeadline = new Date(deliveredDate);
    confirmationDeadline.setDate(deliveredDate.getDate() + 3); // 3-day window

    if (confirmationDeadline.getTime() < now) {
      deadlineHasPassed = true;
    }
  }

  return (
    <div className="my-6 p-6 bg-blue-50 rounded-lg shadow-md border border-blue-200">
      <h3 className="text-lg font-semibold text-blue-700 mb-2">Item Delivered - Action Required</h3>
      <p className="text-sm text-gray-700 mb-1">
        Your item (Order #{deliveryDetails.orderId?.substring(0,8)}) has been marked as delivered.
      </p>
      
      {confirmationDeadline && !deadlineHasPassed && (
        <div className="text-sm text-gray-600 mb-4">
          Please confirm you have received your item in good condition.
          <div className="mt-2 flex items-center justify-center sm:justify-start gap-2 text-blue-600 font-medium">
            <span>Time left to confirm:</span>
            <CountdownTimer endTimeMillis={confirmationDeadline.getTime()} />
          </div>
          <p className="mt-1 text-xs text-gray-500">
            If not confirmed by <strong className="text-blue-600">{new Date(confirmationDeadline).toLocaleDateString('vi-VN', { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}</strong>, 
            the order may be automatically completed.
          </p>
        </div>
      )}

      {confirmationDeadline && deadlineHasPassed && (
        <p className="text-sm text-orange-600 my-3 p-2 bg-orange-100 rounded-md">
          The confirmation window has ended. The system will process the order completion shortly.
        </p>
      )}

      {!confirmationDeadline && ( // Fallback if deliveredAt is missing for some reason
          <p className="text-sm text-gray-600 mb-4">
            Please confirm you have received your item in good condition within approximately 3 days.
            If not confirmed by then, the order may be automatically completed.
          </p>
      )}

      <div className="flex flex-col sm:flex-row sm:flex-wrap gap-3 mt-4">
        <button
          onClick={onConfirmReceipt}
          disabled={isLoadingConfirm || isLoadingReturn || deadlineHasPassed}
          className={`px-5 py-2.5 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm font-semibold shadow transition-colors flex items-center justify-center
            ${(isLoadingConfirm || isLoadingReturn || deadlineHasPassed) ? 'opacity-50 cursor-not-allowed' : ''}`}
        >
          {isLoadingConfirm ? 'Processing...' : "I've Received My Item"}
        </button>
        <button
          onClick={onRequestReturn}
          disabled={isLoadingConfirm || isLoadingReturn || deadlineHasPassed}
          className={`px-5 py-2.5 bg-orange-500 text-white rounded-md hover:bg-orange-600 text-sm font-semibold shadow transition-colors flex items-center justify-center
             ${(isLoadingConfirm || isLoadingReturn || deadlineHasPassed) ? 'opacity-50 cursor-not-allowed' : ''}`}
        >
          {isLoadingReturn ? 'Processing...' : "Request Return/Refund"}
        </button>
      </div>
    </div>
  );
}

export default BuyerDeliveryActions;