// src/pages/ProductsPage.jsx
import React, { useState } from 'react';
import AddProductModal from '../components/AddProductModal'; // We'll create this next

function ProductsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleAddProductSuccess = (newProductData) => {
    console.log('Product Added (Mock):', newProductData);
    // Later: You would refresh the product list here
    setIsModalOpen(false); // Close modal on success
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold">Your Products</h2>
        <button
          onClick={() => setIsModalOpen(true)}
          className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow"
        >
          + Add New Product
        </button>
      </div>

      {/* Placeholder for the list of products */}
      <div className="p-10 border rounded bg-white shadow-sm">
        <p className="text-gray-500">(List of seller's products - View, Edit, Start Auction buttons - will appear here)</p>
      </div>

      {/* The Add Product Modal */}
      <AddProductModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleAddProductSuccess} // Pass success handler
      />
    </div>
  );
}

export default ProductsPage;