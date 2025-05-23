// src/components/order/OrderAlternateBidders.jsx
import React from 'react';

function OrderAlternateBidders({ order }) {
  if (!order || (!order.eligibleSecondBidderId && !order.eligibleThirdBidderId)) {
    return null;
  }

  return (
    <div className="mb-6 p-4 bg-white rounded-lg shadow border border-gray-200">
      <h2 className="text-xl font-semibold text-gray-800 mb-3">Alternate Bidders</h2>
      <ul className="text-sm space-y-1 text-gray-700">
        {order.eligibleSecondBidderId && (
          <li>
            2nd Bidder: <strong className="font-mono">{order.eligibleSecondBidderId.substring(0,8)}...</strong> —{" "}
            {order.eligibleSecondBidAmount?.toLocaleString("vi-VN")}{" "}
            {order.currency || 'VNĐ'}
          </li>
        )}
        {order.eligibleThirdBidderId && (
          <li>
            3rd Bidder: <strong className="font-mono">{order.eligibleThirdBidderId.substring(0,8)}...</strong> —{" "}
            {order.eligibleThirdBidAmount?.toLocaleString("vi-VN")}{" "}
            {order.currency || 'VNĐ'}
          </li>
        )}
      </ul>
    </div>
  );
}

export default OrderAlternateBidders;