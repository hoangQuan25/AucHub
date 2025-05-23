// src/components/order/OrderInfoBanners.jsx
import React from 'react';
import { orderStatusMap } from '../../constants/orderConstants'; // Adjust path

function OrderInfoBanners({ order, isAwaitingSellerFulfillmentConfirmation }) {
  if (!order) return null;

  return (
    <>
      {order.status === "PAYMENT_SUCCESSFUL" && !isAwaitingSellerFulfillmentConfirmation && (
        <div className="my-6 p-4 bg-green-50 text-green-700 rounded-lg border border-green-200 shadow-sm">
          <p className="text-sm font-semibold">
            Payment successful! Awaiting seller fulfillment confirmation before shipping can be arranged.
          </p>
        </div>
      )}

      {order.status?.includes("CANCELLED") && (
        <div className="my-6 p-4 bg-red-50 text-red-700 rounded-lg border border-red-200 shadow-sm">
          <p className="text-sm font-semibold">
            This order has been cancelled. (Status:{" "}
            {orderStatusMap[order.status] || order.status})
          </p>
        </div>
      )}
      {/* Add more banners as needed for other statuses, e.g., AWAITING_SHIPMENT */}
      {order.status === "AWAITING_SHIPMENT" && (
        <div className="my-6 p-4 bg-blue-50 text-blue-700 rounded-lg border border-blue-200 shadow-sm">
          <p className="text-sm font-semibold">
            This order is awaiting shipment by the seller.
          </p>
        </div>
      )}
    </>
  );
}

export default OrderInfoBanners;