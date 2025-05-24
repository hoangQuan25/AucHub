// src/components/ProductCard.jsx
import React from 'react';
// Link is removed as we don't want general card click to navigate in THIS context
import { FaEdit, FaTrashAlt, FaRocket } from 'react-icons/fa';

// Props:
// - product: The product object to display
// - isOwner: Boolean, true if the current user is the seller of this product
// - onEdit: Function to call when edit is clicked
// - onDelete: Function to call when delete is clicked
// - onStartAuction: Function to call when start auction is clicked
// - onClick: Function for general card click (to open the modal) <--- ADDED THIS TO PROPS
function ProductCard({ product, isOwner, onEdit, onDelete, onStartAuction, onClick }) {
  if (!product) return null;

  // This helper is good for action buttons to stop propagation
  // It ensures that clicking an action button doesn't also trigger the card's main onClick
  const handleOwnerAction = (e, actionCallback) => {
    e.stopPropagation(); // Prevent card's main onClick from firing
    if (actionCallback) {
      actionCallback(product); // Pass the product to the callback
    }
  };

  return (
    <div
      className="border rounded-lg bg-white shadow-md hover:shadow-xl transition-shadow flex flex-col overflow-hidden cursor-pointer"
      onClick={onClick} // Attach the main onClick (onViewDetails) here
      tabIndex={0} // Make it focusable for accessibility
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick && onClick(); }} // Keyboard accessibility
    >
      {/* Image section - NOT a Link anymore */}
      <div className="w-full h-48 bg-gray-200 overflow-hidden">
        <img
          src={(product.imageUrls && product.imageUrls.length > 0) ? product.imageUrls[0] : '/placeholder.png'}
          alt={product.title}
          className="w-full h-full object-cover transition-transform duration-300 ease-in-out group-hover:scale-105" // Use group-hover if main div is group
          loading="lazy"
          onError={(e) => { e.target.onerror = null; e.target.src = '/placeholder.png'; }}
        />
      </div>

      <div className="p-4 flex flex-col flex-grow">
        {/* Title section - NOT a Link anymore */}
        <h3
          className="font-semibold text-md text-gray-800 mb-1 truncate hover:text-indigo-600"
          title={product.title}
        >
          {product.title}
        </h3>

        <p className="text-xs text-gray-500 mb-3">
          Condition: {product.condition?.replace(/_/g, " ") || "N/A"}
        </p>

        {product.price && (
           <p className="text-lg font-bold text-green-600 mb-3">
             {/* Ensure product.price is a number for toLocaleString */}
             {typeof product.price === 'number' ? product.price.toLocaleString('vi-VN') : product.price} VNĐ
           </p>
        )}

        {/* Conditional Actions for Owner */}
        {isOwner && (
          <div className="mt-auto pt-3 border-t">
            <div className="flex justify-around items-center mb-2 text-xs text-gray-500">
              <span>Owner Actions:</span>
            </div>
            <div className="flex justify-center items-center space-x-3 mb-2">
              <button
                onClick={(e) => handleOwnerAction(e, onEdit)}
                title="Edit Product"
                className="text-gray-600 hover:text-blue-600 p-1 transition-colors"
                aria-label="Edit Product"
              >
                <FaEdit size="1.2em" />
              </button>
              <button
                onClick={(e) => handleOwnerAction(e, onDelete)}
                title="Delete Product"
                className="text-gray-600 hover:text-red-600 p-1 transition-colors"
                aria-label="Delete Product"
              >
                <FaTrashAlt size="1.2em" />
              </button>
            </div>
            <button
              onClick={(e) => handleOwnerAction(e, onStartAuction)}
              className="w-full text-sm bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2 px-3 rounded flex items-center justify-center transition-colors"
              aria-label="Start Auction for this product"
            >
              <FaRocket className="mr-2" /> Start Auction
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default ProductCard;