// src/pages/MyAuctionsPage.jsx – revised to avoid enum conversion error
import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate } from "react-router-dom";
import apiClient from "../api/apiClient";
import AuctionCard from "../components/AuctionCard";
import CategorySelector from "../components/CategorySelector";
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
  const [liveAuctions, setLiveAuctions] = useState([]);
  const [timedAuctions, setTimedAuctions] = useState([]);
  const [livePagination, setLivePagination] = useState({
    page: 0,
    size: 6,
    totalPages: 0,
  });
  const [timedPagination, setTimedPagination] = useState({
    page: 0,
    size: 6,
    totalPages: 0,
  });

  const [isLoadingLive, setIsLoadingLive] = useState(true);
  const [isLoadingTimed, setIsLoadingTimed] = useState(true);
  const [errorLive, setErrorLive] = useState("");
  const [errorTimed, setErrorTimed] = useState("");

  // Combined Loading for initial view
  const isLoading = isLoadingLive || isLoadingTimed;

  const [statusFilter, setStatusFilter] = useState("ALL");
  const [timeFilter, setTimeFilter] = useState("ALL");

  const [allCategories, setAllCategories] = useState([]);
  const [catLoading, setCatLoading] = useState(false);
  const [catError, setCatError] = useState("");
  const [selectedCatIds, setSelectedCatIds] = useState(new Set());

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
  const fetchAllMyAuctions = useCallback(
    async (
      livePage = livePagination.page,
      timedPage = timedPagination.page
    ) => {
      if (!(initialized && keycloak.authenticated)) return;

      // Set loading states only if not already loading (avoids flicker on filter change)
      if (!isLoadingLive) setIsLoadingLive(true);
      if (!isLoadingTimed) setIsLoadingTimed(true);
      setErrorLive("");
      setErrorTimed("");

      // --- Prepare Common Params ---
      const commonParams = {};
      // Status / ended handling
      if (statusFilter === "ACTIVE" || statusFilter === "SCHEDULED") {
        commonParams.status = statusFilter;
      } else if (statusFilter === "ENDED") {
        commonParams.ended = true; // Use boolean flag
      }
      // Category filter
      if (selectedCatIds.size)
        commonParams.categoryIds = Array.from(selectedCatIds).join(",");
      // Time filter
      const fromIso = calcFromDateParam(timeFilter);
      if (fromIso) commonParams.from = fromIso;

      // --- API Calls ---
      const livePromise = apiClient
        .get("/liveauctions/my-auctions", {
          params: {
            ...commonParams,
            page: livePage,
            size: livePagination.size,
          },
        })
        .catch((err) => ({
          error: true,
          type: "live",
          message:
            err.response?.data?.message || "Unable to load live auctions.",
          errorObj: err,
        })); // Catch errors individually

      const timedPromise = apiClient
        .get("/timedauctions/my-auctions", {
          // New endpoint
          params: {
            ...commonParams,
            page: timedPage,
            size: timedPagination.size,
          },
        })
        .catch((err) => ({
          error: true,
          type: "timed",
          message:
            err.response?.data?.message || "Unable to load timed auctions.",
          errorObj: err,
        })); // Catch errors individually

      // --- Process Results ---
      const [liveResult, timedResult] = await Promise.all([
        livePromise,
        timedPromise,
      ]);

      // Process Live Auctions
      if (liveResult.error) {
        console.error("Failed to fetch my live auctions", liveResult.errorObj);
        setErrorLive(liveResult.message);
        setLiveAuctions([]);
      } else {
        setLiveAuctions(liveResult.data.content || []);
        setLivePagination((p) => ({
          ...p,
          page: liveResult.data.number,
          totalPages: liveResult.data.totalPages || 0,
        }));
      }
      setIsLoadingLive(false);

      // Process Timed Auctions
      if (timedResult.error) {
        console.error(
          "Failed to fetch my timed auctions",
          timedResult.errorObj
        );
        setErrorTimed(timedResult.message);
        setTimedAuctions([]);
      } else {
        setTimedAuctions(timedResult.data.content || []);
        setTimedPagination((p) => ({
          ...p,
          page: timedResult.data.number,
          totalPages: timedResult.data.totalPages || 0,
        }));
      }
      setIsLoadingTimed(false);
    },
    [
      // --- STABLE DEPENDENCIES ---
      // Dependencies that define the query parameters or auth state:
      initialized,
      keycloak.authenticated,
      livePagination.size, // Depend on size used in query
      timedPagination.size, // Depend on size used in query
      statusFilter,
      selectedCatIds,
      timeFilter,
    ]
  );

  /* ------------------------------ effects -------------------------------*/
  /* --- Effect to Fetch Auctions on Filter/Page Change --- */
  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      console.log("Triggering fetch due to auth/page/filter change...");
      fetchAllMyAuctions(livePagination.page, timedPagination.page);
    } else {
      setLiveAuctions([]);
      setTimedAuctions([]);
      setIsLoadingLive(false);
      setIsLoadingTimed(false);
    }
  }, [
    initialized,
    keycloak.authenticated,
    livePagination.page,
    timedPagination.page,
    statusFilter,
    selectedCatIds,
    timeFilter,
  ]);

  /* -------------------------- render helpers ----------------------------*/
  const handleCardClick = useCallback(
    (id, type) => {
      const path =
        type === "LIVE" ? `/live-auctions/${id}` : `/timed-auctions/${id}`;
      navigate(path);
    },
    [navigate]
  );

  // --- Pagination Handlers ---
  const handleLivePageChange = useCallback(
    (newPage) => {
      if (newPage !== livePagination.page) {
        setLivePagination((p) => ({ ...p, page: newPage }));
      }
    },
    [livePagination.page]
  );

  const handleTimedPageChange = useCallback(
    (newPage) => {
      if (newPage !== timedPagination.page) {
        setTimedPagination((p) => ({ ...p, page: newPage }));
      }
    },
    [timedPagination.page]
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
          onSelectionChange={setSelectedCatIds}
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
                onClick={() => setStatusFilter(t.key)}
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
              onChange={(e) => setTimeFilter(e.target.value)}
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

        {/* Body - Split into Sections */}
        <div className="p-6 flex-1 overflow-y-auto">
          {isLoading && (
            <div className="text-center p-10">Loading auctions…</div>
          )}

          {/* Live Auctions Section */}
          <section className="mb-10">
            <h2 className="text-xl font-semibold mb-4">My Live Auctions</h2>
            {isLoadingLive && !isLoading && (
              <div className="text-center p-4 text-sm text-gray-500">
                Loading live auctions...
              </div>
            )}
            {errorLive && (
              <div className="text-center p-4 text-red-600">{errorLive}</div>
            )}
            {!isLoadingLive && !errorLive && liveAuctions.length === 0 && (
              <div className="text-center p-4 border rounded bg-white text-gray-500">
                No live auctions match current filters.
              </div>
            )}
            {!isLoadingLive && !errorLive && liveAuctions.length > 0 && (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {liveAuctions.map((a) => (
                    <AuctionCard
                      key={a.id}
                      auction={a}
                      type="LIVE"
                      onClick={handleCardClick}
                    />
                  ))}
                </div>
                <PaginationControls
                  pagination={livePagination}
                  onPageChange={handleLivePageChange}
                  isLoading={isLoadingLive}
                />
              </>
            )}
          </section>

          {/* Timed Auctions Section */}
          <section>
            <h2 className="text-xl font-semibold mb-4">My Timed Auctions</h2>
            {isLoadingTimed && !isLoading && (
              <div className="text-center p-4 text-sm text-gray-500">
                Loading timed auctions...
              </div>
            )}
            {errorTimed && (
              <div className="text-center p-4 text-red-600">{errorTimed}</div>
            )}
            {!isLoadingTimed && !errorTimed && timedAuctions.length === 0 && (
              <div className="text-center p-4 border rounded bg-white text-gray-500">
                No timed auctions match current filters.
              </div>
            )}
            {!isLoadingTimed && !errorTimed && timedAuctions.length > 0 && (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {timedAuctions.map((a) => (
                    <AuctionCard
                      key={a.id}
                      auction={a}
                      type="TIMED"
                      onClick={handleCardClick}
                    />
                  ))}
                </div>
                <PaginationControls
                  pagination={timedPagination}
                  onPageChange={handleTimedPageChange}
                  isLoading={isLoadingTimed}
                />
              </>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}

export default MyAuctionsPage;
