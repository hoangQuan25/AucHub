// src/components/seller/tabs/AboutSellerTab.jsx
import React from 'react';

const AboutSellerTab = ({ sellerProfile }) => {
  if (!sellerProfile) {
    return <div className="p-6 text-gray-500">Seller information is not available.</div>;
  }

  return (
    <div className="p-6 bg-white rounded-lg shadow">
      <h2 className="text-2xl font-semibold mb-4 text-gray-800">
        About {sellerProfile.username}
      </h2>
      {sellerProfile.sellerDescription ? (
        <p className="text-gray-700 whitespace-pre-line">
          {sellerProfile.sellerDescription}
        </p>
      ) : (
        <p className="text-gray-500">
          This seller hasn't added a description yet.
        </p>
      )}
      <p className="mt-4 text-sm text-gray-500">
        Member since:{" "}
        {new Date(sellerProfile.memberSince).toLocaleDateString()}
      </p>
    </div>
  );
};

export default AboutSellerTab;