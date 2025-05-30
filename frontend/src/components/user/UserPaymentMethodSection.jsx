import React from 'react';

function UserPaymentMethodSection({
  profileData,
  onAddOrUpdatePaymentMethod,
  isAddingPaymentMethod,
  paymentMethodError,
  paymentMethodSuccess,
  stripePromise,
}) {
  if (!profileData) return null;

  const hasDefaultPaymentMethod = profileData.hasDefaultPaymentMethod || !!profileData.defaultCardLast4;

  return (
    <div className="bg-white p-6 rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 border border-gray-200 h-full">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 pb-3 border-b border-gray-200">
        <h3 className="text-xl font-semibold text-slate-800 mb-2 sm:mb-0">
          Payment Method
        </h3>
        <button
          onClick={onAddOrUpdatePaymentMethod}
          disabled={isAddingPaymentMethod || !stripePromise}
          className="bg-sky-500 hover:bg-sky-600 text-white text-sm font-semibold py-2 px-4 rounded-lg disabled:opacity-60 disabled:cursor-not-allowed shadow-md hover:shadow-lg transition-all duration-150 ease-in-out w-full sm:w-auto"
        >
          {isAddingPaymentMethod
            ? 'Processing...'
            : hasDefaultPaymentMethod
            ? 'Update Method'
            : 'Add Payment Method'}
        </button>
      </div>

      {paymentMethodError && (
        <p className="text-red-500 text-sm mb-3 p-3 bg-red-50 rounded-md">{paymentMethodError}</p>
      )}
      {paymentMethodSuccess && (
        <p className="text-green-600 text-sm mb-3 p-3 bg-green-50 rounded-md">{paymentMethodSuccess}</p>
      )}

      {hasDefaultPaymentMethod && profileData.defaultCardLast4 ? (
        <div className="text-sm text-slate-700 space-y-1">
          <p>
            <span className="font-medium text-slate-600">Default Card:</span>{' '}
            <strong className="text-slate-800">{profileData.defaultCardBrand || 'Card'}</strong> ending in ****{' '}
            <strong className="text-slate-800">{profileData.defaultCardLast4}</strong>
          </p>
          {profileData.defaultCardExpiryMonth &&
            profileData.defaultCardExpiryYear && (
              <p className="text-slate-500 text-xs">
                Expires: {profileData.defaultCardExpiryMonth}/{profileData.defaultCardExpiryYear}
              </p>
            )}
        </div>
      ) : (
        <p className="text-slate-500 text-sm italic">
          No default payment method on file. Add one for faster checkouts.
        </p>
      )}
    </div>
  );
}

export default UserPaymentMethodSection;