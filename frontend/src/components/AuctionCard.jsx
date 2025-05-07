import React from "react";
import CountdownTimer from "./CountdownTimer"; // Assuming you have a CountdownTimer component

function AuctionCard({ auction, type, onClick }) {
  return (
    <div
      key={auction.id}
      className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer flex flex-col overflow-hidden"
      onClick={() => onClick(auction.id, type)} // Pass type back
    >
      <div className="w-full h-44 bg-gray-200">
        <img
          src={auction.productImageUrlSnapshot || "/placeholder.png"}
          alt={auction.productTitleSnapshot}
          className="w-full h-full object-cover"
          loading="lazy"
        />
      </div>
      <div className="p-4 flex flex-col flex-1">
        <h3
          className="font-semibold text-sm mb-1 truncate"
          title={auction.productTitleSnapshot}
        >
          {auction.productTitleSnapshot}
        </h3>
        <p className="text-xs text-gray-500 mb-2">Status: {auction.status}</p>
        <div className="mt-auto border-t pt-2 text-xs text-gray-600 grid grid-cols-2 gap-2">
          <span>Current Bid:</span>
          <span className="text-right font-medium">
            {(auction.currentBid ?? 0).toLocaleString("vi-VN")} VNĐ
          </span>
          <span>Ends In/At:</span>
          <span className="text-right font-medium">
            {" "}
            {/* Make text bold/medium */}
            {auction.status === "ACTIVE" ? (
              <CountdownTimer
                endTimeMillis={new Date(auction.endTime).getTime()}
              />
            ) : (
              // Display end time more nicely for non-active
              new Date(auction.endTime).toLocaleDateString("en-GB", {
                day: "2-digit",
                month: "short",
                year: "numeric",
              })
            )}
          </span>
        </div>
      </div>
    </div>
  );
}

export default AuctionCard;
