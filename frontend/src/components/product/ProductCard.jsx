// src/components/ProductCard.jsx
import React from 'react';
import { FaEdit, FaTrash, FaGavel, FaCheckCircle } from 'react-icons/fa';

function ProductCard({ product, isOwner, onEdit, onDelete, onStartAuction, onClick }) {

  console.log("ProductCard Details:", { 
    title: product?.title, 
    isSold: product?.isSold, 
    isOwner: isOwner 
  });
  
  if (!product) return null;

 
  const handleStartAuctionClick = (e) => {
    e.stopPropagation(); // Prevent card click if button is clicked
    if (onStartAuction && !product.isSold) { // Ensure not sold before starting
      onStartAuction(product);
    }
  };

  const handleEditClick = (e) => {
    e.stopPropagation();
    if (onEdit) {
      onEdit(product);
    }
  };

  const handleDeleteClick = (e) => {
    e.stopPropagation();
    if (onDelete) {
      onDelete(product);
    }
  };

  return (
    <div
      className="border rounded-lg bg-white shadow-md hover:shadow-xl transition-shadow flex flex-col overflow-hidden cursor-pointer"
      onClick={onClick} // Attach the main onClick (onViewDetails) here
      tabIndex={0} // Make it focusable for accessibility
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick && onClick(); }} // Keyboard accessibility
    >
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
        <h3
          className="font-semibold text-md text-gray-800 mb-1 truncate hover:text-indigo-600"
          title={product.title}
        >
          {product.title}
        </h3>

        <p className="text-xs text-gray-500 mb-3">
          Condition: {product.condition?.replace(/_/g, " ") || "N/A"}
        </p>


        {/* Conditional Actions for Owner */}
        {isOwner && (
          <div className="mt-auto pt-3 border-t border-gray-200 space-y-2">
            {product.isSold ? (
              // --- DISPLAY "SOLD" BADGE ---
              <div className="flex items-center justify-center p-2 bg-green-100 border border-green-300 rounded-md">
                <FaCheckCircle className="text-green-600 mr-2" />
                <span className="text-md font-semibold text-green-700">SOLD</span>
              </div>
            ) : (
              // --- DISPLAY ACTION BUTTONS FOR UNSOLD ITEMS ---
              <div className="flex flex-col sm:flex-row gap-2">
                <button
                  onClick={handleStartAuctionClick}
                  disabled={product.isSold} // Should already be handled by conditional rendering
                  className="flex-1 whitespace-nowrap inline-flex items-center justify-center px-3 py-2 border border-transparent text-xs font-medium rounded-md shadow-sm text-white bg-purple-600 hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 disabled:bg-gray-300"
                >
                  <FaGavel className="mr-2 h-4 w-4" /> Start Auction
                </button>
                <div className="flex gap-2">
                    <button
                        onClick={handleEditClick}
                        className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 text-xs font-medium rounded-md shadow-sm text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                        title="Edit Product"
                    >
                        <FaEdit />
                    </button>
                    <button
                        onClick={handleDeleteClick}
                        className="flex-1 inline-flex items-center justify-center px-3 py-2 border border-gray-300 text-xs font-medium rounded-md shadow-sm text-red-600 bg-white hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                        title="Delete Product"
                    >
                        <FaTrash />
                    </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default ProductCard;