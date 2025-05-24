import React from 'react';

function UserPaymentMethodSection({
  profileData,
  onAddOrUpdatePaymentMethod,
  isAddingPaymentMethod,
  paymentMethodError,
  paymentMethodSuccess,
  stripePromise, // For disabling button if Stripe.js not loaded
}) {
  if (!profileData) return null;

  // Assuming backend UserDto provides a boolean 'hasDefaultPaymentMethod'
  // If not, derive it: const hasDefaultPaymentMethod = !!profileData.defaultCardLast4;
  const hasDefaultPaymentMethod = profileData.hasDefaultPaymentMethod || !!profileData.defaultCardLast4;


  return (
    <div className="mb-6 p-4 border rounded bg-white shadow-sm">
      <div className="flex justify-between items-center mb-3 border-b pb-2">
        <h3 className="text-lg font-semibold">Payment Method</h3>
        <button
          onClick={onAddOrUpdatePaymentMethod}
          disabled={isAddingPaymentMethod || !stripePromise}
          className="bg-blue-500 hover:bg-blue-600 text-white text-xs font-semibold py-1 px-3 rounded disabled:opacity-50"
        >
          {isAddingPaymentMethod
            ? 'Processing...'
            : hasDefaultPaymentMethod
            ? 'Update Method'
            : 'Add Payment Method'}
        </button>
      </div>
      {paymentMethodError && (
        <p className="text-red-500 text-sm mb-2">{paymentMethodError}</p>
      )}
      {paymentMethodSuccess && (
        <p className="text-green-500 text-sm mb-2">{paymentMethodSuccess}</p>
      )}

      {hasDefaultPaymentMethod && profileData.defaultCardLast4 ? (
        <p>
          <strong>Default Card:</strong>{' '}
          {profileData.defaultCardBrand || 'N/A'} ending in ****{' '}
          {profileData.defaultCardLast4}
          {profileData.defaultCardExpiryMonth &&
            profileData.defaultCardExpiryYear && (
              <span className="text-gray-600 text-sm">
                {' '}
                (Exp: {profileData.defaultCardExpiryMonth}/
                {profileData.defaultCardExpiryYear})
              </span>
            )}
        </p>
      ) : (
        <p className="text-gray-600 text-sm">
          No default payment method saved. Click "Add Payment Method" to set one
          up for faster checkouts and bidding.
        </p>
      )}
    </div>
  );
}

export default UserPaymentMethodSection;