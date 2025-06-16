// src/components/CheckoutForm.jsx
import React, { useState } from 'react';
import {
  PaymentElement, // More modern and recommended, handles various payment methods
  useStripe,
  useElements
} from '@stripe/react-stripe-js';

const CheckoutForm = ({ orderId, amount, currency, onSuccess, onError }) => {
  const stripe = useStripe();
  const elements = useElements();

  const [isProcessing, setIsProcessing] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!stripe || !elements) {
      console.log("Stripe.js has not loaded yet.");
      return;
    }

    setIsProcessing(true);
    setErrorMessage(null);

    const { error, paymentIntent } = await stripe.confirmPayment({ // Using PaymentElement
        elements,
        confirmParams: {
          return_url: `${window.location.origin}/orders/${orderId}?payment_confirmed=true`, // Example
        },
        redirect: 'if_required' // Only redirect if required by authentication (e.g. 3DS)
    });

    if (error) {
      console.error("Stripe payment error:", error);
      setErrorMessage(error.message);
      if (onError) onError(error.message);
      setIsProcessing(false);
    } else {
      // Payment submitted. `paymentIntent.status` will be things like 'succeeded', 'processing', 'requires_capture'
      console.log("Stripe PaymentIntent after confirmation attempt:", paymentIntent);
      if (paymentIntent.status === 'succeeded') {
        console.log("Payment Succeeded (client-side)!", paymentIntent);
        if (onSuccess) onSuccess(paymentIntent);
      } else if (paymentIntent.status === 'requires_action' || paymentIntent.status === 'requires_confirmation') {
         console.log("Further action required or confirmation pending. Status:", paymentIntent.status);
         setErrorMessage("Further action is required to complete your payment. Please follow the prompts.");
      } else {
        console.warn("Payment not yet succeeded (client-side). Status:", paymentIntent.status);
        setErrorMessage(`Payment status: ${paymentIntent.status}. Awaiting final confirmation.`);
      }
      setIsProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4 bg-white shadow rounded-lg">
      <PaymentElement /> {/* Modern element that handles multiple payment types */}
      <button
        type="submit"
        disabled={isProcessing || !stripe || !elements}
        className="w-full px-4 py-2.5 bg-green-600 text-white font-semibold rounded hover:bg-green-700 disabled:opacity-50"
      >
        {isProcessing ? "Processing..." : `Pay ${amount.toLocaleString('vi-VN')} ${currency.toUpperCase()}`}
      </button>
      {errorMessage && <div className="text-red-600 text-sm mt-2">{errorMessage}</div>}
    </form>
  );
};

export default CheckoutForm;