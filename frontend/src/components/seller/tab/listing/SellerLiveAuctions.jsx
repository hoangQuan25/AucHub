// src/components/seller/tabs/listings/SellerLiveAuctions.jsx
import React from 'react';
import AuctionCard from '../../../AuctionCard'; // Adjust path as needed
import PaginationControls from '../../../PaginationControls'; // Adjust path as needed
import { FaBroadcastTower } from 'react-icons/fa'; // Different icon for live auctions
import InteractiveAuctionCard from '../../../InteractiveAuctionCard';

const SellerLiveAuctions = ({
  liveAuctions,
  isLoadingLiveAuctions,
  errorLiveAuctions,
  livePagination,
  listingPageSize,
  onLiveAuctionPageChange, // Renamed prop for clarity
  onAuctionCardClick,
  isOwner,
}) => {
  return (
    <section className="pt-4 border-t">
      <div className="flex items-center mb-3">
        <FaBroadcastTower className="text-xl text-red-600 mr-2" /> {/* Example Icon */}
        <h4 className="text-lg font-semibold text-gray-700">Live Auctions</h4>
      </div>
      
      {isLoadingLiveAuctions && (
        <div className="text-center py-4">Loading live auctions...</div>
      )}
      {errorLiveAuctions && (
        <div className="text-center py-4 text-red-600 bg-red-50 p-3 rounded">
          {errorLiveAuctions}
        </div>
      )}
      {!isLoadingLiveAuctions && !errorLiveAuctions && liveAuctions.length === 0 && (
        <p className="text-gray-500 py-4 text-center">
          No live auctions match current filters.
        </p>
      )}
      {!isLoadingLiveAuctions && !errorLiveAuctions && liveAuctions.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {liveAuctions.map((auction) => (
              <InteractiveAuctionCard
                key={auction.id}
                auction={auction}
                type="LIVE"
              />
            ))}
          </div>
          {livePagination.totalPages > 1 && (
            <PaginationControls
              pagination={{
                page: livePagination.page,
                totalPages: livePagination.totalPages,
                size: listingPageSize,
              }}
              onPageChange={onLiveAuctionPageChange}
              isLoading={isLoadingLiveAuctions}
            />
          )}
        </>
      )}
    </section>
  );
};

export default SellerLiveAuctions;