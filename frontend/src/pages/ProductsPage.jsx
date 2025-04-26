// src/pages/ProductsPage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import apiClient from '../api/apiClient'; // Adjust path
import AddProductModal from '../components/AddProductModal'; // Adjust path

function ProductsPage() {
  const { keycloak, initialized } = useKeycloak();
  const [products, setProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Function to fetch products, wrapped in useCallback
  const fetchMyProducts = useCallback(async () => {
    if (initialized && keycloak.authenticated) {
        setIsLoading(true);
        setError('');
        console.log("Fetching seller's products...");
        try {
            // Assumes Gateway routes /api/products/my correctly and requires SELLER role
            const response = await apiClient.get('/products/my');
            console.log("Products received:", response.data);
            setProducts(response.data || []);
        } catch (err) {
            console.error("Failed to fetch products:", err);
            setError(err.response?.data?.message || "Could not load your products.");
            setProducts([]); // Clear products on error
        } finally {
            setIsLoading(false);
        }
    } else if (initialized) {
         // If initialized but not authenticated somehow
         setIsLoading(false);
         setError("Authentication required.");
         setProducts([]);
    }
  }, [initialized, keycloak.authenticated]); // Dependencies for the fetch function

  // Initial fetch when component mounts or auth changes
  useEffect(() => {
    fetchMyProducts();
  }, [fetchMyProducts]); // Depend on the memoized fetch function

  // Handler for when a new product is successfully added via the modal
  const handleAddProductSuccess = (newProductData) => {
    console.log('New product added via modal (data from modal):', newProductData);
    setIsModalOpen(false); // Close modal
    // Re-fetch the product list to show the new item
    fetchMyProducts();
    // Alternatively, you could try adding the newProductData (if backend returns full DTO)
    // to the 'products' state locally for slightly faster UI update:
    // setProducts(prev => [newProductData, ...prev]); // Add to beginning
  };

  const handleEditProduct = (productId) => {
      console.log("TODO: Implement Edit Product for ID:", productId);
      // Would likely open AddProductModal pre-filled with data fetched for this product ID
  }

  const handleStartAuction = (productId) => {
        console.log("TODO: Implement Start Auction for Product ID:", productId);
      // Would likely open a new "Start Auction" modal, passing the product ID
  }


  // --- Render Logic ---
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

      {isLoading && <div className="text-center p-4">Loading products...</div>}
      {error && <div className="text-center p-4 text-red-600">{error}</div>}

      {!isLoading && !error && products.length === 0 && (
         <div className="text-center p-10 border rounded bg-white shadow-sm">
             <p className="text-gray-500">You haven't added any products yet.</p>
        </div>
      )}

      {!isLoading && !error && products.length > 0 && (
         <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {/* Basic Product List - enhance later */}
            {products.map(product => (
                <div key={product.id} className="border rounded-lg p-4 bg-white shadow hover:shadow-md transition-shadow">
                    {/* Display first image as thumbnail */}
                    {product.imageUrls && product.imageUrls.length > 0 && (
                        <img
                            src={product.imageUrls[0]}
                            alt={product.title}
                            className="w-full h-40 object-cover rounded mb-2"
                            loading="lazy" // Lazy load images
                         />
                    )}
                     <h3 className="font-semibold text-lg mb-1 truncate" title={product.title}>{product.title}</h3>
                     {/* Display condition & categories later */}
                     {/* <p className="text-sm text-gray-600">Condition: {product.condition}</p> */}
                     {/* <p className="text-xs text-gray-500">Categories: {product.categories?.map(c => c.name).join(', ')}</p> */}
                     {/* Add status later (Available, In Auction, Sold) */}
                     <div className="mt-3 pt-3 border-t flex justify-around space-x-1">
                          <button onClick={() => handleEditProduct(product.id)} className="text-xs text-blue-600 hover:underline">Edit</button>
                          <button onClick={() => handleStartAuction(product.id)} className="text-xs text-purple-600 hover:underline">Start Auction</button>
                          {/* Add Delete button later */}
                     </div>
                </div>
            ))}
         </div>
      )}

      {/* The Add Product Modal */}
      <AddProductModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleAddProductSuccess} // Pass success handler to refetch list
      />
    </div>
  );
}

export default ProductsPage;