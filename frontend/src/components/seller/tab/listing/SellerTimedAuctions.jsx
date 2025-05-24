// src/components/seller/tabs/listings/SellerTimedAuctions.jsx
import React from 'react';
import AuctionCard from '../../../AuctionCard'; // Adjust path as needed
import PaginationControls from '../../../PaginationControls'; // Adjust path as needed
import { FaGavel } from 'react-icons/fa'; // Assuming you still want an icon

const SellerTimedAuctions = ({
  timedAuctions,
  isLoadingTimedAuctions,
  errorTimedAuctions,
  timedPagination,
  listingPageSize,
  onTimedAuctionPageChange, // Renamed prop for clarity
  onAuctionCardClick,
  isOwner,
}) => {
  return (
    <section className="mb-6 pt-4 border-t">
      <div className="flex items-center mb-3"> {/* Optional: Add icon like in MyAuctionsPage */}
        <FaGavel className="text-xl text-blue-600 mr-2 transform -scale-x-100" />
        <h4 className="text-lg font-semibold text-gray-700">Timed Auctions</h4>
      </div>

      {isLoadingTimedAuctions && (
        <div className="text-center py-4">Loading timed auctions...</div>
      )}
      {errorTimedAuctions && (
        <div className="text-center py-4 text-red-600 bg-red-50 p-3 rounded">
          {errorTimedAuctions}
        </div>
      )}
      {!isLoadingTimedAuctions && !errorTimedAuctions && timedAuctions.length === 0 && (
        <p className="text-gray-500 py-4 text-center">
          No timed auctions match current filters.
        </p>
      )}
      {!isLoadingTimedAuctions && !errorTimedAuctions && timedAuctions.length > 0 && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {timedAuctions.map((auction) => (
              <AuctionCard
                key={auction.id}
                auction={auction}
                type="TIMED"
                onClick={onAuctionCardClick}
                isOwner={isOwner} // Pass isOwner if AuctionCard uses it for owner-specific actions
              />
            ))}
          </div>
          {timedPagination.totalPages > 1 && (
            <PaginationControls
              pagination={{
                page: timedPagination.page,
                totalPages: timedPagination.totalPages,
                size: listingPageSize,
              }}
              onPageChange={onTimedAuctionPageChange}
              isLoading={isLoadingTimedAuctions}
            />
          )}
        </>
      )}
    </section>
  );
};

export default SellerTimedAuctions;