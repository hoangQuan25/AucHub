// src/pages/MyAuctionsPage.jsx – revised to avoid enum conversion error
import React, { useState, useEffect, useCallback } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate } from "react-router-dom";
import { FaTrash } from "react-icons/fa";
import apiClient from "../api/apiClient";
import AuctionCard from "../components/AuctionCard";
import CategorySelector from "../components/CategorySelector";
import ConfirmationModal from "../components/ConfirmationModal";
import PaginationControls from "../components/PaginationControls";

/* -------------------------------------------------------------------------
 * Tabs / filters: we expose raw enum values the backend knows about.
 * For the UI “Ended” tab, we DON’T send a single status.  Instead we send
 *   ended=true
 * Backend recognises that flag and returns SOLD / RESERVE_NOT_MET / CANCELLED.
 * -------------------------------------------------------------------------*/
const STATUS_TABS = [
  { key: "ALL", label: "All" },
  { key: "ACTIVE", label: "Ongoing" },
  { key: "SCHEDULED", label: "Scheduled" },
  { key: "ENDED", label: "Ended" },
];

const TIME_FILTERS = [
  { key: "ALL", label: "All time" },
  { key: "24H", label: "Last 24 h" },
  { key: "7D", label: "Last 7 days" },
  { key: "30D", label: "Last 30 days" },
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

function MyAuctionsPage() {
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();

  /* ------------------------------- state ---------------------------------*/
  const [followingAuctions, setFollowingAuctions] = useState([]); // Single list for combined results
  const [pagination, setPagination] = useState({
    page: 0,
    size: 12,
    totalPages: 0,
  }); // Single pagination state
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  const [statusFilter, setStatusFilter] = useState("ALL");
  const [timeFilter, setTimeFilter] = useState("ALL");

  const [allCategories, setAllCategories] = useState([]);
  const [catLoading, setCatLoading] = useState(false);
  const [catError, setCatError] = useState("");
  const [selectedCatIds, setSelectedCatIds] = useState(new Set());

  const [isUnfollowConfirmOpen, setIsUnfollowConfirmOpen] = useState(false);
  const [auctionToUnfollow, setAuctionToUnfollow] = useState(null);
  const [unfollowLoading, setUnfollowLoading] = useState(false);
  const [unfollowError, setUnfollowError] = useState("");

  /* ---------------------------- fetch helpers ----------------------------*/
  const fetchCategories = useCallback(async () => {
    if (!(initialized && keycloak.authenticated)) return;
    setCatLoading(true);
    try {
      const resp = await apiClient.get("/products/categories");
      setAllCategories(resp.data || []);
    } catch (e) {
      console.error("Category fetch failed", e);
      setCatError("Could not load categories.");
    } finally {
      setCatLoading(false);
    }
  }, [initialized, keycloak.authenticated]);

  // fire it once on mount (and whenever auth flips)
  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  /* --- MODIFIED: Fetch Auctions Logic --- */
  const fetchFollowingAuctions = useCallback(
    async (pageToFetch = 0) => {
      if (!(initialized && keycloak.authenticated)) return;

      setIsLoading(true); // Set single loading state
      setError(""); // Clear single error state

      // --- Prepare Common Params (Same as before) ---
      const params = {
        page: pageToFetch,
        size: pagination.size, // Use single pagination state
      };
      if (statusFilter === "ACTIVE" || statusFilter === "SCHEDULED") {
        params.status = statusFilter;
      } else if (statusFilter === "ENDED") {
        params.ended = true;
      }
      if (selectedCatIds.size)
        params.categoryIds = Array.from(selectedCatIds).join(",");
      const fromIso = calcFromDateParam(timeFilter);
      if (fromIso) params.from = fromIso;

      console.log("Fetching Following Auctions - Params:", params);

      try {
        // --- Call the NEW Backend Endpoint ---
        const response = await apiClient.get(
          "notifications/following-auctions",
          { params }
        ); // Use the new aggregated endpoint

        const pageData = response.data; // Assuming backend returns Page or PagedResultDto
        // Assuming backend returns a field like 'auctionType' in each content item
        setFollowingAuctions(pageData.content || []);
        setPagination((p) => ({
          ...p,
          page: pageData.number ?? pageToFetch,
          totalPages: pageData.totalPages || 0,
        }));
        setError(""); // Clear error on success
      } catch (err) {
        console.error("Failed to fetch following auctions", err);
        setError(
          err.response?.data?.message || "Unable to load followed auctions."
        );
        setFollowingAuctions([]); // Clear data on error
        setPagination((p) => ({ ...p, page: 0, totalPages: 0 })); // Reset pagination on error
      } finally {
        setIsLoading(false); // Set single loading state off
      }
    },
    [
      initialized,
      keycloak.authenticated,
      pagination.size, // Depends on size
      statusFilter,
      selectedCatIds,
      timeFilter, // Depends on filters
      // NOTE: Does NOT depend on pagination.page or the data/error/loading states
    ]
  );

  /* ------------------------------ effects -------------------------------*/
  /* --- Effect to Fetch Auctions on Filter/Page Change --- */
  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      console.log("Triggering fetch following due to dependency change...");
      // Fetch with the *current* page number from state
      fetchFollowingAuctions(pagination.page);
    } else {
      // Clear data if user logs out etc.
      setFollowingAuctions([]);
      setIsLoading(false);
      setError("");
    }
  }, [
    initialized,
    keycloak.authenticated, // Re-fetch on auth change
    pagination.page, // Re-fetch if page changes
    fetchFollowingAuctions, // Re-fetch if the fetch function itself changes (due to filters/size/auth change)
  ]);

  /* -------------------------- render helpers ----------------------------*/
  const handleCardClick = useCallback(
    (id, type) => {
      if (!type) {
        console.error("Auction type missing for navigation:", id);
        // Maybe default to one type or show error?
        return;
      }
      const path =
        type.toUpperCase() === "LIVE"
          ? `/live-auctions/${id}`
          : `/timed-auctions/${id}`;
      navigate(path);
    },
    [navigate]
  );

  // ─── Unfollow handlers ──────────────────────────────────────────────
  const promptUnfollow = useCallback((auction) => {
    setAuctionToUnfollow(auction);
    setUnfollowError("");
    setIsUnfollowConfirmOpen(true);
  }, []);

  const handleCloseUnfollowConfirm = useCallback(() => {
    setIsUnfollowConfirmOpen(false);
    setAuctionToUnfollow(null);
  }, []);

  const handleConfirmUnfollow = useCallback(async () => {
    if (!auctionToUnfollow) return;
    setUnfollowLoading(true);
    try {
      await apiClient.delete(`/notifications/follow/${auctionToUnfollow.id}`);
      // remove from list instantly
      setFollowingAuctions((prev) =>
        prev.filter((a) => a.id !== auctionToUnfollow.id)
      );
      handleCloseUnfollowConfirm();
    } catch (err) {
      setUnfollowError(
        err.response?.data?.message || "Failed to unfollow auction."
      );
    } finally {
      setUnfollowLoading(false);
    }
  }, [auctionToUnfollow, handleCloseUnfollowConfirm]);

  // --- Pagination Handlers ---
  const handlePageChange = useCallback(
    (newPage) => {
      if (newPage !== pagination.page) {
        setPagination((p) => ({ ...p, page: newPage }));
      }
    },
    [pagination.page]
  );

  /* ------------------------------- UI ----------------------------------*/
  return (
    <div className="flex flex-grow" style={{ height: "calc(100vh - 4rem)" }}>
      {/* Sidebar (No Change) */}
      <aside className="w-60 md:w-72 flex-shrink-0 bg-white p-4 border-r overflow-y-auto">
        <h3 className="text-lg font-semibold border-b pb-2 mb-4">
          Filter by Category
        </h3>
        <CategorySelector
          categories={allCategories}
          selectedIds={selectedCatIds}
          onSelectionChange={(newSet) => {
            setSelectedCatIds(newSet);
            setPagination((p) => ({ ...p, page: 0 })); // Reset to first page
          }}
          isLoading={catLoading}
          error={catError}
        />
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-y-auto bg-gray-50">
        {/* Top Bar (No Change) */}
        <div className="border-b bg-white px-6 py-3 flex flex-wrap items-center gap-6 sticky top-0 z-10">
          <div className="flex gap-4">
            {" "}
            {/* Tabs */}
            {STATUS_TABS.map((t) => (
              <button
                key={t.key}
                onClick={() => {
                  setStatusFilter(t.key);
                  setPagination((p) => ({ ...p, page: 0 })); // Reset to first page
                }}
                className={`text-sm font-medium pb-1 border-b-2 transition-colors ${
                  statusFilter === t.key
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-gray-600 hover:text-indigo-600"
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div className="ml-auto flex items-center gap-2 text-sm">
            {" "}
            {/* Time */}
            <label htmlFor="tFilter" className="text-gray-600">
              Time:
            </label>
            <select
              id="tFilter"
              value={timeFilter}
              onChange={(e) => {
                setTimeFilter(e.target.value);
                setPagination((p) => ({ ...p, page: 0 })); // Reset to first page
              }}
              className="border rounded px-2 py-1 text-sm"
            >
              {TIME_FILTERS.map((f) => (
                <option key={f.key} value={f.key}>
                  {f.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="p-6 flex-1 overflow-y-auto">
          {/* Page Title */}
          <h1 className="text-2xl font-semibold mb-6">
            Auctions I'm Following
          </h1>

          {/* Loading / Error / Empty States */}
          {isLoading && (
            <div className="text-center p-10">Loading followed auctions…</div>
          )}
          {error && (
            <div className="text-center p-10 text-red-600">{error}</div>
          )}
          {!isLoading && !error && followingAuctions.length === 0 && (
            <div className="text-center p-10 border rounded bg-white shadow-sm">
              <p className="text-gray-500">
                You are not following any auctions that match the current
                filters.
              </p>
            </div>
          )}

          {/* Results Grid */}
          {!isLoading && !error && followingAuctions.length > 0 && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {followingAuctions.map((a) => (
                  <div key={`${a.auctionType}-${a.id}`} className="relative">
                    {/* trash-can button */}
                    <button
                      onClick={() => promptUnfollow(a)}
                      className="absolute top-2 right-2 p-2 bg-white/80 hover:bg-white text-red-600 hover:text-red-700 rounded-full shadow transition"
                      title="Unfollow auction"
                    >
                      <FaTrash size="1em" />
                    </button>

                    <AuctionCard
                      auction={a}
                      type={a.auctionType}
                      onClick={handleCardClick}
                    />
                  </div>
                ))}
              </div>
              {/* Single Pagination Control */}
              <PaginationControls
                pagination={pagination}
                onPageChange={handlePageChange}
                isLoading={isLoading}
              />
            </>
          )}
        </div>
      </div>
      <ConfirmationModal
        isOpen={isUnfollowConfirmOpen}
        onClose={handleCloseUnfollowConfirm}
        onConfirm={handleConfirmUnfollow}
        title="Confirm Unfollow Auction"
        message="Are you sure you want to unfollow this auction?"
        confirmText="Yes, Unfollow"
        cancelText="No, Keep Following"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={unfollowLoading}
        error={unfollowError}
      />
    </div>
  );
}

export default MyAuctionsPage;
