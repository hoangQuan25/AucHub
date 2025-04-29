// src/pages/MyAuctionsPage.jsx – revised to avoid enum conversion error
import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate } from "react-router-dom";
import apiClient from "../api/apiClient";
import CountdownTimer from "../components/CountdownTimer";
import CategorySelector from "../components/CategorySelector";

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
  const [auctions, setAuctions] = useState([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 12,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

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

  const fetchAuctions = useCallback(async () => {
    if (!(initialized && keycloak.authenticated)) return;
    setIsLoading(true);
    setError("");
    try {
      const params = {
        page: pagination.page,
        size: pagination.size,
      };
      /* status / ended handling */
      if (statusFilter === "ACTIVE" || statusFilter === "SCHEDULED") {
        params.status = statusFilter; // enum value understood by backend
      } else if (statusFilter === "ENDED") {
        params.ended = true; // custom flag – backend groups SOLD/RESERVE_NOT_MET/CANCELLED
      }
      /* category filter */
      if (selectedCatIds.size)
        params.categoryIds = Array.from(selectedCatIds).join(",");
      /* time filter */
      const fromIso = calcFromDateParam(timeFilter);
      if (fromIso) params.from = fromIso;

      const { data } = await apiClient.get("/liveauctions/my-auctions", {
        params,
      });
      setAuctions(data.content || []);
      setPagination((p) => ({ ...p, totalPages: data.totalPages || 0 }));
    } catch (e) {
      console.error("Failed to fetch my auctions", e);
      setError(e.response?.data?.message || "Unable to load auctions.");
      setAuctions([]);
    } finally {
      setIsLoading(false);
    }
  }, [
    initialized,
    keycloak.authenticated,
    pagination.page,
    pagination.size,
    statusFilter,
    selectedCatIds,
    timeFilter,
  ]);

  /* ------------------------------ effects -------------------------------*/
  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);
  useEffect(() => {
    fetchAuctions();
  }, [fetchAuctions]);

  /* -------------------------- render helpers ----------------------------*/
  const filteredAuctions = useMemo(() => auctions, [auctions]);
  const handleCardClick = (id) => navigate(`/live-auctions/${id}`);

  const pageControls = (
    <div className="flex justify-center items-center gap-4 my-6">
      <button
        className="px-4 py-2 bg-white border rounded disabled:opacity-50"
        disabled={pagination.page === 0 || isLoading}
        onClick={() => setPagination((p) => ({ ...p, page: p.page - 1 }))}
      >
        Previous
      </button>
      <span className="text-sm text-gray-600">
        Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
      </span>
      <button
        className="px-4 py-2 bg-white border rounded disabled:opacity-50"
        disabled={pagination.page >= pagination.totalPages - 1 || isLoading}
        onClick={() => setPagination((p) => ({ ...p, page: p.page + 1 }))}
      >
        Next
      </button>
    </div>
  );

  /* ------------------------------- UI ----------------------------------*/
  return (
    <div className="flex flex-grow" style={{ height: "calc(100vh - 4rem)" }}>
      {/* sidebar */}
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

      {/* main content */}
      <div className="flex-1 flex flex-col overflow-y-auto bg-gray-50">
        {/* top bar */}
        <div className="border-b bg-white px-6 py-3 flex flex-wrap items-center gap-6 sticky top-0 z-10">
          {/* tabs */}
          <div className="flex gap-4">
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
          {/* time range */}
          <div className="ml-auto flex items-center gap-2 text-sm">
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

        {/* body */}
        <div className="p-6 flex-1 overflow-y-auto">
          {isLoading && (
            <div className="text-center p-10">Loading auctions…</div>
          )}
          {error && (
            <div className="text-center p-10 text-red-600">{error}</div>
          )}

          {!isLoading && !error && filteredAuctions.length === 0 && (
            <div className="text-center p-10 border rounded bg-white shadow-sm">
              <p className="text-gray-500">
                No auctions match current filters.
              </p>
            </div>
          )}

          {!isLoading && !error && filteredAuctions.length > 0 && (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {filteredAuctions.map((a) => (
                  <div
                    key={a.id}
                    className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer flex flex-col overflow-hidden"
                    onClick={() => handleCardClick(a.id)}
                  >
                    <div className="w-full h-44 bg-gray-200">
                      <img
                        src={a.productImageUrlSnapshot || "/placeholder.png"}
                        alt={a.productTitleSnapshot}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    </div>
                    <div className="p-4 flex flex-col flex-1">
                      <h3
                        className="font-semibold text-sm mb-1 truncate"
                        title={a.productTitleSnapshot}
                      >
                        {a.productTitleSnapshot}
                      </h3>
                      <p className="text-xs text-gray-500 mb-2">
                        Status: {a.status}
                      </p>
                      {a.fastFinishOnReserve && (
                        <span className="ml-1 inline-block text-[10px] bg-purple-600 text-white px-1.5 py-0.5 rounded">
                          Fast-finish
                        </span>
                      )}
                      <div className="mt-auto border-t pt-2 text-xs text-gray-600 grid grid-cols-2 gap-2">
                        <span>Current Bid:</span>
                        <span className="text-right font-medium">
                          {(a.currentBid ?? 0).toLocaleString("vi-VN")} VNĐ
                        </span>
                        <span>Ends In:</span>
                        <span className="text-right">
                          {a.status === "ACTIVE" ? (
                            <CountdownTimer
                              endTimeMillis={new Date(a.endTime).getTime()}
                            />
                          ) : (
                            new Date(a.endTime).toLocaleString()
                          )}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              {pagination.totalPages > 1 && pageControls}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default MyAuctionsPage;
