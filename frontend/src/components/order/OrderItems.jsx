// src/components/order/OrderItems.jsx
import React from 'react';

function OrderItems({ items, currency }) {
  if (!items || items.length === 0) {
    return <div className="mb-6 p-4 bg-gray-50 rounded border">No items in this order.</div>;
  }

  return (
    <div className="mb-6">
      <h2 className="text-xl font-semibold text-gray-800 mb-3">Items</h2>
      <div className="space-y-4">
        {items.map((item) => (
          <div
            key={item.productId || item.id} // Use a unique key
            className="flex gap-4 items-start bg-white p-4 rounded-lg shadow border border-gray-200"
          >
            <img
              src={item.imageUrl || '/placeholder.png'} // Ensure you have a placeholder
              alt={item.title}
              className="w-24 h-24 object-cover rounded border flex-shrink-0"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = '/placeholder.png';
              }}
            />
            <div className="flex-grow">
              <h3 className="font-semibold text-gray-900 mb-1">{item.title}</h3>
              {item.variation && <p className="text-xs text-gray-500">{item.variation}</p>}
              <p className="text-sm text-gray-600">Quantity: {item.quantity}</p>
              <p className="text-sm font-medium text-gray-800">
                Unit Price: {item.price?.toLocaleString("vi-VN")}{" "}
                {currency || "VNĐ"}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default OrderItems;