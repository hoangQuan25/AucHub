// src/pages/PublicSellerProfilePage.jsx
import React, { useState, useEffect, useCallback } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import apiClient from "../api/apiClient"; // Adjust path
import { useKeycloak } from "@react-keycloak/web";
import {
  FaStore,
  FaStar,
  FaBoxOpen,
  FaTags,
} from "react-icons/fa";
import CategorySelector from "../components/CategorySelector"; 
import SellerProfileHeader from "../components/seller/SellerProfileHeader"; 
import AboutSellerTab from "../components/seller/AboutSellerTab";
import ReviewsTab from "../components/seller/tab/ReviewsTab";
import SellerProductsSection from "../components/seller/tab/listing/SellerProductsSection";
import AuctionFilters from "../components/seller/tab/listing/AuctionFilters";
import SellerLiveAuctions from "../components/seller/tab/listing/SellerLiveAuctions";
import SellerTimedAuctions from "../components/seller/tab/listing/SellerTimedAuctions";
import MySalesTab from "../components/seller/tab/MySalesTab"; 
import SellerDecisionModal from "../components/SellerDecisionModal";
import AddProductModal from "../components/AddProductModal"; 
import StartAuctionModal from "../components/StartAuctionModal"; 
import ProductDetailModal from "../components/ProductDetailModal"; 
import ConfirmationModal from "../components/ConfirmationModal"; 

const STATUS_TABS = [
  { key: "ALL", label: "All Auctions" }, // "All" for auctions shown on profile
  { key: "ACTIVE", label: "Ongoing" },
  { key: "SCHEDULED", label: "Scheduled" },
  { key: "ENDED", label: "Ended" },
];

const TIME_FILTERS = [
  { key: "ALL", label: "All time" },
  { key: "24H", label: "Last 24h" },
  { key: "7D", label: "Last 7 days" },
  { key: "30D", label: "Last 30 days" },
];

const calcFromDateParam = (timeKey) => {
  if (timeKey === "ALL") return undefined;
  const now = Date.now();
  const dayMs = 86_400_000;
  switch (timeKey) {
    case "24H":
      return new Date(now - dayMs).toISOString();
    case "7D":
      return new Date(now - 7 * dayMs).toISOString();
    case "30D":
      return new Date(now - 30 * dayMs).toISOString();
    default:
      return undefined;
  }
};

const TABS = [
  { key: "about", label: "About Seller", icon: FaStore },
  { key: "listings", label: "Listings", icon: FaBoxOpen },
  { key: "reviews", label: "Reviews", icon: FaStar },
  { key: "my-sales", label: "My Sales", icon: FaTags },
];

const LISTING_PAGE_SIZE = 8; // For auctions and products
const REVIEW_PAGE_SIZE = 5;
const SALES_PAGE_SIZE = 10;

function PublicSellerProfilePage() {
  const { identifier } = useParams();
  const navigate = useNavigate();
  const { keycloak, initialized } = useKeycloak();

  const [sellerProfile, setSellerProfile] = useState(null);
  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [profileError, setProfileError] = useState("");
  const [activeTab, setActiveTab] = useState("about");
  const [isOwner, setIsOwner] = useState(false);

  const [reviews, setReviews] = useState([]);
  const [reviewPage, setReviewPage] = useState(0);
  const [reviewTotalPages, setReviewTotalPages] = useState(0);
  const [isLoadingReviews, setIsLoadingReviews] = useState(false);
  const [reviewsError, setReviewsError] = useState("");

  const [products, setProducts] = useState([]);
  const [productPage, setProductPage] = useState(0);
  const [productTotalPages, setProductTotalPages] = useState(0);
  const [isLoadingProducts, setIsLoadingProducts] = useState(false);
  const [productsError, setProductsError] = useState("");

  const [isProductDetailModalOpen, setIsProductDetailModalOpen] =
    useState(false);
  const [selectedProductForDetail, setSelectedProductForDetail] =
    useState(null);

  const [isAddEditModalOpen, setIsAddEditModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null); // null for 'Add', product for 'Edit'

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [productToDelete, setProductToDelete] = useState(null); // Store {id, title}
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  const [isAuctionConfirmModalOpen, setIsAuctionConfirmModalOpen] =
    useState(false);
  const [productForAuctionConfirmation, setProductForAuctionConfirmation] =
    useState(null);

  const [isStartAuctionModalOpen, setIsStartAuctionModalOpen] = useState(false);
  const [productToAuction, setProductToAuction] = useState(null);

  // --- Auction States (aligned with MyAuctionsPage) ---
  const [liveAuctions, setLiveAuctions] = useState([]);
  const [timedAuctions, setTimedAuctions] = useState([]);
  const [livePagination, setLivePagination] = useState({
    page: 0,
    totalPages: 0,
  });
  const [timedPagination, setTimedPagination] = useState({
    page: 0,
    totalPages: 0,
  });
  const [isLoadingLiveAuctions, setIsLoadingLiveAuctions] = useState(false); // Renamed for clarity
  const [isLoadingTimedAuctions, setIsLoadingTimedAuctions] = useState(false); // Renamed for clarity
  const [errorLiveAuctions, setErrorLiveAuctions] = useState(""); // Renamed
  const [errorTimedAuctions, setErrorTimedAuctions] = useState(""); // Renamed

  const isLoadingAnyAuction = isLoadingLiveAuctions || isLoadingTimedAuctions;

  const [auctionStatusFilter, setAuctionStatusFilter] = useState("ALL");
  const [auctionTimeFilter, setAuctionTimeFilter] = useState("ALL");

  const [allCategories, setAllCategories] = useState([]);
  const [catLoading, setCatLoading] = useState(false);
  const [catError, setCatError] = useState("");
  const [selectedCatIds, setSelectedCatIds] = useState(new Set());

  const [activeSalesFilter, setActiveSalesFilter] = useState("ALL");
  const [salesOrders, setSalesOrders] = useState([]);
  const [isLoadingSales, setIsLoadingSales] = useState(false);
  const [salesError, setSalesError] = useState(null);
  const [selectedOrderForDecision, setSelectedOrderForDecision] =
    useState(null);
  const [isDecisionModalOpen, setIsDecisionModalOpen] = useState(false);

  const [currentProductFilter, setCurrentProductFilter] = useState("ALL");

  useEffect(() => {
    if (initialized && keycloak.authenticated && sellerProfile?.id) {
      setIsOwner(keycloak.subject === sellerProfile.id);
    } else {
      setIsOwner(false);
      if (activeTab === "my-sales") {
        setActiveTab("about");
      }
    }
  }, [
    initialized,
    keycloak.authenticated,
    keycloak.subject,
    sellerProfile?.id,
  ]);

  const fetchSellerProfile = useCallback(async () => {
    setIsLoadingProfile(true);
    setProfileError("");
    try {
      const response = await apiClient.get(`/users/sellers/${identifier}`);
      setSellerProfile(response.data);
    } catch (err) {
      console.error("Failed to fetch seller profile:", err);
      setProfileError(
        err.response?.data?.message || "Could not load seller profile."
      );
      setSellerProfile(null);
    } finally {
      setIsLoadingProfile(false);
    }
  }, [identifier]);

  useEffect(() => {
    if (identifier) {
      fetchSellerProfile();
    }
  }, [identifier, fetchSellerProfile]);

  // Fetch categories (e.g., for a filter sidebar)
  const fetchAllCategoriesForFilter = useCallback(async () => {
    setCatLoading(true);
    try {
      const resp = await apiClient.get("/products/categories");
      setAllCategories(resp.data || []);
    } catch (e) {
      console.error("Category fetch failed for profile page", e);
      setCatError("Could not load categories for filtering.");
    } finally {
      setCatLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllCategoriesForFilter();
  }, [fetchAllCategoriesForFilter]);

  const fetchSellerReviews = useCallback(
    async (page = 0) => {
      if (!sellerProfile?.id) return; // Need sellerId to fetch reviews

      setIsLoadingReviews(true);
      setReviewsError("");
      try {
        // Endpoint from SellerReviewController: /reviews/seller/{sellerId}
        const response = await apiClient.get(
          `/users/seller/${sellerProfile.id}`,
          {
            params: {
              page: page,
              size: REVIEW_PAGE_SIZE,
              sort: "createdAt,desc", // Default sort
            },
          }
        );
        setReviews(response.data.content || []);
        setReviewPage(response.data.number);
        setReviewTotalPages(response.data.totalPages || 0);
      } catch (err) {
        console.error("Failed to fetch seller reviews:", err);
        setReviewsError(
          err.response?.data?.message ||
            "Could not load reviews for this seller."
        );
        setReviews([]);
      } finally {
        setIsLoadingReviews(false);
      }
    },
    [sellerProfile?.id]
  );

  const fetchSellerProducts = useCallback(
    async (page = 0, filter = "ALL") => {
      if (!sellerProfile?.id) return;
      setIsLoadingProducts(true);
      setProductsError("");
      try {
        const params = {
          page: page,
          size: LISTING_PAGE_SIZE,
          sort: "createdAt,desc",
        };
        if (selectedCatIds.size > 0) {
          // Apply category filter to products if selected
          params.categoryIds = Array.from(selectedCatIds).join(",");
        }

        if (filter === "FOR_SALE") {
          params.isSold = false;
        } else if (filter === "SOLD") {
          params.isSold = true;
        }

        const response = await apiClient.get(
          `/products/seller/${sellerProfile.id}/products`,
          { params }
        );
        setProducts(response.data.content || []);
        setProductPage(response.data.number);
        setProductTotalPages(response.data.totalPages || 0);
      } catch (err) {
        setProductsError(
          err.response?.data?.message || "Could not load seller's products."
        );
        setProducts([]);
      } finally {
        setIsLoadingProducts(false);
      }
    },
    [sellerProfile?.id, selectedCatIds, LISTING_PAGE_SIZE]
  ); 

  const fetchAllSellerAuctions = useCallback(
    async (
      livePage = livePagination.page,
      timedPage = timedPagination.page
    ) => {
      if (!sellerProfile?.id) return;

      setIsLoadingLiveAuctions(true);
      setIsLoadingTimedAuctions(true);
      setErrorLiveAuctions("");
      setErrorTimedAuctions("");

      const commonParams = {};
      if (
        auctionStatusFilter === "ACTIVE" ||
        auctionStatusFilter === "SCHEDULED"
      ) {
        commonParams.status = auctionStatusFilter;
      } else if (auctionStatusFilter === "ENDED") {
        commonParams.ended = true;
      }

      if (selectedCatIds.size) {
        commonParams.categoryIds = Array.from(selectedCatIds).join(",");
      }
      const fromIso = calcFromDateParam(auctionTimeFilter);
      if (fromIso) commonParams.from = fromIso;

      const livePromise = apiClient
        .get(`/liveauctions/seller/${sellerProfile.id}/live-auctions`, {
          params: {
            ...commonParams,
            page: livePage,
            size: LISTING_PAGE_SIZE,
            sort: "startTime,asc",
          },
        })
        .catch((err) => ({
          error: true,
          type: "live",
          message:
            err.response?.data?.message || "Unable to load live auctions.",
          errorObj: err,
        }));

      const timedSpecificParams = {
        ...commonParams,
        page: timedPage,
        size: LISTING_PAGE_SIZE,
        sort:
          auctionStatusFilter === "ENDED" ? "endTime,desc" : "startTime,asc",
      };

      if (
        auctionStatusFilter === "ALL" ||
        auctionStatusFilter === "SCHEDULED" ||
        auctionStatusFilter === "ENDED"
      ) {
        timedSpecificParams.activeOnly = false;
      }

      const timedPromise = apiClient
        .get(`/timedauctions/seller/${sellerProfile.id}/timed-auctions`, {
          params: timedSpecificParams, // Use the modified params
        })
        .catch((err) => ({
          error: true,
          type: "timed",
          message:
            err.response?.data?.message || "Unable to load timed auctions.",
          errorObj: err,
        }));

      const [liveResult, timedResult] = await Promise.all([
        livePromise,
        timedPromise,
      ]);

      // Process Live Auctions
      if (liveResult.error) {
        console.error(
          "Failed to fetch seller's live auctions",
          liveResult.errorObj
        );
        setErrorLiveAuctions(liveResult.message);
        setLiveAuctions([]);
      } else {
        setLiveAuctions(liveResult.data.content || []);
        setLivePagination((p) => ({
          ...p,
          page: liveResult.data.number,
          totalPages: liveResult.data.totalPages || 0,
        }));
      }
      setIsLoadingLiveAuctions(false);

      // Process Timed Auctions
      if (timedResult.error) {
        console.error(
          "Failed to fetch seller's timed auctions",
          timedResult.errorObj
        );
        setErrorTimedAuctions(timedResult.message);
        setTimedAuctions([]);
      } else {
        setTimedAuctions(timedResult.data.content || []);
        setTimedPagination((p) => ({
          ...p,
          page: timedResult.data.number,
          totalPages: timedResult.data.totalPages || 0,
        }));
      }
      setIsLoadingTimedAuctions(false);
    },
    [
      sellerProfile?.id,
      auctionStatusFilter,
      auctionTimeFilter,
      selectedCatIds, // Add selectedCatIds
      livePagination.page, // only for initial call from useEffect
      timedPagination.page, // only for initial call from useEffect
    ]
  );

  const fetchSalesOrders = useCallback(async () => {
    if (!initialized || !keycloak.authenticated) {
      setSalesError("Please log in to view your sales.");
      setSalesOrders([]);
      return;
    }

    setIsLoadingSales(true); // Use sales-specific loading state
    setSalesError(null); // Use sales-specific error state
    try {
      const params = {
        page: 0, // Add pagination state for sales later if needed
        size: SALES_PAGE_SIZE,
      };
      if (activeSalesFilter !== "ALL") {
        // Use sales-specific filter state
        params.status = activeSalesFilter;
      }
      const response = await apiClient.get(`/orders/my-sales`, { params });
      setSalesOrders(response.data.content || []); // Use sales-specific order state
    } catch (err) {
      console.error("Failed to fetch sales orders:", err);
      setSalesError(
        err.response?.data?.message || "Could not load your sales orders."
      );
      setSalesOrders([]);
    } finally {
      setIsLoadingSales(false);
    }
  }, [initialized, keycloak.authenticated, activeSalesFilter]);

  // Fetch data based on activeTab and loaded profile
  useEffect(() => {
    if (sellerProfile?.id || (activeTab === "my-sales" && isOwner)) {
      if (activeTab === "reviews") {
        fetchSellerReviews(reviewPage);
      } else if (activeTab === "listings") {
        fetchSellerProducts(productPage, currentProductFilter);
        // fetchAllSellerAuctions is now triggered by its own specific useEffect
      } else if (activeTab === "my-sales") {
        if (isOwner) {
          fetchSalesOrders();
        } else {
          setSalesOrders([]);
          setSalesError("Access denied to view sales.");
        }
      }
    }
  }, [
    activeTab,
    sellerProfile?.id,
    isOwner,
    fetchSellerReviews,
    reviewPage,
    fetchSellerProducts,
    productPage,
    currentProductFilter, // currentProductFilter will trigger refetch if it changes
    fetchSalesOrders, // fetchSalesOrders dependency will trigger refetch if activeSalesFilter changes
  ]);

  // Specific useEffect for auction refetch when auction filters or pagination change
  useEffect(() => {
    if (activeTab === "listings" && sellerProfile?.id) {
      fetchAllSellerAuctions(livePagination.page, timedPagination.page);
    }
  }, [
    activeTab, // ensure we are on the listings tab
    sellerProfile?.id, // ensure profile is loaded
    livePagination.page,
    timedPagination.page,
    auctionStatusFilter,
    auctionTimeFilter,
    selectedCatIds,
  ]);

  const handleOpenAddProductModal = () => {
    if (!isOwner) return; // Safety check
    setEditingProduct(null); // 'Add' mode
    setIsAddEditModalOpen(true);
  };

  const handleOpenEditProductModal = (product) => {
    if (!isOwner) return;
    setEditingProduct(product); // 'Edit' mode
    setIsAddEditModalOpen(true);
  };

  // On Save Success (from AddProductModal)
  const handleSaveProductSuccess = (savedProductData) => {
    setIsAddEditModalOpen(false);
    setEditingProduct(null);
    fetchSellerProducts(productPage); // Refetch current page of products
    // Optionally show a success toast/notification
  };

  // Prompt Delete Product (from card or detail modal)
  const promptDeleteProduct = (product) => {
    if (!isOwner) return;
    setProductToDelete(product);
    setDeleteError("");
    setIsDeleteModalOpen(true);
  };

  // Confirm Delete Product
  const handleConfirmDeleteProduct = async () => {
    if (!productToDelete || !isOwner) return;
    setIsDeleting(true);
    setDeleteError("");
    try {
      await apiClient.delete(`/products/${productToDelete.id}`); // Assuming this is your delete endpoint
      setIsDeleteModalOpen(false);
      setProductToDelete(null);
      fetchSellerProducts(0); // Refetch from first page or current page
    } catch (err) {
      setDeleteError(
        err.response?.data?.message || "Could not delete product."
      );
    } finally {
      setIsDeleting(false);
    }
  };

  // Prompt Start Auction (from card or detail modal)
  const promptStartAuction = (product) => {
    if (!isOwner) return;
    setProductForAuctionConfirmation(product);
    setIsAuctionConfirmModalOpen(true);
  };

  // Confirm Start Auction (opens StartAuctionModal)
  const handleConfirmStartAuction = () => {
    if (!productForAuctionConfirmation || !isOwner) return;
    setProductToAuction(productForAuctionConfirmation);
    setIsAuctionConfirmModalOpen(false);
    setProductForAuctionConfirmation(null);
    setIsStartAuctionModalOpen(true);
  };

  const handleStartAuctionSubmit = (createdAuctionDto) => {
    setIsStartAuctionModalOpen(false);
    const reopenedProductInfo = productToAuction;
    setProductToAuction(null);
    fetchSellerProducts(0); // Or current page
    if (activeTab === "listings") {
      fetchAllSellerAuctions(0, 0); // Refresh auctions if on listings tab
    }

    if (
      selectedOrderForDecision &&
      selectedOrderForDecision.productId === reopenedProductInfo?.id
    ) {
      const originalOrderId = selectedOrderForDecision.id;
      log.info(
        `Attempting to update original order ${originalOrderId} as superseded by new auction.`
      );
      try {

        console.log(
          `Frontend: New auction started for product from order ${originalOrderId}. Original order should be finalized on backend.`
        );
      } catch (error) {
        log.error(
          `Error trying to update original order ${originalOrderId} after reopening:`,
          error
        );
      } finally {
        setSelectedOrderForDecision(null); // Clear the original order from state
      }
    }
  };

  const handleViewProductDetails = useCallback((product) => {
    setSelectedProductForDetail(product);
    setIsProductDetailModalOpen(true);
  }, []);

  const handleCloseProductDetailModal = useCallback(() => {
    setIsProductDetailModalOpen(false);
    setSelectedProductForDetail(null);
  }, []);

  const handleReviewPageChange = (newPage) => {
    setReviewPage(newPage);
  };
  const handleProductPageChange = (newPage) => {
    setProductPage(newPage);
  };
  const handleLiveAuctionPageChange = (newPage) => {
    setLivePagination((p) => ({ ...p, page: newPage }));
  };
  const handleTimedAuctionPageChange = (newPage) => {
    setTimedPagination((p) => ({ ...p, page: newPage }));
  };

  const handleAuctionCardClick = useCallback(
    (id, type) => {
      const path =
        type === "LIVE" ? `/live-auctions/${id}` : `/timed-auctions/${id}`;
      navigate(path);
    },
    [navigate]
  );

  const handleAuctionFilterChange = useCallback(() => {
    setLivePagination((p) => ({ ...p, page: 0, totalPages: 0 })); // Reset totalPages too
    setTimedPagination((p) => ({ ...p, page: 0, totalPages: 0 }));
  }, []);

  const handleOpenDecisionModal = (order) => {
    setSelectedOrderForDecision(order);
    setIsDecisionModalOpen(true);
  };

  const handleCloseDecisionModal = () => {
    setSelectedOrderForDecision(null);
    setIsDecisionModalOpen(false);
    if (activeTab === "my-sales" && isOwner) {
      fetchSalesOrders();
    }
  };

  const handleInitiateReopenAuctionFromDecision = (orderFromDecisionModal) => {
    console.log("--- handleInitiateReopenAuctionFromDecision ---");
    console.log(
      "Received orderFromDecisionModal:",
      JSON.stringify(orderFromDecisionModal, null, 2)
    );

    setIsDecisionModalOpen(false);
    // setSelectedOrderForDecision(null); // Already handled or will be by onClose

    let productIdForReopen = null;
    let productTitleForReopen = "Product (Details Missing)"; // Fallback title
    let productImageUrlForReopen = null;

    if (
      orderFromDecisionModal.items &&
      orderFromDecisionModal.items.length > 0
    ) {
      const firstItem = orderFromDecisionModal.items[0];
      productIdForReopen = firstItem.productId;
      productTitleForReopen = firstItem.title; 
      productImageUrlForReopen = firstItem.imageUrl; 
    } else {
      console.warn(
        "OrderSummaryDto is missing items or items array is empty. Cannot get product details for reopen.",
        orderFromDecisionModal
      );
    }

    console.log(
      "Extracted for reopen: productId =",
      productIdForReopen,
      ", title =",
      productTitleForReopen
    );

    const productForReopen = {
      id: productIdForReopen, // Use extracted productId
      title: productTitleForReopen, // Use extracted title
      imageUrls: productImageUrlForReopen ? [productImageUrlForReopen] : [], // Use extracted imageUrl
      originalOrderId: orderFromDecisionModal.id, // This is the OrderSummaryDto's ID (the order ID)
    };

    console.log(
      "Constructed productForReopen:",
      JSON.stringify(productForReopen, null, 2)
    );

    if (!productIdForReopen) {
      console.error(
        "Cannot proceed to reopen auction: Product ID is missing from order summary items."
      );
      alert(
        "Error: Product details are missing from the order summary, cannot reopen auction."
      );
      return; // Prevent opening StartAuctionModal if productId is missing
    }

    setProductToAuction(productForReopen);
    setIsStartAuctionModalOpen(true);
  };

  const handleProductFilterChange = useCallback((newFilter) => {
    setCurrentProductFilter(newFilter);
    setProductPage(0); // Reset to the first page when filter changes
  }, []);

  const renderTabContent = () => {
    if (!sellerProfile)
      return (
        <div className="text-center p-10 text-gray-500">
          Seller profile not found or still loading.
        </div>
      );

    switch (activeTab) {
      case "about":
        return <AboutSellerTab sellerProfile={sellerProfile} />;
      case "listings":
        return (
          <div className="space-y-10 p-1 md:p-4">
            <SellerProductsSection
              products={products}
              isLoadingProducts={isLoadingProducts}
              productsError={productsError}
              productPage={productPage}
              productTotalPages={productTotalPages}
              listingPageSize={LISTING_PAGE_SIZE}
              onProductPageChange={handleProductPageChange} 
              isOwner={isOwner}
              onAddNewProduct={handleOpenAddProductModal}
              onEditProduct={handleOpenEditProductModal} 
              onDeleteProduct={promptDeleteProduct} 
              onStartAuctionForProduct={promptStartAuction} 
              onViewDetails={handleViewProductDetails}
              currentProductFilter={currentProductFilter}
              onProductFilterChange={handleProductFilterChange}
            />

            <AuctionFilters
              auctionStatusFilter={auctionStatusFilter}
              setAuctionStatusFilter={setAuctionStatusFilter}
              auctionTimeFilter={auctionTimeFilter}
              setAuctionTimeFilter={setAuctionTimeFilter}
              statusTabs={STATUS_TABS}
              timeFilters={TIME_FILTERS}
              onFilterChange={handleAuctionFilterChange}
            />

            <SellerTimedAuctions
              timedAuctions={timedAuctions}
              isLoadingTimedAuctions={isLoadingTimedAuctions}
              errorTimedAuctions={errorTimedAuctions}
              timedPagination={timedPagination}
              listingPageSize={LISTING_PAGE_SIZE}
              onTimedAuctionPageChange={handleTimedAuctionPageChange} // Pass the handler
              onAuctionCardClick={handleAuctionCardClick}
              isOwner={isOwner}
            />

            <SellerLiveAuctions
              liveAuctions={liveAuctions}
              isLoadingLiveAuctions={isLoadingLiveAuctions}
              errorLiveAuctions={errorLiveAuctions}
              livePagination={livePagination}
              listingPageSize={LISTING_PAGE_SIZE}
              onLiveAuctionPageChange={handleLiveAuctionPageChange} // Pass the handler
              onAuctionCardClick={handleAuctionCardClick}
              isOwner={isOwner}
            />
          </div>
        );
      case "my-sales":
        if (!isOwner) {
          return (
            <div className="p-6 text-center text-gray-500 bg-white rounded-md shadow">
              This sales information is private.
            </div>
          );
        }
        return (
          <MySalesTab
            salesOrders={salesOrders}
            isLoadingSales={isLoadingSales}
            salesError={salesError}
            activeSalesFilter={activeSalesFilter}
            setActiveSalesFilter={setActiveSalesFilter}
            onOpenDecisionModal={handleOpenDecisionModal}
          />
        );
      case "reviews":
        return (
          <ReviewsTab
            reviews={reviews}
            isLoadingReviews={isLoadingReviews}
            reviewsError={reviewsError}
            reviewPage={reviewPage}
            reviewTotalPages={reviewTotalPages}
            reviewPageSize={REVIEW_PAGE_SIZE} // Pass the constant
            handleReviewPageChange={handleReviewPageChange}
            sellerUsername={sellerProfile?.username} // Pass seller username for heading
          />
        );
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-grow" style={{ height: "calc(100vh - 4rem)" }}>
      {/* Sidebar for Categories */}
      <aside
        className={`
          bg-white flex-shrink-0 overflow-hidden
          transition-all duration-300 ease-in-out
          ${
            activeTab === "listings"
              ? "w-60 md:w-72 p-4 opacity-100 border-r" // Visible state
              : "w-0 p-0 opacity-0 border-r border-transparent pointer-events-none" // Collapsed state
          }
        `}
      >
        <h3 className="text-lg font-semibold border-b pb-2 mb-4">
          Filter by Category
        </h3>
        <CategorySelector
          categories={allCategories}
          selectedIds={selectedCatIds}
          onSelectionChange={(newSelectedIds) => {
            setSelectedCatIds(newSelectedIds);
            // Reset pages when category filter changes for relevant listings
            setProductPage(0);
            setLivePagination((p) => ({ ...p, page: 0 }));
            setTimedPagination((p) => ({ ...p, page: 0 }));
          }}
          isLoading={catLoading}
          error={catError}
        />
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col overflow-y-auto bg-gray-50">
        {/* Profile Header */}
        <div className="p-4 md:px-6 md:pt-6">
          <SellerProfileHeader
            sellerProfile={sellerProfile}
            isLoadingProfile={isLoadingProfile}
            profileError={profileError}
          />
        </div>

        {/* Sticky Tabs Nav */}
        <div className="sticky top-0 z-10 bg-gray-50 border-b border-gray-200 px-4 md:px-6">
          <nav className="flex flex-wrap -mb-px bg-white shadow-sm rounded-t-md">
            {TABS.map((tab) => (
              <button
                key={tab.key}
                onClick={() => {
                  setActiveTab(tab.key); 
                }}
                className={`py-3 px-4 sm:px-6 text-sm font-medium focus:outline-none transition-colors duration-150 ease-in-out flex items-center space-x-2 ${
                  activeTab === tab.key
                    ? "text-indigo-600 border-b-2 border-indigo-600 font-semibold"
                    : "text-gray-600 hover:text-gray-800 hover:border-gray-300 border-b-2 border-transparent"
                }`}
              >
                <tab.icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            ))}
          </nav>
        </div>

        {/* Tab Content */}
        <div className="flex-grow overflow-y-auto p-4 md:p-6">
          {renderTabContent()}
        </div>
      </div>

      {isOwner && ( // Only render these modals if the user is the owner
        <>
          <AddProductModal
            isOpen={isAddEditModalOpen}
            onClose={() => {
              setIsAddEditModalOpen(false);
              setEditingProduct(null);
            }}
            onSuccess={handleSaveProductSuccess}
            editingProduct={editingProduct}
          />

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
            confirmButtonClass="bg-red-600 hover:bg-red-700"
            isLoading={isDeleting}
            error={deleteError}
          />

          <ConfirmationModal // For auction start confirmation
            isOpen={isAuctionConfirmModalOpen}
            onClose={() => {
              setIsAuctionConfirmModalOpen(false);
              setProductForAuctionConfirmation(null);
            }}
            onConfirm={handleConfirmStartAuction}
            title="Confirm Start Auction"
            message={`Are you sure you want to proceed with starting an auction for "${
              productForAuctionConfirmation?.title || "this item"
            }"?\nYou will configure auction details next.`}
            confirmText="Proceed"
            confirmButtonClass="bg-purple-600 hover:bg-purple-700"
          />

          {productToAuction && ( // Render StartAuctionModal only when productToAuction is set
            <StartAuctionModal
              isOpen={isStartAuctionModalOpen}
              onClose={() => {
                setIsStartAuctionModalOpen(false);
                setProductToAuction(null);
              }}
              product={productToAuction}
              onStartAuctionSubmit={handleStartAuctionSubmit}
            />
          )}
        </>
      )}

      {/* Product Detail Modal (can be viewed by anyone, actions inside depend on owner) */}
      {selectedProductForDetail && (
        <ProductDetailModal
          isOpen={isProductDetailModalOpen}
          onClose={handleCloseProductDetailModal}
          product={selectedProductForDetail}
          isOwner={isOwner} // Pass isOwner to ProductDetailModal
          onEdit={isOwner ? handleOpenEditProductModal : undefined}
          onDelete={isOwner ? promptDeleteProduct : undefined}
          onStartAuction={isOwner ? promptStartAuction : undefined}
        />
      )}

      {selectedOrderForDecision && isDecisionModalOpen && isOwner && (
        <SellerDecisionModal
          order={selectedOrderForDecision}
          isOpen={isDecisionModalOpen}
          onClose={() => {
            setIsDecisionModalOpen(false);
            setSelectedOrderForDecision(null);
            if (activeTab === "my-sales") fetchSalesOrders(); 
          }}
          onInitiateReopenAuction={handleInitiateReopenAuctionFromDecision}
        />
      )}
    </div>
  );
}

export default PublicSellerProfilePage;
