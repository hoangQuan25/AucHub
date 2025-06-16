import React, { useState } from 'react';
import { useStripe, useElements, CardElement, Elements } from '@stripe/react-stripe-js';

const StripeSetupFormModal = ({
  isOpen,
  onClose,
  clientSecret,
  onSuccess,
  onError: onStripeError
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!stripe || !elements || !clientSecret) {
      setError("Stripe is not ready or client secret is missing. Please try again.");
      return;
    }

    const cardElement = elements.getElement(CardElement);
    if (!cardElement) {
      setError("Card details are not available. Please ensure the card element is loaded.");
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const { error: stripeJsError, setupIntent } = await stripe.confirmCardSetup(
        clientSecret,
        {
          payment_method: { card: cardElement }
        }
      );

      if (stripeJsError) {
        console.error("Stripe.js confirmCardSetup error:", stripeJsError);
        const msg = stripeJsError.message || "An error occurred with Stripe.";
        setError(msg);
        onStripeError && onStripeError(msg);
        setIsProcessing(false);
        return;
      }

      if (setupIntent && setupIntent.status === 'succeeded') {
        console.log("Stripe SetupIntent Succeeded:", setupIntent);
        onSuccess && onSuccess(setupIntent.payment_method);
        onClose();
      } else {
        console.warn("Stripe SetupIntent status not succeeded:", setupIntent);
        const msg = setupIntent?.last_setup_error?.message || "Payment method setup failed. Please try again.";
        setError(msg);
        onStripeError && onStripeError(msg);
      }
    } catch (e) {
      console.error("Exception during card setup:", e);
      const msg = "An unexpected error occurred. Please try again.";
      setError(msg);
      onStripeError && onStripeError(msg);
    } finally {
      setIsProcessing(false);
    }
  };

  const cardElementOptions = {
    style: {
      base: {
        color: "#32325d",
        fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
        fontSmoothing: "antialiased",
        fontSize: "16px",
        "::placeholder": { color: "#aab7c4" }
      },
      invalid: { color: "#fa755a", iconColor: "#fa755a" }
    }
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex justify-center items-center">
      <div className="bg-white p-6 rounded-lg shadow-xl w-full max-w-md">
        <h2 className="text-xl font-semibold mb-4">Add / Update Payment Method</h2>
        <form onSubmit={handleSubmit}>
          <div className="mb-4 p-3 border rounded">
            <CardElement options={cardElementOptions} />
          </div>
          {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
          <div className="flex justify-end gap-3 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={isProcessing}
              className="px-4 py-2 text-gray-700 border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!stripe || !elements || isProcessing || !clientSecret}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
            >
              {isProcessing ? "Processing..." : "Save Payment Method"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

/**
 * Wrapper that provides the Stripe Elements context and only renders when open.
 */
const StripeWrappedSetupFormModal = ({
  isOpen,
  clientSecret,
  stripePromise,
  onClose,
  onSuccess,
  onError
}) => {
  if (!isOpen || !clientSecret) return null;

  const appearance = { theme: 'stripe' };
  const options = { clientSecret, appearance };

  return (
    <Elements stripe={stripePromise} options={options}>
      <StripeSetupFormModal
        isOpen={isOpen}
        onClose={onClose}
        clientSecret={clientSecret}
        onSuccess={onSuccess}
        onError={onError}
      />
    </Elements>
  );
};

export default StripeWrappedSetupFormModal;