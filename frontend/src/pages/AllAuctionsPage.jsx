// src/pages/AllAuctionsPage.jsx (Renamed from LiveAuctionsPage)
import React, { useState, useCallback, useEffect } from "react";
import { FaHeart, FaRegHeart } from 'react-icons/fa';
import { useNotifications } from '../context/NotificationContext';
import { useKeycloak } from '@react-keycloak/web';
import { useNavigate } from "react-router-dom";
import CountdownTimer from "../components/CountdownTimer";
import apiClient from "../api/apiClient";

// Reusable component for rendering an auction card
function AuctionCard({ auction, type }) {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const { followedAuctionIds, followAuction, unfollowAuction } = useNotifications();
  const isFollowed = followedAuctionIds?.has(auction.id);

  const handleViewAuction = (auctionId) => {
    // Navigate to the correct detail page based on type
    const detailPath = type === 'LIVE' ? `/live-auctions/${auctionId}` : `/timed-auctions/${auctionId}`;
    navigate(detailPath);
  };

  // --- Handler for clicking the heart icon ---
  const handleFollowToggle = (event) => {
    event.stopPropagation(); // VERY IMPORTANT: Stop click from propagating to the card's handler
    if (!keycloak.authenticated) return; // Should not happen if button not rendered, but safe check

    if (isFollowed) {
      console.log(`Requesting unfollow for auction ${auction.id}`);
      unfollowAuction(auction.id); // Call context function (type might not be needed for unfollow)
    } else {
      console.log(`Requesting follow for auction ${auction.id} type ${type}`);
      followAuction(auction.id, type); // Call context function
    }
  };

  return (
    <div
      key={auction.id}
      className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden flex flex-col relative"
      onClick={() => handleViewAuction(auction.id)}
    >
      {/* Image */}
      <div className="w-full h-48 bg-gray-200 flex-shrink-0 relative">
        <img
          src={auction.productImageUrlSnapshot || "/placeholder.png"}
          alt={auction.productTitleSnapshot}
          className="w-full h-full object-cover"
          loading="lazy"
        />
      </div>

      {keycloak.authenticated && ( 
             <button
                onClick={handleFollowToggle}
                className={`absolute top-2 right-2 p-1.5 rounded-full transition-colors duration-150 z-10 ${
                    isFollowed
                    ? 'bg-red-500 text-white hover:bg-red-600' // Style for followed
                    : 'bg-black/30 text-white hover:bg-black/50' // Style for not followed
                }`}
                aria-label={isFollowed ? 'Unfollow Auction' : 'Follow Auction'}
                title={isFollowed ? 'Unfollow Auction' : 'Follow Auction'}
             >
                 {isFollowed ? <FaHeart size="1em"/> : <FaRegHeart size="1em"/>}
             </button>
        )}

      {/* Details */}
      <div className="p-4 flex flex-col flex-grow">
        <h3
          className="font-semibold text-md mb-1 truncate"
          title={auction.productTitleSnapshot}
        >
          {auction.productTitleSnapshot}
        </h3>
        <p className="text-sm font-bold text-indigo-600 mb-1">
           {/* Display currency correctly - Assuming VND */}
          Current Bid: {(auction.currentBid ?? 0).toLocaleString("vi-VN")} VNĐ
        </p>
        <div className="mt-auto">
            <p className="text-s font-medium"> {/* Removed text-red-600 here, timer handles color */}
                <CountdownTimer endTimeMillis={new Date(auction.endTime).getTime()} />
            </p>
        </div>
      </div>
    </div>
  );
}

// Reusable component for pagination controls
function PaginationControls({ pagination, onPageChange, isLoading }) {
  if (pagination.totalPages <= 1) return null; // Don't show if only one page

  const handlePrevPage = () => onPageChange(pagination.page - 1);
  const handleNextPage = () => onPageChange(pagination.page + 1);

  return (
    <div className="flex justify-center items-center space-x-4 mt-8">
      <button
        onClick={handlePrevPage}
        disabled={pagination.page === 0 || isLoading}
        className="px-4 py-2 border rounded bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Previous
      </button>
      <span className="text-sm text-gray-600">
        Page {pagination.page + 1} of {pagination.totalPages}
      </span>
      <button
        onClick={handleNextPage}
        disabled={pagination.page >= pagination.totalPages - 1 || isLoading}
        className="px-4 py-2 border rounded bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Next
      </button>
    </div>
  );
}


// Main Page Component
function AllAuctionsPage() {
  const navigate = useNavigate(); // Keep for potential future use

  // --- State for BOTH auction types ---
  const [liveAuctions, setLiveAuctions] = useState([]);
  const [timedAuctions, setTimedAuctions] = useState([]);

  const [livePagination, setLivePagination] = useState({ page: 0, size: 4, totalPages: 0 }); // Smaller initial size?
  const [timedPagination, setTimedPagination] = useState({ page: 0, size: 4, totalPages: 0 });

  const [isLoadingLive, setIsLoadingLive] = useState(true);
  const [isLoadingTimed, setIsLoadingTimed] = useState(true);
  const [errorLive, setErrorLive] = useState("");
  const [errorTimed, setErrorTimed] = useState("");

  // Combined loading state for initial load indicator
  const isLoading = isLoadingLive || isLoadingTimed;
  // --- End State ---


  // --- Fetching Logic ---
  const fetchLiveAuctions = useCallback(async (page = 0, size = 4) => {
    setIsLoadingLive(true);
    setErrorLive("");
    try {
      // Use the correct endpoint for live auctions
      const response = await apiClient.get("/liveauctions/live-auctions", {
        params: { page, size, sort: "endTime,asc" },
      });
      const pageData = response.data;
      setLiveAuctions(pageData.content || []);
      setLivePagination({
        page: pageData.number,
        size: pageData.size,
        totalPages: pageData.totalPages,
      });
    } catch (err) {
      console.error("Failed to fetch live auctions:", err);
      setErrorLive(err.response?.data?.message || "Could not load live auctions.");
      setLiveAuctions([]);
    } finally {
      setIsLoadingLive(false);
    }
  }, []);

  const fetchTimedAuctions = useCallback(async (page = 0, size = 4) => {
    setIsLoadingTimed(true);
    setErrorTimed("");
    try {
      // Use the NEW endpoint for active timed auctions
      const response = await apiClient.get("/timedauctions/timed-auctions", { // Assuming base endpoint with status filter
        params: { page, size, sort: "endTime,asc", status: "ACTIVE" }, // Explicitly request ACTIVE status
      });
      const pageData = response.data;
      setTimedAuctions(pageData.content || []);
      setTimedPagination({
        page: pageData.number,
        size: pageData.size,
        totalPages: pageData.totalPages,
      });
    } catch (err) {
      console.error("Failed to fetch timed auctions:", err);
      setErrorTimed(err.response?.data?.message || "Could not load timed auctions.");
      setTimedAuctions([]);
    } finally {
      setIsLoadingTimed(false);
    }
  }, []);

  // Initial fetch for both types
  useEffect(() => {
    fetchLiveAuctions(livePagination.page, livePagination.size);
    fetchTimedAuctions(timedPagination.page, timedPagination.size);
  }, [fetchLiveAuctions, fetchTimedAuctions]); // Only depends on the functions themselves

  // Handlers to change page for each type
  const handleLivePageChange = (newPage) => {
    fetchLiveAuctions(newPage, livePagination.size);
  };

  const handleTimedPageChange = (newPage) => {
    fetchTimedAuctions(newPage, timedPagination.size);
  };
  // --- End Fetching Logic ---


  return (
    <div className="p-6 max-w-7xl mx-auto">
      <h1 className="text-3xl font-bold mb-8 text-center">Ongoing Auctions</h1>

      {/* Combined Loading Indicator for initial load */}
      {isLoading && !errorLive && !errorTimed && (
        <div className="text-center p-10">Loading auctions...</div>
      )}

      {/* Live Auctions Section */}
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-4 border-b pb-2">Live Auctions</h2>
        {isLoadingLive && <div className="text-center p-4">Loading...</div>}
        {errorLive && <div className="p-4 text-red-600 text-center">{errorLive}</div>}
        {!isLoadingLive && !errorLive && liveAuctions.length === 0 && (
          <p className="text-center text-gray-500 py-4">No live auctions currently active.</p>
        )}
        {!isLoadingLive && !errorLive && liveAuctions.length > 0 && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {liveAuctions.map((auction) => (
                <AuctionCard key={auction.id} auction={auction} type="LIVE" />
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
        <h2 className="text-2xl font-semibold mb-4 border-b pb-2">Timed Auctions</h2>
        {isLoadingTimed && <div className="text-center p-4">Loading...</div>}
        {errorTimed && <div className="p-4 text-red-600 text-center">{errorTimed}</div>}
        {!isLoadingTimed && !errorTimed && timedAuctions.length === 0 && (
          <p className="text-center text-gray-500 py-4">No timed auctions currently active.</p>
        )}
        {!isLoadingTimed && !errorTimed && timedAuctions.length > 0 && (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {timedAuctions.map((auction) => (
                <AuctionCard key={auction.id} auction={auction} type="TIMED" />
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
  );
}

export default AllAuctionsPage; // Rename the export