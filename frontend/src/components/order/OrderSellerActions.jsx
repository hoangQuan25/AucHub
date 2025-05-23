// src/components/order/OrderSellerActions.jsx
import React from 'react';
// Assuming FaCheckCircle is for delivered, adjust if needed
import { FaShippingFast, FaCheckCircle, FaExclamationTriangle, FaRedo, FaTimes, FaBoxOpen } from 'react-icons/fa';


function OrderSellerActions({
  order,
  deliveryDetails, // We need this to check deliveryStatus
  isProcessing,
  isMarkingAsShipped, // Specific loading state for marking as shipped
  onOpenSellerCancelModal,
  onOpenConfirmFulfillmentModal,
  isAwaitingSellerFulfillmentConfirmation,
  onOpenMarkAsShippedModal,
  onOpenMarkAsDeliveredModal, // New prop
  // onOpenReportIssueModal, // Will add this later
}) {
  if (!order) return null;

  const orderStatus = order.status;
  const currentDeliveryStatus = deliveryDetails?.deliveryStatus;

  const canMarkAsShipped = orderStatus === "AWAITING_SHIPMENT" && 
                           (!currentDeliveryStatus || // No delivery record yet (should be rare if logic is tight)
                            currentDeliveryStatus === "PENDING_PREPARATION" ||
                            currentDeliveryStatus === "READY_FOR_SHIPMENT");

  const canMarkAsDelivered = currentDeliveryStatus === "SHIPPED_IN_TRANSIT";

  const canReportIssue = currentDeliveryStatus && 
                         currentDeliveryStatus !== "DELIVERED" && 
                         currentDeliveryStatus !== "CANCELLED" &&
                         currentDeliveryStatus !== "COMPLETED_BY_BUYER" && // New statuses
                         currentDeliveryStatus !== "COMPLETED_AUTO";

  return (
    <div className="px-6 py-4 border-t border-gray-200 bg-gray-100 mb-6 rounded-lg shadow">
      <h3 className="text-md font-semibold text-gray-700 mb-3">
        Seller Actions: 
      </h3>
      <div className="flex flex-wrap items-center gap-3">
        {/* Order Lifecycle Actions (Pre-Delivery) */}
        {(orderStatus === "AWAITING_WINNER_PAYMENT" || orderStatus === "AWAITING_NEXT_BIDDER_PAYMENT") && (
          <button
            onClick={onOpenSellerCancelModal}
            disabled={isProcessing}
            className={`px-4 py-2 bg-red-500 text-white rounded text-sm font-medium transition-colors ${
              isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-600'
            }`}
          >
            Cancel Sale (Pre-Payment)
          </button>
        )}

        {isAwaitingSellerFulfillmentConfirmation && (
          <>
            <button
              onClick={onOpenConfirmFulfillmentModal}
              disabled={isProcessing}
              className={`px-4 py-2 bg-green-500 text-white rounded text-sm font-medium transition-colors ${
                isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-green-600'
              }`}
            >
              Confirm for Shipping
            </button>
            <button
              onClick={onOpenSellerCancelModal} 
              disabled={isProcessing}
              className={`px-4 py-2 bg-red-500 text-white rounded text-sm font-medium transition-colors ${
                isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-600'
              }`}
            >
              Cancel Order & Refund
            </button>
          </>
        )}

        {orderStatus === "AWAITING_SELLER_DECISION" && (
          <p className="text-sm text-yellow-800 bg-yellow-100 p-3 rounded w-full">
            This order requires your decision. Manage from "My Sales".
          </p>
        )}

        {/* Delivery Actions */}
        {canMarkAsShipped && (
          <button
            onClick={onOpenMarkAsShippedModal}
            disabled={isProcessing || isMarkingAsShipped} 
            className={`px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              (isProcessing || isMarkingAsShipped) 
                ? 'opacity-50 cursor-not-allowed' 
                : 'hover:bg-blue-700'
            }`}
          >
            <FaShippingFast /> Mark as Shipped
          </button>
        )}

        {canMarkAsDelivered && (
          <button
            onClick={onOpenMarkAsDeliveredModal}
            disabled={isProcessing} // We'll use a specific loading state for this too
            className={`px-4 py-2 bg-green-600 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-green-700'
            }`}
          >
            <FaCheckCircle /> Mark as Delivered
          </button>
        )}
        
        {/* {canReportIssue && (
          <button
            onClick={onOpenReportIssueModal}
            disabled={isProcessing}
            className={`px-4 py-2 bg-yellow-500 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-yellow-600'
            }`}
          >
            <FaExclamationTriangle /> Report Issue
          </button>
        )} */}

      </div>
    </div>
  );
}

export default OrderSellerActions;