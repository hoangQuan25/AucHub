// src/components/order/OrderHeader.jsx
import React from 'react';
import { orderStatusMap } from '../../constants/orderConstants'; // Adjust path if necessary

function OrderHeader({ order }) {
  if (!order) {
    return (
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">
        Loading Order Details...
      </h1>
    );
  }

  return (
    <>
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">
        Order Details #{(order.id || "N/A").toString().substring(0, 8)}
      </h1>
      <ul className="mb-6 space-y-1 text-sm text-gray-700 bg-white p-4 rounded-lg shadow border border-gray-200">
        <li>
          <strong>Auction ID:</strong>{" "}
          <span className="font-mono">{order.auctionId?.substring(0, 8) || "N/A"}</span>
        </li>
        <li>
          <strong>Seller:</strong> {order.sellerUsernameSnapshot} ({order.sellerId})
        </li>
        <li>
          <strong>Status:</strong>{" "}
          <span className="font-semibold">{orderStatusMap[order.status] || order.status}</span>
        </li>
        <li>
          <strong>Auction Type:</strong> {order.auctionType}
        </li>
        <li>
          <strong>Created At:</strong>{" "}
          {new Date(order.createdAt).toLocaleString('vi-VN')}
        </li>
        <li>
          <strong>Updated At:</strong>{" "}
          {new Date(order.updatedAt).toLocaleString('vi-VN')}
        </li>
      </ul>
    </>
  );
}

export default OrderHeader;