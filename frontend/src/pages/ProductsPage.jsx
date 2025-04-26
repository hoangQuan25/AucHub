// src/pages/ProductsPage.jsx (Layout with Hierarchical Filter Sidebar & Actions)
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import apiClient from '../api/apiClient';
import AddProductModal from '../components/AddProductModal';
import CategorySelector from '../components/CategorySelector'; // Component that handles hierarchy
import ProductDetailModal from '../components/ProductDetailModal'; // Assuming this exists
import { FaEdit, FaTrashAlt, FaRocket } from 'react-icons/fa'; // Icons for actions

function ProductsPage() {
  const { keycloak, initialized } = useKeycloak();

  // Product State
  const [products, setProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Add/Edit Modal State
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  // Details Modal State
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedProductForDetail, setSelectedProductForDetail] = useState(null);

  // Category State (for filtering)
  const [allCategories, setAllCategories] = useState([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [categoryError, setCategoryError] = useState('');
  const [filterCategoryIds, setFilterCategoryIds] = useState(new Set()); // Store selected filter IDs

  // --- Data Fetching Callbacks ---
  const fetchMyProducts = useCallback(async (filterIds = filterCategoryIds) => {
    // Check if initialized and authenticated before fetching
    if (!(initialized && keycloak.authenticated)) {
        setError("Authentication required to view products.");
        setIsLoading(false);
        setProducts([]);
        return;
    }
    setIsLoading(true); setError('');
    console.log("Fetching seller's products with category filter:", filterIds);
    try {
        // TODO: Modify backend later to accept category filters: /api/products/my?categoryIds=1,2,3
        const response = await apiClient.get('/products/my'); // Use correct API path
        setProducts(response.data || []);
    } catch (err) {
        console.error("Failed to fetch products:", err);
        setError(err.response?.data?.message || "Could not load your products.");
        setProducts([]);
    } finally { setIsLoading(false); }
  }, [initialized, keycloak.authenticated]); // Dependency check

  const fetchAllCategories = useCallback(async () => {
    if (!(initialized && keycloak.authenticated)) return; // Don't fetch if not logged in
    setCategoryLoading(true); setCategoryError('');
    try {
        const response = await apiClient.get('/products/categories'); // Ensure this endpoint is correct
        setAllCategories(response.data || []);
    } catch (err) {
        console.error("Failed to fetch categories:", err);
        setCategoryError('Could not load categories for filtering.'); setAllCategories([]);
    } finally { setCategoryLoading(false); }
  }, [initialized, keycloak.authenticated]);

  // Initial data fetching
  useEffect(() => {
    // Only run fetches if keycloak is initialized
    if (initialized) {
      fetchMyProducts(filterCategoryIds);
      fetchAllCategories();
    }
  }, [initialized, fetchMyProducts, fetchAllCategories]); // Add initialized here

  // --- Filtering Logic (Frontend) ---
  const filteredProducts = useMemo(() => {
    if (filterCategoryIds.size === 0) return products;
    return products.filter(product =>
        product.categories?.some(cat => filterCategoryIds.has(cat.id))
    );
  }, [products, filterCategoryIds]);

  // --- Handlers ---
  const handleAddProductSuccess = () => { setIsAddModalOpen(false); fetchMyProducts(filterCategoryIds); };
  const handleFilterCategoryChange = (newSelectedIdsSet) => { setFilterCategoryIds(newSelectedIdsSet); };
  const applyFilters = () => { console.log("Applying filters with selected IDs:", filterCategoryIds); /* TODO: Trigger backend filtering later */ };
  const handleViewDetails = (product) => { setSelectedProductForDetail(product); setIsDetailModalOpen(true); };
  const handleCloseDetailModal = () => { setIsDetailModalOpen(false); setSelectedProductForDetail(null); };
  const handleEditProduct = (e, product) => { e.stopPropagation(); console.log("TODO: Open Edit Modal for Product:", product.id); /* Open AddProductModal in edit mode */};
  const handleDeleteProduct = (e, productId) => { e.stopPropagation(); console.log("TODO: Delete Product:", productId); /* Confirmation -> API Call -> fetchMyProducts */};
  const handleStartAuction = (e, productId) => { e.stopPropagation(); console.log("TODO: Start Auction for Product:", productId); /* Open Start Auction Modal */ };

  // Display loading until Keycloak is initialized
  if (!initialized) {
      return <div className="text-center p-10">Initializing authentication...</div>;
  }

  // --- Render Logic ---
  return (
    // Main flex container for sidebar + content
    // Use h-screen and overflow-hidden on parent elements if needed to constrain height
    <div className="flex flex-grow" style={{ height: 'calc(100vh - 4rem)' }}> {/* Adjust 4rem based on your header height */}

      {/* Filter Sidebar (Fixed Width, Scrollable) */}
      <aside className="w-60 md:w-72 flex-shrink-0 bg-white p-4 border-r overflow-y-auto">
         <h3 className="text-lg font-semibold border-b pb-2 mb-4">Filter by Category</h3>
         <CategorySelector
             categories={allCategories}
             selectedIds={filterCategoryIds}
             onSelectionChange={handleFilterCategoryChange}
             isLoading={categoryLoading}
             error={categoryError}
         />
         <button
            onClick={applyFilters}
            className="w-full mt-4 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 px-3 rounded text-sm"
         >
            Apply Filters
         </button>
      </aside>

      {/* Main Content Area (Takes remaining space, Scrollable) */}
      <div className="flex-grow p-6 bg-gray-50 overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold">Your Products</h2>
          <button onClick={() => setIsAddModalOpen(true)} className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center">
            <span className="mr-1">+</span> Add New Product
          </button>
        </div>

        {/* Loading/Error/Empty States */}
        {isLoading && <div className="text-center p-4">Loading products...</div>}
        {error && <div className="text-center p-4 text-red-600">{error}</div>}
        {!isLoading && !error && products.length === 0 && (
             <div className="text-center p-10 border rounded bg-white shadow-sm">
                 <p className="text-gray-500">You haven't added any products yet.</p>
            </div>
         )}
        {!isLoading && !error && products.length > 0 && filteredProducts.length === 0 && (
             <div className="text-center p-10 border rounded bg-white shadow-sm">
                 <p className="text-gray-500">No products match the selected filters.</p>
            </div>
         )}

        {/* Product Grid */}
        {!isLoading && !error && filteredProducts.length > 0 && (
           <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {filteredProducts.map(product => (
                  <div
                     key={product.id}
                     className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer flex flex-col overflow-hidden"
                     onClick={() => handleViewDetails(product)}
                  >
                     {/* Image */}
                      <div className="w-full h-48 bg-gray-200">
                         <img
                             src={product.imageUrls && product.imageUrls.length > 0 ? product.imageUrls[0] : '/placeholder.png'}
                             alt={product.title} className="w-full h-full object-cover" loading="lazy"
                         />
                      </div>
                      {/* Details */}
                      <div className="p-4 flex flex-col flex-grow">
                          <h3 className="font-semibold text-md mb-1 truncate" title={product.title}>{product.title}</h3>
                          <p className="text-xs text-gray-500 mb-3">Condition: {product.condition?.replace('_',' ') || 'N/A'}</p>
                          <div className="mt-auto pt-3 border-t">
                             <div className="flex justify-between items-center mb-3">
                                 <span className="text-xs text-gray-500">Actions:</span>
                                 <div className='space-x-3'>
                                     <button onClick={(e) => handleEditProduct(e, product)} title="Edit Product" className="text-gray-500 hover:text-blue-600 p-1"><FaEdit size="1.1em"/></button>
                                     <button onClick={(e) => handleDeleteProduct(e, product.id)} title="Delete Product" className="text-gray-500 hover:text-red-600 p-1"><FaTrashAlt size="1.1em"/></button>
                                 </div>
                             </div>
                             <button onClick={(e) => handleStartAuction(e, product.id)} className="w-full text-sm bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2 px-4 rounded flex items-center justify-center">
                                 <FaRocket className="mr-2"/> Start Auction
                             </button>
                          </div>
                      </div>
                  </div>
              ))}
           </div>
        )}
      </div>

      {/* Modals */}
      <AddProductModal isOpen={isAddModalOpen} onClose={() => setIsAddModalOpen(false)} onSuccess={handleAddProductSuccess} />
      {selectedProductForDetail && ( <ProductDetailModal isOpen={isDetailModalOpen} onClose={handleCloseDetailModal} product={selectedProductForDetail} onEdit={handleEditProduct} onDelete={handleDeleteProduct} onStartAuction={handleStartAuction} /> )}
    </div>
  );
}

export default ProductsPage;