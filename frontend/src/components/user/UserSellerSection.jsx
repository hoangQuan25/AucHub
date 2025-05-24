import React from 'react';

function UserSellerSection({
  isSeller, // Directly pass the boolean isSeller status
  onPromptBecomeSeller,
  isSellerActivating,
  sellerActivationError,
  sellerActivationSuccess,
}) {
  return (
    <div className="mt-6 p-4 border rounded bg-white shadow-sm">
      <h3 className="text-lg font-semibold mb-2 border-b pb-1">
        Seller Status
      </h3>
      {sellerActivationError && (
        <p className="text-red-500 mb-2 text-sm">{sellerActivationError}</p>
      )}
      {sellerActivationSuccess && (
        <p className="text-green-500 mb-2 text-sm">
          {sellerActivationSuccess}
        </p>
      )}

      {isSeller ? (
        <p className="text-green-700 font-medium">
          ✔ You are registered as a Seller.
        </p>
      ) : (
        <div>
          <p className="mb-3 text-sm text-gray-700">
            Upgrade your account to list items and start selling on AucHub.
          </p>
          <button
            onClick={onPromptBecomeSeller}
            disabled={isSellerActivating}
            className="bg-purple-600 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50 transition duration-150 ease-in-out"
          >
            {isSellerActivating ? 'Processing...' : 'Become a Seller'}
          </button>
        </div>
      )}
    </div>
  );
}

export default UserSellerSection;