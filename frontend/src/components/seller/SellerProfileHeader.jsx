import React from 'react';
import { FaUserCircle } from 'react-icons/fa';
import StarRating from '../common/StarRating';

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
    return null;
  }

  return (
    <header className="mb-8 flex flex-col md:flex-row items-center md:items-start gap-6">
      {sellerProfile.avatarUrl ? (
        <img
          src={sellerProfile.avatarUrl}
          alt={`${sellerProfile.username}'s avatar`}
          className="w-32 h-32 md:w-40 md:h-40 rounded-full object-cover border-4 border-slate-300 shadow-lg"
        />
      ) : (
        <FaUserCircle className="w-32 h-32 md:w-40 md:h-40 text-slate-400 border-4 border-slate-300 rounded-full shadow-lg" />
      )}
      <div className="flex-1">
        <h1 className="text-3xl md:text-4xl font-bold text-indigo-800 tracking-tight flex items-center gap-2">
          {sellerProfile.username}
          {sellerProfile.isVerified && (
            <span className="ml-2 px-2 py-0.5 bg-indigo-100 text-indigo-700 text-xs rounded-full font-semibold">
              Verified
            </span>
          )}
        </h1>
        <div className="mt-2">
          <StarRating
            rating={sellerProfile.averageRating}
            totalReviews={sellerProfile.reviewCount}
          />
        </div>
        <p className="text-sm text-slate-500 mt-2">
          Joined: <span className="font-medium text-slate-700">{new Date(sellerProfile.memberSince).toLocaleDateString()}</span>
        </p>
      </div>
    </header>
  );
};

export default SellerProfileHeader;