import React from 'react';
// Assuming FaBoxOpen is used for the decision action icon
import { FaShippingFast, FaCheckCircle, FaExclamationTriangle, FaRedo, FaTimes, FaBoxOpen } from 'react-icons/fa';

function OrderSellerActions({
  order,
  deliveryDetails,
  isProcessing,
  isMarkingAsShipped,
  onOpenSellerCancelModal,
  onOpenConfirmFulfillmentModal,
  isAwaitingSellerFulfillmentConfirmation,
  onOpenMarkAsShippedModal,
  onOpenMarkAsDeliveredModal,
  onOpenSellerDecisionModal, // NEW: handler for seller decision
  canSellerMakeDecision      // NEW: flag to control decision button visibility
}) {
  if (!order) return null;

  const orderStatus = order.status;
  const currentDeliveryStatus = deliveryDetails?.deliveryStatus;

  const canMarkAsShipped =
    orderStatus === "AWAITING_SHIPMENT" &&
    (
      !currentDeliveryStatus ||
      currentDeliveryStatus === "PENDING_PREPARATION" ||
      currentDeliveryStatus === "READY_FOR_SHIPMENT"
    );

  const canMarkAsDelivered = currentDeliveryStatus === "SHIPPED_IN_TRANSIT";

  const canReportIssue =
    currentDeliveryStatus &&
    currentDeliveryStatus !== "DELIVERED" &&
    currentDeliveryStatus !== "CANCELLED" &&
    currentDeliveryStatus !== "COMPLETED_BY_BUYER" &&
    currentDeliveryStatus !== "COMPLETED_AUTO";

  return (
    <div className="px-6 py-4 border-t border-gray-200 bg-gray-100 mb-6 rounded-lg shadow">
      <h3 className="text-md font-semibold text-gray-700 mb-3">
        Seller Actions:
      </h3>
      <div className="flex flex-wrap items-center gap-3">
        {/* Pre-Payment Cancellation */}
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

        {/* Confirm Fulfillment or Cancel */}
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

        {/* Seller Decision */}
        {orderStatus === "AWAITING_SELLER_DECISION" && canSellerMakeDecision && (
          <button
            onClick={onOpenSellerDecisionModal}
            disabled={isProcessing}
            className={`px-4 py-2 bg-yellow-500 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-yellow-600'
            }`}
          >
            <FaBoxOpen /> Process Decision
          </button>
        )}

        {/* Delivery Actions */}
        {canMarkAsShipped && (
          <button
            onClick={onOpenMarkAsShippedModal}
            disabled={isProcessing || isMarkingAsShipped}
            className={`px-4 py-2 bg-blue-600 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              isProcessing || isMarkingAsShipped
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
            disabled={isProcessing}
            className={`px-4 py-2 bg-green-600 text-white rounded text-sm font-medium shadow transition-colors flex items-center gap-2 ${
              isProcessing ? 'opacity-50 cursor-not-allowed' : 'hover:bg-green-700'
            }`}
          >
            <FaCheckCircle /> Mark as Delivered
          </button>
        )}

        {/* Issue Reporting (commented out for future) */}
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
