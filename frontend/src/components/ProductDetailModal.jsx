// src/components/ProductDetailModal.jsx (New File)
import React from 'react';
import { FaEdit, FaTrashAlt, FaRocket } from 'react-icons/fa'; // Import icons

function ProductDetailModal({ isOpen, onClose, product, onEdit, onDelete, onStartAuction }) {
  if (!isOpen || !product) return null;

  // Stop propagation on modal content click if needed,
  // but usually handled by background overlay click
  const handleContentClick = (e) => {
    e.stopPropagation();
  };

  return (
    // Modal Overlay
    <div
      className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4"
      onClick={onClose} // Close when clicking overlay
    >
      {/* Modal Content */}
      <div
        className="bg-white p-6 rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto relative"
        onClick={handleContentClick} // Prevent overlay click from closing when clicking content
      >
         {/* Close Button */}
         <button onClick={onClose} className="absolute top-3 right-4 text-gray-600 hover:text-gray-900 text-2xl font-bold">&times;</button>

        <h2 className="text-2xl font-bold mb-4 border-b pb-2">{product.title}</h2>

        {/* Image Gallery/Carousel (Basic for now) */}
        <div className="mb-4">
           <h3 className="text-lg font-semibold mb-2">Images</h3>
           {product.imageUrls && product.imageUrls.length > 0 ? (
               <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-2">
                   {product.imageUrls.map((url, index) => (
                       <img key={index} src={url} alt={`${product.title} - Image ${index + 1}`} className="w-full h-auto object-cover rounded border" />
                   ))}
               </div>
           ) : ( <p className="text-sm text-gray-500">No images available.</p> )}
        </div>

        {/* Description */}
        <div className="mb-4">
           <h3 className="text-lg font-semibold mb-1">Description</h3>
           <p className="text-sm text-gray-700 whitespace-pre-wrap">{product.description}</p>
        </div>

        {/* Condition */}
         <div className="mb-4">
           <h3 className="text-lg font-semibold mb-1">Condition</h3>
           <p className="text-sm text-gray-700">{product.condition?.replace('_',' ') || 'N/A'}</p>
        </div>

        {/* Categories */}
        <div className="mb-6">
           <h3 className="text-lg font-semibold mb-1">Categories</h3>
           {product.categories && product.categories.length > 0 ? (
               <div className="flex flex-wrap gap-2">
                   {product.categories.map(cat => (
                       <span key={cat.id} className="bg-gray-200 text-gray-800 text-xs font-semibold px-2.5 py-0.5 rounded">
                           {cat.name}
                       </span>
                   ))}
               </div>
            ) : ( <p className="text-sm text-gray-500">No categories assigned.</p> )}
        </div>

        {/* Action Buttons */}
        <div className="mt-6 pt-4 border-t flex justify-end items-center space-x-3">
           <button onClick={(e) => {onClose(); onEdit(e, product)}} title="Edit Product" className="flex items-center text-sm px-4 py-2 bg-blue-100 text-blue-700 rounded hover:bg-blue-200">
               <FaEdit className="mr-1"/> Edit
           </button>
           <button onClick={(e) => {onClose(); onDelete(e, product.id)}} title="Delete Product" className="flex items-center text-sm px-4 py-2 bg-red-100 text-red-700 rounded hover:bg-red-200">
                <FaTrashAlt className="mr-1"/> Delete
           </button>
           <button onClick={(e) => {onClose(); onStartAuction(e, product.id)}} className="flex items-center text-sm bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2 px-4 rounded">
               <FaRocket className="mr-2"/> Start Auction
           </button>
        </div>

      </div>
    </div>
  );
}

export default ProductDetailModal;