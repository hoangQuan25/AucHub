import React from 'react';
import { FaEdit, FaTrash, FaGavel, FaCheckCircle, FaHourglassHalf, FaLock } from 'react-icons/fa';

function ProductCard({ product, isOwner, onEdit, onDelete, onStartAuction, onClick }) {
  if (!product) return null;

  const handleActionClick = (e, action) => {
    e.stopPropagation(); // Prevent card's main onClick from firing
    if (action) {
      action(product);
    }
  };

  const renderStatusBadge = () => {
    switch (product.status) {
      case 'SOLD':
        return (
          <div className="flex items-center justify-center p-2 bg-green-100 border border-green-300 rounded-md">
            <FaCheckCircle className="text-green-600 mr-2" />
            <span className="text-md font-semibold text-green-700">SOLD</span>
          </div>
        );
      case 'IN_AUCTION':
        return (
          <div className="flex items-center justify-center p-2 bg-blue-100 border border-blue-300 rounded-md">
            <FaLock className="text-blue-600 mr-2" />
            <span className="text-md font-semibold text-blue-700">IN AUCTION</span>
          </div>
        );
      case 'AWAITING_COMPLETION':
        return (
          <div className="flex items-center justify-center p-2 bg-yellow-100 border border-yellow-400 rounded-md">
            <FaHourglassHalf className="text-yellow-600 mr-2" />
            <span className="text-md font-semibold text-yellow-700">PROCESSING</span>
          </div>
        );
      default: // AVAILABLE or any other status
        return (
          <div className="flex flex-col sm:flex-row gap-2">
            <button
              onClick={(e) => handleActionClick(e, onStartAuction)}
              className="flex-1 whitespace-nowrap inline-flex items-center justify-center px-3 py-2 border border-transparent text-xs font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500"
            >
              <FaGavel className="mr-2 h-4 w-4" /> Start Auction
            </button>
            <div className="flex gap-2">
              <button
                onClick={(e) => handleActionClick(e, onEdit)}
                className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 text-xs font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                title="Edit Product"
              >
                <FaEdit />
              </button>
              <button
                onClick={(e) => handleActionClick(e, onDelete)}
                className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 text-xs font-medium rounded-md shadow-sm text-red-600 bg-white hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                title="Delete Product"
              >
                <FaTrash />
              </button>
            </div>
          </div>
        );
    }
  };

  return (
    <div
      className="border rounded-lg bg-white shadow-md hover:shadow-xl transition-shadow flex flex-col overflow-hidden group"
      onClick={onClick} // Card is clickable only if available
      tabIndex={0}
      onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && product.status === 'AVAILABLE') onClick && onClick(); }}
    >
      <div className="relative w-full h-48 bg-gray-200 overflow-hidden">
        <img
          src={(product.imageUrls && product.imageUrls.length > 0) ? product.imageUrls[0] : '/placeholder.png'}
          alt={product.title}
          className="w-full h-full object-cover transition-transform duration-300 ease-in-out group-hover:scale-105"
          loading="lazy"
        />
        {/* Add an overlay if the product is not available for a subtle visual cue */}
        {product.status !== 'AVAILABLE' && (
          <div className="absolute inset-0 bg-gray-500 bg-opacity-20"></div>
        )}
      </div>

      <div className="p-4 flex flex-col flex-grow">
        <h3 className="font-semibold text-md text-gray-800 mb-1 truncate hover:text-indigo-600" title={product.title}>
          {product.title}
        </h3>
        <p className="text-xs text-gray-500 mb-3">
          Condition: {product.condition?.replace(/_/g, " ") || "N/A"}
        </p>

        {isOwner && (
          <div className="mt-auto pt-3 border-t border-gray-200">
            {renderStatusBadge()}
          </div>
        )}
      </div>
    </div>
  );
}

export default ProductCard;