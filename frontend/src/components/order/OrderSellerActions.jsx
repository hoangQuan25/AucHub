import React from "react";
import {
  FaShippingFast,
  FaCheckCircle,
  FaExclamationTriangle,
  FaRedo,
  FaTimes,
  FaBoxOpen,
} from "react-icons/fa";

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
  onOpenSellerDecisionModal,
  canSellerMakeDecision,
}) {
  if (!order) return null;

  const orderStatus = order.status;
  const currentDeliveryStatus = deliveryDetails?.deliveryStatus;

  const canMarkAsShipped =
    orderStatus === "AWAITING_SHIPMENT" &&
    (!currentDeliveryStatus ||
      currentDeliveryStatus === "PENDING_PREPARATION" ||
      currentDeliveryStatus === "READY_FOR_SHIPMENT");

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
      <div className="flex flex-col sm:flex-row flex-wrap gap-3 justify-center">
        {/* Pre-Payment Cancellation */}
        {(orderStatus === "AWAITING_WINNER_PAYMENT" ||
          orderStatus === "AWAITING_NEXT_BIDDER_PAYMENT") && (
          <button
            onClick={onOpenSellerCancelModal}
            disabled={isProcessing}
            className={`flex-1 min-w-[180px] px-5 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 text-sm font-semibold shadow transition-colors text-center ${
              isProcessing ? "opacity-50 cursor-not-allowed" : ""
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
              className={`flex-1 min-w-[180px] px-5 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm font-semibold shadow transition-colors text-center ${
                isProcessing ? "opacity-50 cursor-not-allowed" : ""
              }`}
            >
              Confirm for Shipping
            </button>
            <button
              onClick={onOpenSellerCancelModal}
              disabled={isProcessing}
              className={`flex-1 min-w-[180px] px-5 py-2.5 bg-red-500 text-white rounded-lg hover:bg-red-600 text-sm font-semibold shadow transition-colors text-center ${
                isProcessing ? "opacity-50 cursor-not-allowed" : ""
              }`}
            >
              Cancel Order & Refund
            </button>
          </>
        )}

        {/* Seller Decision */}
        {orderStatus === "AWAITING_SELLER_DECISION" &&
          canSellerMakeDecision && (
            <button
              onClick={onOpenSellerDecisionModal}
              disabled={isProcessing}
              className={`flex-1 min-w-[180px] px-5 py-2.5 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 text-sm font-semibold shadow transition-colors flex items-center justify-center gap-2 ${
                isProcessing ? "opacity-50 cursor-not-allowed" : ""
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
            className={`flex-1 min-w-[180px] px-5 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm font-semibold shadow transition-colors flex items-center justify-center gap-2 text-center ${
              isProcessing || isMarkingAsShipped
                ? "opacity-50 cursor-not-allowed"
                : ""
            }`}
          >
            <FaShippingFast /> Mark as Shipped
          </button>
        )}

        {canMarkAsDelivered && (
          <button
            onClick={onOpenMarkAsDeliveredModal}
            disabled={isProcessing}
            className={`flex-1 min-w-[180px] px-5 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 text-sm font-semibold shadow transition-colors flex items-center justify-center gap-2 text-center ${
              isProcessing ? "opacity-50 cursor-not-allowed" : ""
            }`}
          >
            <FaCheckCircle /> Mark as Delivered
          </button>
        )}
      </div>
    </div>
  );
}

export default OrderSellerActions;
