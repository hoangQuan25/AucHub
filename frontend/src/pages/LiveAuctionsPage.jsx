// src/pages/LiveAuctionsPage.jsx (New File - Mockup)
import React, { useState } from "react";
import { useNavigate } from "react-router-dom"; // To navigate to detail page later

// Mock data for active live auctions
const mockLiveAuctions = [
  {
    id: "live101",
    productId: "prod1",
    title: "Vintage Pokemon Card Lot",
    currentBid: 150000,
    timeLeftMs: 900000,
    imageUrl: "/placeholder.png",
    seller: "SellerA",
  },
  {
    id: "live102",
    productId: "prod2",
    title: "Used IKEA Bookshelf - Good Condition",
    currentBid: 350000,
    timeLeftMs: 1800000,
    imageUrl: "/placeholder.png",
    seller: "SellerB",
  },
  {
    id: "live103",
    productId: "prod3",
    title: "Handmade Ceramic Vase",
    currentBid: 75000,
    timeLeftMs: 300000,
    imageUrl: "/placeholder.png",
    seller: "SellerC",
  },
  {
    id: "live104",
    productId: "prod4",
    title: "Retro Gaming T-Shirt (Large)",
    currentBid: 120000,
    timeLeftMs: 2700000,
    imageUrl: "/placeholder.png",
    seller: "SellerD",
  },
];

// Simple Countdown Timer Display Component (Mock - doesn't actually count down yet)
const MockCountdown = ({ ms }) => {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  return (
    <span>
      {minutes}:{seconds < 10 ? "0" : ""}
      {seconds} left
    </span>
  );
};

function LiveAuctionsPage() {
  const navigate = useNavigate();

  const handleViewAuction = (auctionId) => {
    console.log(`Maps to /live-auctions/${auctionId}`);
    navigate(`/live-auctions/${auctionId}`);
  };

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Live Auctions Happening Now</h1>

      {/* Grid for auction cards */}
      {mockLiveAuctions.length === 0 ? (
        <p>No live auctions currently active.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {mockLiveAuctions.map((auction) => (
            <div
              key={auction.id}
              className="border rounded-lg bg-white shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
              onClick={() => handleViewAuction(auction.id)}
            >
              {/* Image */}
              <div className="w-full h-48 bg-gray-200">
                <img
                  src={auction.imageUrl}
                  alt={auction.title}
                  className="w-full h-full object-cover"
                  loading="lazy"
                />
              </div>
              {/* Details */}
              <div className="p-4">
                <h3
                  className="font-semibold text-md mb-1 truncate"
                  title={auction.title}
                >
                  {auction.title}
                </h3>
                <p className="text-sm font-bold text-indigo-600 mb-1">
                  Current Bid: {auction.currentBid.toLocaleString("vi-VN")} VNĐ
                </p>
                <p className="text-xs text-red-600 font-medium">
                  <MockCountdown ms={auction.timeLeftMs} />
                </p>
                {/* <p className="text-xs text-gray-500 mt-1">Seller: {auction.seller}</p> */}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default LiveAuctionsPage;
