import React from 'react';

function UserSellerSection({
  isSeller,
  onPromptBecomeSeller,
  isSellerActivating,
  sellerActivationError,
  sellerActivationSuccess,
}) {
  return (
    <div className="bg-white p-6 rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 border border-gray-200">
      <h3 className="text-xl font-semibold text-slate-800 mb-4 pb-3 border-b border-gray-200">
        Seller Status
      </h3>
      
      {sellerActivationError && (
        <p className="text-red-500 mb-3 text-sm p-3 bg-red-50 rounded-md">{sellerActivationError}</p>
      )}
      {sellerActivationSuccess && (
        <p className="text-green-600 mb-3 text-sm p-3 bg-green-50 rounded-md">
          {sellerActivationSuccess}
        </p>
      )}

      {isSeller ? (
        <div className="flex items-center space-x-2 text-green-700">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-green-500" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
          </svg>
          <p className="font-medium">You are a registered Seller.</p>
        </div>
      ) : (
        <div>
          <p className="mb-4 text-sm text-slate-600">
            Elevate your account to unlock seller privileges, list items, and start your selling journey on AucHub.
          </p>
          <button
            onClick={onPromptBecomeSeller}
            disabled={isSellerActivating}
            className="bg-purple-600 hover:bg-purple-700 text-white font-semibold py-2.5 px-5 rounded-lg disabled:opacity-60 disabled:cursor-not-allowed shadow-md hover:shadow-lg transition-all duration-150 ease-in-out w-full sm:w-auto"
          >
            {isSellerActivating ? 'Processing...' : 'Become a Seller'}
          </button>
        </div>
      )}
    </div>
  );
}

export default UserSellerSection;