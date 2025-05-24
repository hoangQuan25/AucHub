// src/components/seller/SellerProfileHeader.jsx
import React from 'react';
import { FaUserCircle, FaStar, FaStarHalfAlt, FaRegStar } from 'react-icons/fa';
import StarRating from '../common/StarRating';

// StarRating component (can be kept here or moved to a shared utils/components file if used elsewhere)
const SellerProfileHeader = ({ sellerProfile, isLoadingProfile, profileError }) => {
  if (isLoadingProfile && !sellerProfile) {
    return <div className="h-48 animate-pulse bg-gray-200 rounded-lg mb-6"></div>;
  }

  if (!isLoadingProfile && profileError && !sellerProfile) {
    return (
      <div className="p-6 bg-red-100 text-red-700 rounded-lg mb-6 text-center">
        Could not load seller header information. Error: {profileError}
      </div>
    );
  }

  if (!sellerProfile) {
    // This case might not be hit if the parent handles the main error, but good as a fallback
    return null; 
  }

  return (
    <header className="mb-6 p-6 bg-white rounded-lg shadow-md flex flex-col md:flex-row items-center md:items-start space-y-4 md:space-y-0 md:space-x-6">
      {sellerProfile.avatarUrl ? (
        <img
          src={sellerProfile.avatarUrl}
          alt={`${sellerProfile.username}'s avatar`}
          className="w-32 h-32 md:w-40 md:h-40 rounded-full object-cover border-4 border-gray-200 shadow-sm"
        />
      ) : (
        <FaUserCircle className="w-32 h-32 md:w-40 md:h-40 text-gray-300" />
      )}
      <div className="text-center md:text-left">
        <h1 className="text-3xl md:text-4xl font-bold text-gray-800">
          {sellerProfile.username}
        </h1>
        <div className="mt-2">
          <StarRating
            rating={sellerProfile.averageRating}
            totalReviews={sellerProfile.reviewCount}
          />
        </div>
        <p className="text-sm text-gray-500 mt-1">
          Joined: {new Date(sellerProfile.memberSince).toLocaleDateString()}
        </p>
      </div>
    </header>
  );
};

export default SellerProfileHeader;