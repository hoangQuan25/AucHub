// src/components/ProductDetailModal.jsx
import React from 'react';
import { FaEdit, FaTrashAlt, FaRocket, FaTimes } from 'react-icons/fa';

function ProductDetailModal({
  isOpen,
  onClose,
  product,
  isOwner,
  onEdit,       // Expects onEdit(product)
  onDelete,     // Expects onDelete(product)
  onStartAuction // Expects onStartAuction(product)
}) {
  if (!isOpen || !product) return null;

  const handleContentClick = (e) => {
    e.stopPropagation();
  };

  return (
    // Modal Overlay
    <div
      className="fixed inset-0 bg-black bg-opacity-70 flex justify-center items-center z-50 p-4 transition-opacity duration-300 ease-in-out"
      onClick={onClose}
    >
      {/* Modal Content */}
      <div
        className="bg-white p-5 sm:p-6 md:p-8 rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto relative transform transition-all duration-300 ease-in-out scale-95 opacity-0 animate-modalFadeInScale"
        onClick={handleContentClick}
      >
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-500 hover:text-gray-800 transition-colors z-10"
          aria-label="Close modal"
        >
          <FaTimes size="1.5em" />
        </button>

        <h2 className="text-2xl sm:text-3xl font-bold mb-4 border-b pb-3 pr-10">{product.title}</h2>

        {/* Image Gallery Section ... */}
        <div className="mb-5">
          <h3 className="text-lg font-semibold mb-2 text-gray-700">Images</h3>
          {product.imageUrls && product.imageUrls.length > 0 ? (
            <div className={`grid gap-2 ${product.imageUrls.length === 1 ? 'grid-cols-1' : 'grid-cols-2 sm:grid-cols-3'}`}>
              {product.imageUrls.map((url, index) => (
                <div key={index} className="aspect-w-1 aspect-h-1 bg-gray-100 rounded overflow-hidden">
                   <img
                    src={url}
                    alt={`${product.title} - Image ${index + 1}`}
                    className="w-full h-full object-cover rounded border border-gray-200 hover:opacity-90 transition-opacity"
                    loading="lazy"
                  />
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">No images available.</p>
          )}
        </div>

        {/* Description Section ... */}
        <div className="mb-5">
          <h3 className="text-lg font-semibold mb-1 text-gray-700">Description</h3>
          <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
            {product.description || <span className="italic text-gray-500">No description provided.</span>}
          </p>
        </div>

        {/* Condition Section ... */}
        <div className="mb-5">
          <h3 className="text-lg font-semibold mb-1 text-gray-700">Condition</h3>
          <p className="text-sm text-gray-700">
            {product.condition?.replace('_', ' ') || <span className="italic text-gray-500">Not specified.</span>}
          </p>
        </div>

        {/* Categories Section ... */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold mb-2 text-gray-700">Categories</h3>
          {product.categories && product.categories.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {product.categories.map(cat => (
                <span
                  key={cat.id}
                  className="bg-gray-200 text-gray-800 text-xs font-semibold px-2.5 py-1 rounded-full"
                >
                  {cat.name}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500 italic">No categories assigned.</p>
          )}
        </div>


        {/* Action Buttons - Updated onClick handlers */}
        <div className="mt-8 pt-5 border-t border-gray-200 flex flex-col sm:flex-row justify-end items-center space-y-3 sm:space-y-0 sm:space-x-3">
          {isOwner && (
            <>
              {onEdit && (
                <button
                  onClick={(e) => {
                    e.stopPropagation(); // Optional: if needed, though modal content click handles outer propagation
                    onClose();
                    onEdit(product); // Call with product ONLY
                  }}
                  title="Edit Product"
                  className="w-full sm:w-auto flex items-center justify-center text-sm px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-md shadow-sm transition-colors"
                >
                  <FaEdit className="mr-2" /> Edit
                </button>
              )}
              {onDelete && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onClose();
                    onDelete(product); // Call with product ONLY
                  }}
                  title="Delete Product"
                  className="w-full sm:w-auto flex items-center justify-center text-sm px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-md shadow-sm transition-colors"
                >
                  <FaTrashAlt className="mr-2" /> Delete
                </button>
              )}
              {onStartAuction && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onClose();
                    onStartAuction(product); // Call with product ONLY
                  }}
                  className="w-full sm:w-auto flex items-center justify-center text-sm bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2 px-4 rounded-md shadow-sm transition-colors"
                >
                  <FaRocket className="mr-2" /> Start Auction
                </button>
              )}
            </>
          )}
          {!isOwner && product.isAuctionable && !product.activeAuctionId && (
             <button /* ... More Info button ... */ > More Info </button>
          )}
           <button
            onClick={onClose} // This one is fine as is
            className="w-full sm:w-auto text-sm px-4 py-2 bg-gray-200 text-gray-700 hover:bg-gray-300 rounded-md transition-colors"
            aria-label="Close details"
          >
            Close
          </button>
        </div>
        <style>{`
          @keyframes fadeInScale {
            0% { opacity: 0; transform: scale(0.95); }
            100% { opacity: 1; transform: scale(1); }
          }
          .animate-modalFadeInScale {
            animation: fadeInScale 0.3s ease-out forwards;
          }
        `}</style>
      </div>
    </div>
  );
}

export default ProductDetailModal;