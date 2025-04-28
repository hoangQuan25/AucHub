// src/pages/LiveAuctionsPage.jsx (New File - Mockup)
import React, { useState, useCallback, useEffect } from "react";
import { useNavigate } from "react-router-dom"; // To navigate to detail page later
import CountdownTimer from "../components/CountdownTimer";
import apiClient from "../api/apiClient"; // Adjust the import path as needed



function LiveAuctionsPage() {
  const navigate = useNavigate();

  // --- NEW STATE ---
  const [auctions, setAuctions] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [pagination, setPagination] = useState({
    page: 0, // Spring Data Page is 0-indexed
    size: 12, // Default page size
    totalPages: 0,
    totalElements: 0,
  });
  // --- END NEW STATE ---

  // Function to fetch auctions
  const fetchAuctions = useCallback(async (page = 0, size = 12) => {
    setIsLoading(true);
    setError("");
    try {
      const response = await apiClient.get("/liveauctions/live-auctions", {
        params: {
          page: page,
          size: size,
          sort: "endTime,asc", // Sort by soonest ending first (adjust as needed)
        },
      });

      // Assuming backend returns Spring Page object structure
      const pageData = response.data;
      if (pageData && pageData.content) {
        setAuctions(pageData.content); // pageData.content should be LiveAuctionSummaryDto[]
        setPagination({
          page: pageData.number,
          size: pageData.size,
          totalPages: pageData.totalPages,
          totalElements: pageData.totalElements,
        });
      } else {
        // Handle unexpected response structure
        setAuctions([]);
        setPagination((prev) => ({ ...prev, totalPages: 0, totalElements: 0 }));
        setError("Failed to parse auction data.");
      }
    } catch (err) {
      console.error("Failed to fetch auctions:", err);
      setError(err.response?.data?.message || "Could not load auctions.");
      setAuctions([]); // Clear auctions on error
    } finally {
      setIsLoading(false);
    }
  }, []); // Empty dependency array means this function doesn't change

  // Initial fetch and fetch on page change
  useEffect(() => {
    fetchAuctions(pagination.page, pagination.size);
  }, [pagination.page, pagination.size, fetchAuctions]); // Re-fetch when page or size changes

  const handleViewAuction = (auctionId) => {
    // Navigate using the auction's actual UUID ID
    navigate(`/live-auctions/${auctionId}`); // Use the UUID auctionId
  };

  // --- NEW: Pagination Handlers ---
  const handleNextPage = () => {
    if (pagination.page < pagination.totalPages - 1) {
      setPagination((prev) => ({ ...prev, page: prev.page + 1 }));
    }
  };

  const handlePrevPage = () => {
    if (pagination.page > 0) {
      setPagination((prev) => ({ ...prev, page: prev.page - 1 }));
    }
  };
  // --- END Pagination Handlers ---

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Live Auctions Happening Now</h1>

      {/* Loading State */}
      {isLoading && <div className="text-center p-4">Loading auctions...</div>}

      {/* Error State */}
      {error && <div className="text-center p-4 text-red-600">{error}</div>}

      {/* Empty State (after load, no error) */}
      {!isLoading && !error && auctions.length === 0 && (
        <p className="text-center text-gray-500">
          No live auctions currently active.
        </p>
      )}

      {/* Auction Grid (Data Loaded) */}
      {!isLoading && !error && auctions.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {/* Map over FETCHER auctions state */}
            {auctions.map(
              (
                auction // auction should match LiveAuctionSummaryDto
              ) => (
                <div
                  key={auction.id} // Use the UUID from the DTO
                  className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
                  onClick={() => handleViewAuction(auction.id)} // Pass UUID
                >
                  {/* Image */}
                  <div className="w-full h-48 bg-gray-200">
                    <img
                      src={
                        auction.productImageUrlSnapshot || "/placeholder.png"
                      } // Use snapshot URL
                      alt={auction.productTitleSnapshot} // Use snapshot title
                      className="w-full h-full object-cover"
                      loading="lazy"
                    />
                  </div>
                  {/* Details */}
                  <div className="p-4">
                    <h3
                      className="font-semibold text-md mb-1 truncate"
                      title={auction.productTitleSnapshot} // Use snapshot title
                    >
                      {auction.productTitleSnapshot}
                    </h3>
                    <p className="text-sm font-bold text-indigo-600 mb-1">
                      {/* Handle case where currentBid might be null (show start price?) */}
                      Current Bid:{" "}
                      {(auction.currentBid ?? 0).toLocaleString("vi-VN")} VNĐ
                    </p>
                    <p className="text-xs text-red-600 font-medium">
                      {/* Use the extracted CountdownTimer */}
                      <CountdownTimer
                        // Convert endTime string to milliseconds
                        endTimeMillis={new Date(auction.endTime).getTime()}
                        // onEnd prop is likely not needed here, but can be passed if required
                        // onEnd={() => console.log(`Auction ${auction.id} visually ended on list page`)}
                      />
                      <span className="ml-1">left</span>{" "}
                      {/* Add 'left' text if needed */}
                    </p>
                    {/* Optional: Display seller */}
                    {/* <p className="text-xs text-gray-500 mt-1">Seller: {auction.sellerUsernameSnapshot}</p> */}
                  </div>
                </div>
              )
            )}
          </div>

          {/* --- NEW: Pagination Controls --- */}
          {pagination.totalPages > 1 && (
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
                disabled={
                  pagination.page >= pagination.totalPages - 1 || isLoading
                }
                className="px-4 py-2 border rounded bg-white text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Next
              </button>
            </div>
          )}
          {/* --- END Pagination Controls --- */}
        </>
      )}
    </div>
  );
}

export default LiveAuctionsPage;
