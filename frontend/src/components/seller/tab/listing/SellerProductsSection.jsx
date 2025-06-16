// src/components/seller/tabs/listings/SellerProductsSection.jsx
import React from 'react';
import ProductCard from '../../../product/ProductCard'; // Adjust path as needed
import PaginationControls from '../../../PaginationControls'; // Adjust path as needed
import { FaTags, FaPlusCircle, FaFilter } from 'react-icons/fa'; // Added FaFilter

// Define filter options - can be moved to a constants file if used elsewhere
const PRODUCT_STATUS_FILTERS = [
  { key: 'ALL', label: 'All Products' },
  { key: 'FOR_SALE', label: 'For Sale' }, // Corresponds to isSold = false
  { key: 'SOLD', label: 'Sold' },       // Corresponds to isSold = true
];

const SellerProductsSection = ({
  products,
  isLoadingProducts,
  productsError,
  productPage,
  productTotalPages,
  listingPageSize,
  onProductPageChange,
  isOwner,
  onAddNewProduct,
  onEditProduct,
  onDeleteProduct,
  onStartAuctionForProduct,
  onViewDetails,
  currentProductFilter, // e.g., 'ALL', 'FOR_SALE', 'SOLD'
  onProductFilterChange, // function to call when filter changes
}) => {
  return (
    <section>
      <div className="flex flex-col sm:flex-row items-center justify-between mb-4"> {/* Allow stacking on small screens */}
        <div className="flex items-center mb-3 sm:mb-0">
          <FaTags className="text-2xl text-purple-600 mr-3" />
          <h3 className="text-xl font-semibold text-gray-800">Items for Sale</h3>
        </div>
        {isOwner && (
          <button
            onClick={onAddNewProduct}
            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center self-start sm:self-center"
          >
            <FaPlusCircle className="mr-2" /> Add New Product
          </button>
        )}
      </div>

      {/* --- FILTER UI --- */}
      {isOwner && onProductFilterChange && ( // Show filters only if handler is provided (likely for owner)
        <div className="mb-6 flex flex-wrap items-center gap-2 p-2 border rounded-md bg-gray-50 shadow-sm">
          <FaFilter className="text-purple-600 mr-2" />
          <span className="text-sm font-medium text-gray-700">Filter by status:</span>
          {PRODUCT_STATUS_FILTERS.map(filter => (
            <button
              key={filter.key}
              onClick={() => onProductFilterChange(filter.key)}
              className={`px-3 py-1.5 text-xs sm:text-sm rounded-md transition-colors duration-150 ease-in-out focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-opacity-50 ${
                currentProductFilter === filter.key
                  ? "bg-purple-600 text-white shadow-md"
                  : "text-gray-600 hover:bg-purple-100 hover:text-purple-700 bg-white border border-gray-300"
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      )}

      {/* Existing content: Loading, Error, No Products, Product Grid, Pagination */}
      {isLoadingProducts && (
        // ... skeleton loaders ...
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: Math.min(listingPageSize, 4) }).map((_, index) => (
            <div key={`skeleton-${index}`} className="border rounded-lg bg-white shadow p-4 animate-pulse">
              <div className="w-full h-48 bg-gray-300 rounded mb-3"></div>
              <div className="h-4 bg-gray-300 rounded w-3/4 mb-2"></div>
              <div className="h-3 bg-gray-300 rounded w-1/2 mb-4"></div>
              <div className="h-8 bg-gray-300 rounded w-full"></div>
            </div>
          ))}
        </div>
      )}
      {productsError && (
        // ... error display ...
         <div className="text-center py-4 text-red-600 bg-red-50 p-3 rounded border border-red-200">
          {productsError}
        </div>
      )}
      {!isLoadingProducts && !productsError && products.length === 0 && (
        // ... no products message ...
        <p className="text-gray-500 py-6 text-center bg-white p-6 rounded-md shadow-sm border">
          {isOwner && currentProductFilter === 'ALL' && "You haven't added any products for sale yet."}
          {isOwner && currentProductFilter === 'FOR_SALE' && "You have no products currently marked 'For Sale'."}
          {isOwner && currentProductFilter === 'SOLD' && "You have no products marked as 'Sold'."}
          {!isOwner && "This seller has no products matching the current filter."}
          {isOwner && currentProductFilter === 'ALL' && (
             <button
                onClick={onAddNewProduct}
                className="mt-4 bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center mx-auto"
            >
                <FaPlusCircle className="mr-2" /> Add Your First Product
            </button>
          )}
        </p>
      )}
      {!isLoadingProducts && !productsError && products.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product} // ProductDto should now include `isSold` or `status`
                isOwner={isOwner}
                onEdit={onEditProduct}
                onDelete={onDeleteProduct}
                onStartAuction={onStartAuctionForProduct} // This should check product.isSold internally or before calling
                onClick={() => onViewDetails(product)}
              />
            ))}
          </div>
          {productTotalPages > 1 && (
            // ... pagination controls ...
            <div className="mt-8">
              <PaginationControls
                pagination={{
                  page: productPage,
                  totalPages: productTotalPages,
                }}
                onPageChange={onProductPageChange}
                isLoading={isLoadingProducts}
              />
            </div>
          )}
        </>
      )}
    </section>
  );
};

export default SellerProductsSection;