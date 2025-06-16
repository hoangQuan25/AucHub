// src/components/order/OrderPaymentDetails.jsx
import React from 'react';
import CountdownTimer from '../CountdownTimer'; // Adjust path
import { FaCreditCard, FaHistory } from 'react-icons/fa';

function OrderPaymentDetails({
  order,
  userProfile,
  currentPayerBidAmount,
  buyerPremiumAmount,
  totalAmountDue,
  isProcessing,
  paymentProcessing,
  onDeclinePurchase, 
  onInitiatePayment, 
  onOpenPaymentModal,
}) {
  if (!order || !order.paymentDeadline) return null;

  return (
    <div className="mb-6 p-6 bg-orange-50 rounded-lg border border-orange-200 shadow-sm">
      <h2 className="text-xl font-semibold text-gray-800 mb-4">Payment Required</h2>
      <div className="space-y-2 mb-4 text-sm text-gray-700">
        {currentPayerBidAmount > 0 || totalAmountDue > 0 ? ( // Show breakdown if amounts are valid
          <>
            <div className="flex justify-between">
              <span>Your Bid Amount:</span>
              <span className="font-medium">{currentPayerBidAmount.toLocaleString("vi-VN")} {order.currency || "VNĐ"}</span>
            </div>
            <div className="flex justify-between">
              <span>Buyer's Premium (10%):</span>
              <span className="font-medium">{buyerPremiumAmount.toLocaleString("vi-VN")} {order.currency || "VNĐ"}</span>
            </div>
            <hr className="my-1 border-orange-200"/>
            <div className="flex justify-between text-lg">
              <strong className="text-gray-800">Total Amount Due:</strong>
              <strong className="text-red-600">{totalAmountDue.toLocaleString("vi-VN")} {order.currency || "VNĐ"}</strong>
            </div>
          </>
        ) : (
          <div className="flex justify-between text-lg">
            <strong className="text-gray-800">Total Amount Due:</strong>
            <strong className="text-red-600">{totalAmountDue.toLocaleString("vi-VN")} {order.currency || "VNĐ"}</strong>
          </div>
        )}
      </div>
      <div className="mb-4 text-sm text-orange-700 font-medium flex items-center justify-center gap-1">
        <span className="font-semibold">Payment Deadline:</span>
        <CountdownTimer endTimeMillis={new Date(order.paymentDeadline).getTime()} />
      </div>

      <div className="flex flex-col space-y-3 sm:flex-row sm:space-y-0 sm:space-x-3 sm:justify-end">
        <button
          onClick={onDeclinePurchase}
          disabled={isProcessing || paymentProcessing}
          className="px-5 py-2.5 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 text-sm font-semibold shadow-sm transition-colors"
        >
          Decline Purchase
        </button>

        {userProfile?.hasDefaultPaymentMethod && userProfile.defaultCardLast4 ? (
          <button
            onClick={() => onInitiatePayment(true)} // true for useSavedCard
            disabled={isProcessing || paymentProcessing}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-teal-600 text-white rounded-lg hover:bg-teal-700 text-sm font-semibold shadow transition-colors"
          >
            <FaHistory /> Pay with {userProfile.defaultCardBrand} ****{userProfile.defaultCardLast4}
          </button>
        ) : (
          <button
            onClick={onOpenPaymentModal} // This will open the modal defined in OrderDetailPage
            disabled={isProcessing || paymentProcessing}
            className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-teal-600 text-white rounded-lg hover:bg-teal-700 text-sm font-semibold shadow transition-colors"
          >
            <FaCreditCard /> Make Payment
          </button>
        )}

        {userProfile?.hasDefaultPaymentMethod && (
            <button
              onClick={() => onInitiatePayment(false)} // false for new card
              disabled={isProcessing || paymentProcessing}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-slate-600 text-white rounded-lg hover:bg-slate-700 text-sm font-semibold shadow transition-colors text-center"
            >
              Use a Different Card
            </button>
        )}
      </div>
    </div>
  );
}

export default OrderPaymentDetails;