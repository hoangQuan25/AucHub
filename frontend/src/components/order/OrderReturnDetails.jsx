// src/components/order/OrderReturnDetails.jsx
import React from "react";
import { FaUndo, FaInfoCircle, FaArchive } from "react-icons/fa";
import { orderStatusMap } from "../../constants/orderConstants";

function OrderReturnDetails({
  order,
  deliveryDetails,
  isSeller,
  onConfirmReturnReceived,
  isProcessing,
}) {
  // Ensure we have the necessary data to display
  if (
    !deliveryDetails ||
    !deliveryDetails.deliveryStatus.startsWith("RETURN_")
  ) {
    // Also check order status to catch final states like ORDER_RETURNED
    if (
      !order ||
      ![
        "ORDER_RETURNED",
        "RETURN_APPROVED_BY_SELLER",
        "REFUND_FAILED",
      ].includes(order.status)
    ) {
      return null;
    }
  }

  // REFACTORED: Get all return info cleanly from deliveryDetails
  const {
    returnReason = "Not specified",
    returnComments = "",
    returnCourier = "Not specified",
    returnTrackingNumber = "Not specified",
  } = deliveryDetails;

  let returnImageUrls = [];
  if (deliveryDetails.returnImageUrls) {
    try {
      returnImageUrls = JSON.parse(deliveryDetails.returnImageUrls);
      console.log("Parsed returnImageUrls:", returnImageUrls);
    } catch (e) {
      console.error("Failed to parse returnImageUrls:", e);
    }
  }

  // The Order Status is the primary source of truth for the process stage
  const statusText =
    orderStatusMap[order.status] || order.status.replace(/_/g, " ");

  let infoMessage = "";
  // NEW: Seller can confirm receipt only when the item has been "requested" by buyer and is presumably on its way back.
  const canSellerConfirmReceipt =
    isSeller && deliveryDetails.deliveryStatus === "RETURN_REQUESTED_BY_BUYER";

  // REFACTORED: Logic for info messages is now simpler and more direct.
  if (isSeller) {
    switch (order.status) {
      case "AWAITING_SHIPMENT": // This state occurs if buyer requested return
        infoMessage =
          "The buyer has requested to return the item. Once you receive it, confirm below to trigger the refund.";
        break;
      case "RETURN_APPROVED_BY_SELLER":
        infoMessage =
          "You've confirmed receipt. The refund to the buyer is now being processed by the system.";
        break;
      case "ORDER_RETURNED":
        infoMessage =
          "The return is complete and the buyer has been successfully refunded.";
        break;
      case "REFUND_FAILED":
        infoMessage =
          "CRITICAL: The refund to the buyer failed. Please contact support to resolve this manually.";
        break;
      default:
        infoMessage = "A return is in progress.";
    }
  } else {
    // Buyer's view
    switch (order.status) {
      case "AWAITING_SHIPMENT":
        infoMessage =
          "Your return request is submitted. The seller is awaiting the item. Please ensure you have shipped it.";
        break;
      case "RETURN_APPROVED_BY_SELLER":
        infoMessage =
          "The seller has received your item and the refund is being processed. This may take a few business days.";
        break;
      case "ORDER_RETURNED":
        infoMessage = "Your return is complete and you have been refunded.";
        break;
      case "REFUND_FAILED":
        infoMessage =
          "There was an issue processing your refund. Please contact support for assistance.";
        break;
      default:
        infoMessage = "Your return is in progress.";
    }
  }

  return (
    <div className="my-6 p-6 bg-orange-50 rounded-lg shadow border border-orange-200">
      <h2 className="text-xl font-semibold text-orange-800 mb-4 flex items-center gap-2">
        <FaUndo /> Return In Progress
      </h2>
      <div className="space-y-3 text-sm">
        <div className="mt-2 p-3 bg-blue-50 text-blue-700 text-xs rounded-md border border-blue-200 flex items-start gap-2">
          <FaInfoCircle
            className="text-blue-500 flex-shrink-0 mt-0.5"
            size={16}
          />
          <p>{infoMessage}</p>
        </div>

        <div className="pt-3 border-t border-orange-100">
          <h4 className="text-md font-semibold text-gray-700 mb-2">
            Buyer's Return Details:
          </h4>
          {/* ... your existing layout for displaying reason, courier, etc. is perfect ... */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4 gap-y-2">
            <div>
              <span className="text-gray-500 font-medium">Reason:</span>
              <p className="text-gray-800 pl-2">{returnReason}</p>
            </div>
            <div>
              <span className="text-gray-500 font-medium">Return Courier:</span>
              <p className="text-gray-800 pl-2">{returnCourier}</p>
            </div>
            <div>
              <span className="text-gray-500 font-medium">
                Return Tracking #:
              </span>
              <p className="text-gray-800 pl-2 font-mono">
                {returnTrackingNumber}
              </p>
            </div>
            <div>
              <span className="text-gray-500 font-medium">Return Comment:</span>
              <p className="text-gray-800 pl-2">
                {returnComments || (
                  <span className="italic text-gray-400">
                    (No comment provided)
                  </span>
                )}
              </p>
            </div>
          </div>
        </div>

        {returnImageUrls.length > 0 && (
          <div className="pt-3 border-t border-orange-100">
            <h4 className="text-md font-semibold text-gray-700 mb-2">
              Return Photos:
            </h4>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {returnImageUrls.map((url, idx) => (
                <a
                  key={idx}
                  href={url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block"
                >
                  <img
                    src={url}
                    alt={`Return evidence ${idx + 1}`}
                    className="w-full h-32 object-cover rounded border border-gray-200 shadow hover:scale-105 transition-transform"
                  />
                </a>
              ))}
            </div>
          </div>
        )}
      </div>

      {canSellerConfirmReceipt && (
        <div className="mt-6 pt-4 border-t border-orange-200">
          <h3 className="text-md font-semibold text-gray-700 mb-2">
            Seller Action Required
          </h3>
          <p className="text-xs text-gray-600 mb-3">
            Confirm you have received the returned item. This is the final step
            and will trigger the automated refund to the buyer.
          </p>
          <div className="flex justify-center">
            <button
              onClick={onConfirmReturnReceived}
              disabled={isProcessing}
              className={`px-4 py-2.5 bg-green-600 text-white rounded text-sm font-semibold shadow transition-colors flex items-center justify-center gap-2 ${
                isProcessing
                  ? "opacity-50 cursor-not-allowed"
                  : "hover:bg-green-700"
              }`}
              style={{ minWidth: 0, width: "auto" }}
            >
              <FaArchive className="mr-1" /> Confirm Item Received & Initiate
              Refund
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default OrderReturnDetails;
