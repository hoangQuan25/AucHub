// src/components/seller/tabs/listings/SellerProductsSection.jsx
import React from 'react';
import ProductCard from '../../../product/ProductCard'; // Adjust path as needed
import PaginationControls from '../../../PaginationControls'; // Adjust path as needed
import { FaTags, FaPlusCircle } from 'react-icons/fa'; // Added FaPlusCircle

const SellerProductsSection = ({
  products,
  isLoadingProducts,
  productsError,
  productPage,
  productTotalPages,
  listingPageSize,
  onProductPageChange,
  isOwner,
  // NEW: Handler for adding a new product
  onAddNewProduct,
  // Updated handlers for actions on ProductCard
  onEditProduct,
  onDeleteProduct,
  onStartAuctionForProduct,
  onViewDetails,
}) => {
  return (
    <section>
      <div className="flex items-center justify-between mb-6"> {/* Increased margin-bottom */}
        <div className="flex items-center">
          <FaTags className="text-2xl text-purple-600 mr-3" />
          <h3 className="text-xl font-semibold text-gray-800">Items for Sale</h3>
        </div>
        {/* "Add New Product" Button for Owner */}
        {isOwner && (
          <button
            onClick={onAddNewProduct}
            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center"
          >
            <FaPlusCircle className="mr-2" /> Add New Product
          </button>
        )}
      </div>

      {isLoadingProducts && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {/* Skeleton Loaders - Example for 4 skeletons */}
          {Array.from({ length: listingPageSize > 4 ? 4 : listingPageSize }).map((_, index) => (
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
        <div className="text-center py-4 text-red-600 bg-red-50 p-3 rounded">
          {productsError}
        </div>
      )}
      {!isLoadingProducts && !productsError && products.length === 0 && (
        <p className="text-gray-500 py-4 text-center bg-white p-6 rounded-md shadow-sm">
          {isOwner
            ? "You haven't added any products for sale yet."
            : "This seller has no products currently listed for sale."}
          {isOwner && ( // Prompt to add if owner and no products
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
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                isOwner={isOwner}
                // Pass the correct handlers
                onEdit={onEditProduct}
                onDelete={onDeleteProduct}
                onStartAuction={onStartAuctionForProduct}
                onClick={() => onViewDetails(product)}
                // Ensure ProductCard shows/hides its internal action buttons based on isOwner
              />
            ))}
          </div>
          {productTotalPages > 1 && (
            <div className="mt-6"> {/* Added margin-top for spacing */}
              <PaginationControls
                pagination={{
                  currentPage: productPage, // Ensure correct prop name if PaginationControls expects currentPage
                  totalPages: productTotalPages,
                  pageSize: listingPageSize, // Pass pageSize if your component uses it
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