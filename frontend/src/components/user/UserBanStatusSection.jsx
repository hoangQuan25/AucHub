// src/components/user/UserBanStatusSection.jsx (NEW FILE)
import React from 'react';
import { FaUserSlash } from 'react-icons/fa'; // Or FaBan, FaExclamationTriangle

const UserBanStatusSection = ({ banEndsAt }) => {
  if (!banEndsAt) {
    return null;
  }

  const formattedDate = new Date(banEndsAt).toLocaleString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div
      className="mb-8 p-4 border-l-4 border-red-500 bg-red-100 text-red-800 rounded-md shadow-lg"
      role="alert"
    >
      <div className="flex items-center">
        <FaUserSlash className="h-8 w-8 mr-4 text-red-600" aria-hidden="true" />
        <div>
          <h3 className="font-bold text-xl">Account Restricted</h3>
          <p className="mt-1 text-sm">
            Your bidding privileges are currently suspended due to repeated payment defaults where you were the winning bidder.
          </p>
          <p className="text-sm mt-2">
            This restriction is active until: <strong className="font-semibold">{formattedDate}</strong>.
          </p>
          <p className="text-xs mt-2 text-red-700">
            Please ensure timely payments for any future successful bids once the restriction is lifted to avoid further, potentially longer, restrictions. If you believe this is an error, please contact support.
          </p>
        </div>
      </div>
    </div>
  );
};

export default UserBanStatusSection;