// src/pages/AuctionSearchPage.jsx
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import apiClient from '../api/apiClient';

import CategorySelector from '../components/CategorySelector';
import AuctionFilters from '../components/seller/tab/listing/AuctionFilters'; // Reusable
import LiveAuctionList from '../components/seller/tab/listing/SellerLiveAuctions';
import TimedAuctionList from '../components/seller/tab/listing/SellerTimedAuctions';

const STATUS_TABS = [
  { key: "ALL", label: "All Statuses" }, // "All Auctions" label might be confusing with type filter
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

const LISTING_PAGE_SIZE = 12;

// Helper function to calculate 'from' date for backend (can be imported or defined here)
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


function AuctionSearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // --- Parse initial state from URL ---
  const query = useMemo(() => searchParams.get('query') || '', [searchParams]);
  const typeFilter = useMemo(() => searchParams.get('type')?.toUpperCase() || 'ALL', [searchParams]); // Ensure uppercase for consistency

  const initialCategoryIds = useMemo(() => {
    const catStr = searchParams.get('categories');
    return catStr ? new Set(catStr.split(',').map(id => parseInt(id, 10)).filter(id => !isNaN(id))) : new Set();
  }, [searchParams]);

  const initialStatusFilter = useMemo(() => searchParams.get('status')?.toUpperCase() || 'ALL', [searchParams]);
  const initialTimeFilter = useMemo(() => searchParams.get('timePeriod') || 'ALL', [searchParams]);
  const initialLivePage = useMemo(() => parseInt(searchParams.get('livePage') || '0', 10), [searchParams]);
  const initialTimedPage = useMemo(() => parseInt(searchParams.get('timedPage') || '0', 10), [searchParams]);

  // --- Component State ---
  const [liveAuctions, setLiveAuctions] = useState([]);
  const [timedAuctions, setTimedAuctions] = useState([]);
  const [livePagination, setLivePagination] = useState({ page: initialLivePage, totalPages: 0, totalElements: 0 });
  const [timedPagination, setTimedPagination] = useState({ page: initialTimedPage, totalPages: 0, totalElements: 0 });

  const [isLoadingLive, setIsLoadingLive] = useState(false);
  const [isLoadingTimed, setIsLoadingTimed] = useState(false);
  const [errorLive, setErrorLive] = useState('');
  const [errorTimed, setErrorTimed] = useState('');

  const [allCategories, setAllCategories] = useState([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [categoryError, setCategoryError] = useState('');

  const [selectedCategoryIds, setSelectedCategoryIds] = useState(initialCategoryIds);
  const [auctionStatusFilter, setAuctionStatusFilter] = useState(initialStatusFilter);
  const [auctionTimeFilter, setAuctionTimeFilter] = useState(initialTimeFilter);

  // --- Fetch All Categories for Sidebar ---
  const fetchAllCategories = useCallback(async () => {
    setCategoryLoading(true);
    setCategoryError('');
    try {
      const response = await apiClient.get('/products/categories'); // Assuming public endpoint
      setAllCategories(response.data || []);
    } catch (err) {
      console.error("Failed to fetch categories:", err);
      setCategoryError("Could not load categories.");
    } finally {
      setCategoryLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllCategories();
  }, [fetchAllCategories]);


  // --- Fetch Search Results ---
  const fetchSearchResults = useCallback(async (
    currentQuery, currentTypeFilter, currentCategories, currentStatusFilter, currentTimeFilter,
    currentPageLive, currentPageTimed
  ) => {
    // Avoid fetching if no query and all filters are effectively "ALL"
    if (!currentQuery && currentTypeFilter === 'ALL' && currentCategories.size === 0 && currentStatusFilter === 'ALL' && currentTimeFilter === 'ALL') {
      setLiveAuctions([]);
      setTimedAuctions([]);
      setLivePagination(prev => ({ ...prev, page: 0, totalPages: 0, totalElements: 0 }));
      setTimedPagination(prev => ({ ...prev, page: 0, totalPages: 0, totalElements: 0 }));
      setErrorLive('');
      setErrorTimed('');
      setIsLoadingLive(false); // Ensure loading states are reset
      setIsLoadingTimed(false);
      return;
    }

    const commonParams = {
      query: currentQuery || undefined,
      categoryIds: currentCategories.size > 0 ? Array.from(currentCategories).join(',') : undefined,
      from: calcFromDateParam(currentTimeFilter), // Use the calcFromDateParam function
      size: LISTING_PAGE_SIZE,
    };

    // Set 'status' and 'ended' based on auctionStatusFilter
    if (currentStatusFilter === 'ENDED') {
      commonParams.ended = true;
      // commonParams.status = undefined; // Explicitly undefined or just don't set
    } else if (currentStatusFilter !== 'ALL') {
      commonParams.status = currentStatusFilter; // e.g., ACTIVE, SCHEDULED
      // commonParams.ended = false; // Or let backend default if ended is not true
    }
    // If currentStatusFilter is 'ALL' (and not 'ENDED'), backend defaults to non-ended (ACTIVE, SCHEDULED)

    const promises = [];

    if (currentTypeFilter === 'LIVE' || currentTypeFilter === 'ALL') {
      setIsLoadingLive(true);
      setErrorLive('');
      promises.push(
        apiClient.get('/liveauctions/search', { params: { ...commonParams, page: currentPageLive } })
          .then(res => ({ type: 'live', data: res.data }))
          .catch(err => ({ type: 'live', error: err, message: err.response?.data?.message || 'Could not load live auctions.' }))
      );
    } else {
      setLiveAuctions([]);
      setLivePagination({ page: 0, totalPages: 0, totalElements: 0 });
    }

    if (currentTypeFilter === 'TIMED' || currentTypeFilter === 'ALL') {
      setIsLoadingTimed(true);
      setErrorTimed('');
      promises.push(
        apiClient.get('/timedauctions/search', { params: { ...commonParams, page: currentPageTimed } })
          .then(res => ({ type: 'timed', data: res.data }))
          .catch(err => ({ type: 'timed', error: err, message: err.response?.data?.message || 'Could not load timed auctions.' }))
      );
    } else {
      setTimedAuctions([]);
      setTimedPagination({ page: 0, totalPages: 0, totalElements: 0 });
    }

    if (promises.length === 0) {
        setIsLoadingLive(false);
        setIsLoadingTimed(false);
        return;
    }

    const results = await Promise.all(promises);

    results.forEach(result => {
      if (result.type === 'live') {
        if (result.error) {
          console.error('Failed to fetch live auction search results:', result.error);
          setErrorLive(result.message);
          setLiveAuctions([]);
        } else {
          setLiveAuctions(result.data.content || []);
          setLivePagination({ page: result.data.number, totalPages: result.data.totalPages || 0, totalElements: result.data.totalElements || 0 });
        }
        setIsLoadingLive(false);
      } else if (result.type === 'timed') {
        if (result.error) {
          console.error('Failed to fetch timed auction search results:', result.error);
          setErrorTimed(result.message);
          setTimedAuctions([]);
        } else {
          setTimedAuctions(result.data.content || []);
          setTimedPagination({ page: result.data.number, totalPages: result.data.totalPages || 0, totalElements: result.data.totalElements || 0 });
        }
        setIsLoadingTimed(false);
      }
    });
  }, [LISTING_PAGE_SIZE]);

  // --- useEffect to Sync URL with State and Fetch Data ---
  useEffect(() => {
    // Update pagination states from URL first if they changed
    const urlLivePage = parseInt(searchParams.get('livePage') || '0', 10);
    const urlTimedPage = parseInt(searchParams.get('timedPage') || '0', 10);

    let livePageChanged = false;
    let timedPageChanged = false;

    if (urlLivePage !== livePagination.page) {
        setLivePagination(p => ({...p, page: urlLivePage}));
        livePageChanged = true;
    }
    if (urlTimedPage !== timedPagination.page) {
        setTimedPagination(p => ({...p, page: urlTimedPage}));
        timedPageChanged = true;
    }
    

    fetchSearchResults(
      query, typeFilter, selectedCategoryIds, auctionStatusFilter, auctionTimeFilter,
      urlLivePage, // Use page numbers directly from URL for the fetch call
      urlTimedPage
    );

  }, [query, typeFilter, selectedCategoryIds, auctionStatusFilter, auctionTimeFilter, searchParams, fetchSearchResults]);


  // --- Handlers to Update URL and State (which then triggers fetch via useEffect) ---
  const updateUrlParams = (newValues) => {
    const currentParams = new URLSearchParams(searchParams);
    let paramsChanged = false;

    for (const key in newValues) {
      const oldValue = currentParams.get(key);
      let newValueStr = '';

      if (newValues[key] instanceof Set) {
        newValueStr = newValues[key].size > 0 ? Array.from(newValues[key]).join(',') : undefined;
      } else {
        newValueStr = newValues[key];
      }

      // Normalize 'ALL' or empty to undefined for cleaner URLs (optional)
      if (key === 'status' && newValueStr === 'ALL') newValueStr = undefined;
      if (key === 'timePeriod' && newValueStr === 'ALL') newValueStr = undefined;
      if (key === 'type' && newValueStr === 'ALL') newValueStr = undefined;
      if ((key === 'livePage' || key === 'timedPage') && (newValueStr === 0 || newValueStr === '0')) newValueStr = undefined;


      if (newValueStr === undefined || newValueStr === null || newValueStr === '') {
        if (oldValue !== null) { // Only delete if it was there
          currentParams.delete(key);
          paramsChanged = true;
        }
      } else if (String(oldValue) !== String(newValueStr)) {
        currentParams.set(key, newValueStr);
        paramsChanged = true;
      }
    }
    
    if (paramsChanged) {
      setSearchParams(currentParams, { replace: true });
    }
  };


  const handleCategoryFilterChange = useCallback((newSelectedIds) => {
    setSelectedCategoryIds(newSelectedIds);
    setLivePagination(p => ({ ...p, page: 0 })); // Reset page state
    setTimedPagination(p => ({ ...p, page: 0 }));
    updateUrlParams({ categories: newSelectedIds, livePage: undefined, timedPage: undefined });
  }, [updateUrlParams, setSearchParams]); // Added setSearchParams to dependencies of updateUrlParams

  const handleAuctionStatusFilterChange = useCallback((newStatus) => {
    setAuctionStatusFilter(newStatus);
    setLivePagination(p => ({ ...p, page: 0 }));
    setTimedPagination(p => ({ ...p, page: 0 }));
    updateUrlParams({ status: newStatus, livePage: undefined, timedPage: undefined });
  }, [updateUrlParams, setSearchParams]);

  const handleAuctionTimeFilterChange = useCallback((newTime) => {
    setAuctionTimeFilter(newTime);
    setLivePagination(p => ({ ...p, page: 0 }));
    setTimedPagination(p => ({ ...p, page: 0 }));
    updateUrlParams({ timePeriod: newTime, livePage: undefined, timedPage: undefined });
  }, [updateUrlParams, setSearchParams]);

  const handleLivePageChange = useCallback((newPage) => {
    setLivePagination(p => ({ ...p, page: newPage })); // Update local state for pagination component
    updateUrlParams({ livePage: newPage });
  }, [updateUrlParams, setSearchParams]);

  const handleTimedPageChange = useCallback((newPage) => {
    setTimedPagination(p => ({ ...p, page: newPage })); // Update local state
    updateUrlParams({ timedPage: newPage });
  }, [updateUrlParams, setSearchParams]);

  const handleAuctionCardClick = useCallback((id, auctionTypeClicked) => {
      const path = auctionTypeClicked === "LIVE" ? `/live-auctions/${id}` : `/timed-auctions/${id}`;
      navigate(path);
  }, [navigate]);

  const displayQuery = searchParams.get('query') || ''; // Get fresh query from URL for display

  return (
    <div className="flex flex-grow bg-gray-100" style={{ height: "calc(100vh - 4rem)" }}> {/* Adjust for header */}
      {/* Filter Sidebar */}
      <aside className="w-60 md:w-72 flex-shrink-0 bg-white p-4 border-r overflow-y-auto shadow-sm">
        <h3 className="text-lg font-semibold border-b pb-2 mb-4 text-gray-700">
          Filter by Category
        </h3>
        <CategorySelector
          categories={allCategories}
          selectedIds={selectedCategoryIds} // Controlled by state updated from URL
          onSelectionChange={handleCategoryFilterChange}
          isLoading={categoryLoading}
          error={categoryError}
        />
      </aside>

      {/* Main Content Area */}
      <div className="flex-grow p-4 sm:p-6 bg-gray-50 overflow-y-auto">
        <h1 className="text-2xl sm:text-3xl font-bold mb-2 text-gray-800">
          Auction Search Results
        </h1>
        {displayQuery && (
          <p className="text-md text-gray-600 mb-6">
            Showing results for: "<span className="font-semibold text-indigo-700">{displayQuery}</span>"
            {typeFilter !== 'ALL' && ` in ${typeFilter.toLowerCase()} auctions`}
          </p>
        )}

        <AuctionFilters
          auctionStatusFilter={auctionStatusFilter} // Controlled by state updated from URL
          setAuctionStatusFilter={handleAuctionStatusFilterChange}
          auctionTimeFilter={auctionTimeFilter} // Controlled by state updated from URL
          setAuctionTimeFilter={handleAuctionTimeFilterChange}
          statusTabs={STATUS_TABS}
          timeFilters={TIME_FILTERS} 
          onFilterChange={() => { /* Logic now in individual handlers to reset pagination */ }}
        />

        {(isLoadingLive || isLoadingTimed) && <div className="text-center p-10 text-gray-600">Loading results...</div>}
        {errorLive && <div className="text-center p-4 my-4 text-red-600 bg-red-50 rounded-md border border-red-200">{errorLive}</div>}
        {errorTimed && <div className="text-center p-4 my-4 text-red-600 bg-red-50 rounded-md border border-red-200">{errorTimed}</div>}

        {!(isLoadingLive || isLoadingTimed) && (
          <>
            {/* Live Auctions Section */}
            {(typeFilter === 'LIVE' || typeFilter === 'ALL') && liveAuctions.length > 0 && (
              <LiveAuctionList
                liveAuctions={liveAuctions}
                isLoadingLiveAuctions={isLoadingLive}
                errorLiveAuctions={errorLive} // Pass specific error
                livePagination={livePagination}
                listingPageSize={LISTING_PAGE_SIZE}
                onLiveAuctionPageChange={handleLivePageChange}
                onAuctionCardClick={handleAuctionCardClick}
                isOwner={false}
              />
            )}

            {/* Timed Auctions Section */}
            {(typeFilter === 'TIMED' || typeFilter === 'ALL') && timedAuctions.length > 0 && (
              <TimedAuctionList
                timedAuctions={timedAuctions}
                isLoadingTimedAuctions={isLoadingTimed}
                errorTimedAuctions={errorTimed} // Pass specific error
                timedPagination={timedPagination}
                listingPageSize={LISTING_PAGE_SIZE}
                onTimedAuctionPageChange={handleTimedPageChange}
                onAuctionCardClick={handleAuctionCardClick}
                isOwner={false}
              />
            )}

            {/* No Results Message */}
            {!isLoadingLive && !isLoadingTimed && !errorLive && !errorTimed &&
             liveAuctions.length === 0 && timedAuctions.length === 0 &&
             (query || selectedCategoryIds.size > 0 || auctionStatusFilter !== 'ALL' || auctionTimeFilter !== 'ALL' || typeFilter !== 'ALL') &&
             (
                 <div className="text-center p-10 border rounded bg-white shadow-sm mt-6">
                    <p className="text-gray-500 text-lg">No auctions found matching your criteria.</p>
                    <p className="text-sm text-gray-400 mt-2">Try adjusting your search or filters.</p>
                </div>
            )}
            {/* Initial state prompt if no query and no filters active */}
             {!isLoadingLive && !isLoadingTimed && !errorLive && !errorTimed &&
             liveAuctions.length === 0 && timedAuctions.length === 0 &&
             !query && selectedCategoryIds.size === 0 && auctionStatusFilter === 'ALL' && auctionTimeFilter === 'ALL' && typeFilter === 'ALL' &&
             (
                 <div className="text-center p-10 border rounded bg-white shadow-sm mt-6">
                    <p className="text-gray-500 text-lg">Use the search bar above or filters to find auctions.</p>
                </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
export default AuctionSearchPage;