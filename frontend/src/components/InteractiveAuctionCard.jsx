import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import { useNotifications } from '../context/NotificationContext'; // Ensure path is correct
import { FaHeart, FaRegHeart } from 'react-icons/fa';
import CountdownTimer from './CountdownTimer'; // Ensure path is correct

function InteractiveAuctionCard({ auction, type }) { // 'type' is "LIVE" or "TIMED"
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const { followedAuctionIds, followAuction, unfollowAuction } = useNotifications();
  const isFollowed = followedAuctionIds?.has(auction.id);

  const handleViewAuction = (auctionId) => {
    const detailPath = type === 'LIVE' ? `/live-auctions/${auctionId}` : `/timed-auctions/${auctionId}`;
    navigate(detailPath);
  };

  const handleFollowToggle = (event) => {
    event.stopPropagation(); // Prevent card click
    if (!keycloak.authenticated) return;

    if (isFollowed) {
      unfollowAuction(auction.id);
    } else {
      followAuction(auction.id, type);
    }
  };

  return (
    <div
      key={auction.id} // Key is usually handled by the parent mapping
      className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer flex flex-col overflow-hidden relative"
      onClick={() => handleViewAuction(auction.id)}
    >
      <div className="w-full h-44 bg-gray-200 relative"> {/* Added relative for heart */}
        <img
          src={auction.productImageUrlSnapshot || "/placeholder.png"}
          alt={auction.productTitleSnapshot}
          className="w-full h-full object-cover"
          loading="lazy"
        />
        {/* Heart Icon Button */}
        {keycloak.authenticated && (
          <button
            onClick={handleFollowToggle}
            className={`absolute top-2 right-2 p-1.5 rounded-full transition-colors duration-150 z-10 ${
              isFollowed
                ? 'bg-red-500 text-white hover:bg-red-600'
                : 'bg-black/30 text-white hover:bg-black/50'
            }`}
            aria-label={isFollowed ? 'Unfollow Auction' : 'Follow Auction'}
            title={isFollowed ? 'Unfollow Auction' : 'Follow Auction'}
          >
            {isFollowed ? <FaHeart size="1em"/> : <FaRegHeart size="1em"/>}
          </button>
        )}
      </div>
      <div className="p-4 flex flex-col flex-1">
        <h3
          className="font-semibold text-sm mb-1 truncate"
          title={auction.productTitleSnapshot}
        >
          {auction.productTitleSnapshot}
        </h3>
        <div className="mt-auto border-t pt-2 text-xs text-gray-600 grid grid-cols-2 gap-2">
          <span>Current Bid:</span>
          <span className="text-right font-medium">
            {(auction.currentBid ?? 0).toLocaleString("vi-VN")} VNĐ
          </span>
          <span>Ends In/At:</span>
          <span className="text-right font-medium">
            {auction.status === "ACTIVE" && auction.endTime ? (
              <CountdownTimer
                endTimeMillis={new Date(auction.endTime).getTime()}
              />
            ) : (
              auction.endTime ? new Date(auction.endTime).toLocaleDateString("en-GB", {
                day: "2-digit",
                month: "short",
                year: "numeric",
              }) : 'N/A' // Fallback for missing endTime
            )}
          </span>
        </div>
      </div>
    </div>
  );
}

export default InteractiveAuctionCard;