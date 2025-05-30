// src/components/order/OrderAlternateBidders.jsx
import React from 'react';

function OrderAlternateBidders({ order }) {
  if (!order || (!order.eligibleSecondBidderId && !order.eligibleThirdBidderId)) {
    return null;
  }

  return (
    <div className="mb-6 p-4 bg-white rounded-lg shadow border border-indigo-200">
      <h2 className="text-xl font-bold text-indigo-700 mb-3 flex items-center gap-2">
        <span className="inline-block w-2 h-2 bg-indigo-500 rounded-full mr-2" />
        Alternate Bidders
      </h2>
      <ul className="text-base space-y-2">
        {order.eligibleSecondBidderId && (
          <li className="flex items-center gap-3">
            <span className="inline-block bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full text-xs font-semibold">
              2nd Bidder
            </span>
            <span className="font-semibold text-gray-800">
              {order.eligibleSecondBidAmount?.toLocaleString("vi-VN")} {order.currency || 'VNĐ'}
            </span>
          </li>
        )}
        {order.eligibleThirdBidderId && (
          <li className="flex items-center gap-3">
            <span className="inline-block bg-purple-100 text-purple-700 px-2 py-0.5 rounded-full text-xs font-semibold">
              3rd Bidder
            </span>
            <span className="font-semibold text-gray-800">
              {order.eligibleThirdBidAmount?.toLocaleString("vi-VN")} {order.currency || 'VNĐ'}
            </span>
          </li>
        )}
      </ul>
    </div>
  );
}

export default OrderAlternateBidders;