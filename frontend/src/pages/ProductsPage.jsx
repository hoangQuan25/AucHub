// src/pages/ProductsPage.jsx (Layout with Hierarchical Filter Sidebar & Actions)
import React, { useState, useEffect, useMemo, useCallback } from "react";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient";
import AddProductModal from "../components/AddProductModal";
import CategorySelector from "../components/CategorySelector"; // Component that handles hierarchy
import ProductDetailModal from "../components/ProductDetailModal"; // Assuming this exists
import ConfirmationModal from "../components/ConfirmationModal"; // Assuming this exists
import StartAuctionModal from "../components/StartAuctionModal"; // New component for auction
import { FaEdit, FaTrashAlt, FaRocket } from "react-icons/fa"; // Icons for actions

function ProductsPage() {
  const { keycloak, initialized } = useKeycloak();

  // Product State
  const [products, setProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  // --- MODIFIED: Modal State ---
  const [isAddEditModalOpen, setIsAddEditModalOpen] = useState(false); // Renamed for clarity
  const [editingProduct, setEditingProduct] = useState(null); // null for 'Add' mode, product object for 'Edit' mode
  // --- END MODIFICATION ---

  // --- NEW: State for Delete Confirmation ---
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState(null); // Store {id, title} for message
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  // Details Modal State
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedProductForDetail, setSelectedProductForDetail] =
    useState(null);

  // Category State (for filtering)
  const [allCategories, setAllCategories] = useState([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [categoryError, setCategoryError] = useState("");
  const [filterCategoryIds, setFilterCategoryIds] = useState(new Set()); // Store selected filter IDs

  // --- NEW STATE for Start Auction Modal ---
  const [isStartAuctionModalOpen, setIsStartAuctionModalOpen] = useState(false);
  const [productToAuction, setProductToAuction] = useState(null);
  const [isAuctionConfirmModalOpen, setIsAuctionConfirmModalOpen] = useState(false);
  const [productForAuctionConfirmation, setProductForAuctionConfirmation] = useState(null);
  // ---

  // --- Data Fetching Callbacks ---
  const fetchMyProducts = useCallback(
    async (filterIds = filterCategoryIds) => {
      // Check if initialized and authenticated before fetching
      if (!(initialized && keycloak.authenticated)) {
        setError("Authentication required to view products.");
        setIsLoading(false);
        setProducts([]);
        return;
      }
      setIsLoading(true);
      setError("");
      console.log(
        "Fetching seller's products with category filter:",
        filterIds
      );
      try {
        // TODO: Modify backend later to accept category filters: /api/products/my?categoryIds=1,2,3
        const response = await apiClient.get("/products/my"); // Use correct API path
        setProducts(response.data || []);
      } catch (err) {
        console.error("Failed to fetch products:", err);
        setError(
          err.response?.data?.message || "Could not load your products."
        );
        setProducts([]);
      } finally {
        setIsLoading(false);
      }
    },
    [initialized, keycloak.authenticated]
  ); // Dependency check

  const fetchAllCategories = useCallback(async () => {
    if (!(initialized && keycloak.authenticated)) return; // Don't fetch if not logged in
    setCategoryLoading(true);
    setCategoryError("");
    try {
      const response = await apiClient.get("/products/categories"); // Ensure this endpoint is correct
      setAllCategories(response.data || []);
    } catch (err) {
      console.error("Failed to fetch categories:", err);
      setCategoryError("Could not load categories for filtering.");
      setAllCategories([]);
    } finally {
      setCategoryLoading(false);
    }
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
    return products.filter((product) =>
      product.categories?.some((cat) => filterCategoryIds.has(cat.id))
    );
  }, [products, filterCategoryIds]);

  // --- Handlers ---
  // --- MODIFIED: Modal Success Handler ---
  const handleSaveProductSuccess = (savedProductData) => {
    console.log("Product saved (Add/Edit):", savedProductData);
    setIsAddEditModalOpen(false); // Close the Add/Edit modal
    setEditingProduct(null); // Clear editing state
    fetchMyProducts(filterCategoryIds); // Refetch list to show changes/new item
  };
  
  const handleFilterCategoryChange = (newSelectedIdsSet) => {
    setFilterCategoryIds(newSelectedIdsSet);
  };
  
  const handleViewDetails = (product) => {
    setSelectedProductForDetail(product);
    setIsDetailModalOpen(true);
  };
  const handleCloseDetailModal = () => {
    setIsDetailModalOpen(false);
    setSelectedProductForDetail(null);
  };
  // --- MODIFIED: Edit Handler ---
  const handleEditProduct = (e, product) => {
    e.stopPropagation(); // Prevent card click if called from card button
    console.log("Action: Edit Product", product);
    setEditingProduct(product); // Set the product to edit
    setIsAddEditModalOpen(true); // Open the *same* modal used for adding
  };
  // --- END MODIFICATION ---
  const handleDeleteProduct = (e, productId) => {
    e.stopPropagation();
    console.log(
      "TODO: Delete Product:",
      productId
    ); /* Confirmation -> API Call -> fetchMyProducts */
  };

  const promptStartAuction = (e, product) => {
    e.stopPropagation(); // Prevent card click
    console.log("Prompting confirmation to start auction for:", product);
    setProductForAuctionConfirmation(product); // Store product for confirmation
    setIsAuctionConfirmModalOpen(true);       // Open the confirmation modal
  };

  // --- NEW: Handler for when the user CONFIRMS starting the auction ---
  const handleConfirmStartAuction = () => {
    if (!productForAuctionConfirmation) return; // Should not happen, but safety check

    console.log("User confirmed. Opening Start Auction Modal for Product:", productForAuctionConfirmation);

    // Set the product state needed for the actual StartAuctionModal
    setProductToAuction(productForAuctionConfirmation);
    // setStartAuctionError(''); // Reset error if managed here (likely managed within StartAuctionModal now)

    // Close the confirmation modal
    setIsAuctionConfirmModalOpen(false);
    setProductForAuctionConfirmation(null); // Clear confirmation state

    // Open the *actual* modal to configure the auction details
    setIsStartAuctionModalOpen(true);
  };

  // --- NEW: Handler to simply close the auction confirmation modal ---
  const handleCloseAuctionConfirmModal = () => {
    setIsAuctionConfirmModalOpen(false);
    setProductForAuctionConfirmation(null); // Clear confirmation state
  };

  const handleStartAuctionSubmit = (createdAuctionDto) => {
    // This function DOES NOT make the API call. StartAuctionModal does.
    // This function just handles the SUCCESSFUL result passed back from the modal.

    console.log(
      "Auction successfully created (data received from modal):",
      createdAuctionDto
    );

    // 1. Close the modal (optional, modal might close itself via onClose)
    setIsStartAuctionModalOpen(false);

    // 2. Clear the selected product state in this page
    setProductToAuction(null);
  };

  // --- DELETE HANDLERS ---
  // Opens the delete confirmation modal
  const promptDeleteProduct = (e, product) => {
    e.stopPropagation(); // Prevent card click if called from card button
    console.log("Prompting delete for:", product);
    setProductToDelete(product); // Store the product object (or just id/title)
    setDeleteError(""); // Clear previous delete errors
    setIsDeleteModalOpen(true);
  };

  // Handles the actual API call after modal confirmation
  const handleConfirmDeleteProduct = async () => {
    if (!productToDelete) return;

    setIsDeleting(true);
    setDeleteError("");
    console.log(`Attempting to delete product ID: ${productToDelete.id}`);

    try {
      // Call backend DELETE endpoint (ensure Gateway routes DELETE /api/products/{id})
      await apiClient.delete(`/products/${productToDelete.id}`);
      console.log(`Product ID: ${productToDelete.id} deleted successfully.`);

      setIsDeleteModalOpen(false); // Close modal
      setProductToDelete(null); // Clear selected product
      fetchMyProducts(filterCategoryIds); // Refresh the product list

      // Optional: Show a success notification to the user
      // showToast("Product deleted successfully!");
    } catch (err) {
      console.error("Failed to delete product:", err);
      setDeleteError(
        err.response?.data?.message ||
          "Could not delete product. Please try again."
      );
      // Keep modal open to show error? Or close it? Let's close it for now.
      // setIsDeleteModalOpen(false);
      // setProductToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };
  // --- END DELETE HANDLERS ---

  // Display loading until Keycloak is initialized
  if (!initialized) {
    return (
      <div className="text-center p-10">Initializing authentication...</div>
    );
  }

  // --- Render Logic ---
  return (
    // Main flex container for sidebar + content
    // Use h-screen and overflow-hidden on parent elements if needed to constrain height
    <div className="flex flex-grow" style={{ height: "calc(100vh - 4rem)" }}>
      {" "}
      {/* Adjust 4rem based on your header height */}
      {/* Filter Sidebar (Fixed Width, Scrollable) */}
      <aside className="w-60 md:w-72 flex-shrink-0 bg-white p-4 border-r overflow-y-auto">
        <h3 className="text-lg font-semibold border-b pb-2 mb-4">
          Filter by Category
        </h3>
        <CategorySelector
          categories={allCategories}
          selectedIds={filterCategoryIds}
          onSelectionChange={handleFilterCategoryChange}
          isLoading={categoryLoading}
          error={categoryError}
        />
      </aside>
      {/* Main Content Area (Takes remaining space, Scrollable) */}
      <div className="flex-grow p-6 bg-gray-50 overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold">Your Products</h2>
          <button
            onClick={() => {
              setEditingProduct(null);
              setIsAddEditModalOpen(true);
            }}
            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow flex items-center"
          >
            + Add New Product
          </button>
        </div>

        {/* Loading/Error/Empty States */}
        {isLoading && (
          <div className="text-center p-4">Loading products...</div>
        )}
        {error && <div className="text-center p-4 text-red-600">{error}</div>}
        {!isLoading && !error && products.length === 0 && (
          <div className="text-center p-10 border rounded bg-white shadow-sm">
            <p className="text-gray-500">You haven't added any products yet.</p>
          </div>
        )}
        {!isLoading &&
          !error &&
          products.length > 0 &&
          filteredProducts.length === 0 && (
            <div className="text-center p-10 border rounded bg-white shadow-sm">
              <p className="text-gray-500">
                No products match the selected filters.
              </p>
            </div>
          )}

        {/* Product Grid */}
        {!isLoading && !error && filteredProducts.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {filteredProducts.map((product) => (
              <div
                key={product.id}
                className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer flex flex-col overflow-hidden"
                onClick={() => handleViewDetails(product)}
              >
                {/* Image */}
                <div className="w-full h-48 bg-gray-200">
                  <img
                    src={
                      product.imageUrls && product.imageUrls.length > 0
                        ? product.imageUrls[0]
                        : "/placeholder.png"
                    }
                    alt={product.title}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                </div>
                {/* Details */}
                <div className="p-4 flex flex-col flex-grow">
                  <h3
                    className="font-semibold text-md mb-1 truncate"
                    title={product.title}
                  >
                    {product.title}
                  </h3>
                  <p className="text-xs text-gray-500 mb-3">
                    Condition: {product.condition?.replace("_", " ") || "N/A"}
                  </p>
                  <div className="mt-auto pt-3 border-t">
                    <div className="flex justify-between items-center mb-3">
                      <span className="text-xs text-gray-500">Actions:</span>
                      <div className="space-x-3">
                        <button
                          onClick={(e) => handleEditProduct(e, product)}
                          title="Edit Product"
                          className="text-gray-500 hover:text-blue-600 p-1"
                        >
                          <FaEdit size="1.1em" />
                        </button>
                        <button
                          onClick={(e) => promptDeleteProduct(e, product)}
                          title="Delete Product"
                          className="text-gray-500 hover:text-red-600 p-1"
                        >
                          <FaTrashAlt size="1.1em" />
                        </button>
                      </div>
                    </div>
                    <button
                      onClick={(e) => promptStartAuction(e, product)}
                      className="w-full text-sm bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2 px-4 rounded flex items-center justify-center"
                    >
                      <FaRocket className="mr-2" /> Start Auction
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      {/* Modals */}
      <AddProductModal
        isOpen={isAddEditModalOpen}
        onClose={() => {
          setIsAddEditModalOpen(false);
          setEditingProduct(null);
        }} // Clear editing state on close
        onSuccess={handleSaveProductSuccess} // Use combined success handler
        editingProduct={editingProduct} // Pass the product being edited (or null)
      />
      {selectedProductForDetail && (
        <ProductDetailModal
          isOpen={isDetailModalOpen}
          onClose={handleCloseDetailModal}
          product={selectedProductForDetail}
          onEdit={handleEditProduct}
          onDelete={promptDeleteProduct}
          onStartAuction={promptStartAuction}
        />
      )}
      <ConfirmationModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setProductToDelete(null);
        }}
        onConfirm={handleConfirmDeleteProduct}
        title="Confirm Deletion"
        message={`Are you sure you want to delete the product "${
          productToDelete?.title || "this item"
        }"? This action cannot be undone.`}
        confirmText="Delete"
        confirmButtonClass="bg-red-600 hover:bg-red-700" // Optional: Style delete button
        isLoading={isDeleting} // Pass loading state
        error={deleteError} // Pass error state
      />
      {/* --- NEW: Confirmation Modal for Starting Auction --- */}
      <ConfirmationModal
        isOpen={isAuctionConfirmModalOpen}
        onClose={handleCloseAuctionConfirmModal} // Use the new close handler
        onConfirm={handleConfirmStartAuction}     // Use the new confirm handler
        title="Confirm Start Auction"
        message={`Are you sure you want to proceed with starting an auction for "${
          productForAuctionConfirmation?.title || "this item"
        }"?\n\nYou will configure the auction details in the next step.`}
        confirmText="Proceed"
        cancelText="Cancel"
        confirmButtonClass="bg-purple-600 hover:bg-purple-700" // Style like the original button
        // isLoading={false} // Confirmation step itself isn't loading anything async
        // error={null}      // No specific error state for this simple confirmation
      />
      {/* --- NEW: Render Start Auction Modal --- */}
      {productToAuction && ( // Render only when a product is selected
        <StartAuctionModal
          isOpen={isStartAuctionModalOpen}
          onClose={() => {
            setIsStartAuctionModalOpen(false);
            setProductToAuction(null);
          }}
          product={productToAuction}
          onStartAuctionSubmit={handleStartAuctionSubmit} // Pass the handler
          // Pass loading/error states if you want modal to display them:
          // isLoading={startAuctionLoading}
          // error={startAuctionError}
        />
      )}
      {/* --- END --- */}
    </div>
  );
}

export default ProductsPage;
