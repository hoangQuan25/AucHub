// src/components/CheckoutForm.jsx
import React, { useState } from 'react';
import {
  PaymentElement, // More modern and recommended, handles various payment methods
  // CardElement, // Simpler for just card input
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
      // Stripe.js has not yet loaded.
      // Make sure to disable form submission until Stripe.js has loaded.
      console.log("Stripe.js has not loaded yet.");
      return;
    }

    setIsProcessing(true);
    setErrorMessage(null);

    // This clientSecret was obtained when your backend PaymentService created the PaymentIntent
    // It's passed as an option to the <Elements> provider in OrderDetailPage.jsx
    // The confirmPayment method uses the clientSecret from the options of the Elements provider.
    const { error, paymentIntent } = await stripe.confirmPayment({ // Using PaymentElement
        elements,
        confirmParams: {
          // Make sure to change this to your payment completion page
          // This is where Stripe will redirect the user after payment (e.g., 3D Secure)
          // For SPA, this often points back to a page in your app that checks PI status.
          return_url: `${window.location.origin}/orders/${orderId}?payment_confirmed=true`, // Example
        },
        redirect: 'if_required' // Only redirect if required by authentication (e.g. 3DS)
    });
    
    // If using CardElement:
    // const { error, paymentIntent } = await stripe.confirmCardPayment(clientSecret, { // clientSecret needs to be passed as prop
    //   payment_method: {
    //     card: elements.getElement(CardElement),
    //     // billing_details: { name: 'Jenny Rosen' }, // Optional
    //   },
    // });


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
         // Stripe.js might handle redirects automatically if 'redirect: if_required' is used with confirmPayment
         // Or you might need to handle specific actions here based on paymentIntent.next_action
         setErrorMessage("Further action is required to complete your payment. Please follow the prompts.");
      } else {
        console.warn("Payment not yet succeeded (client-side). Status:", paymentIntent.status);
        setErrorMessage(`Payment status: ${paymentIntent.status}. Awaiting final confirmation.`);
        // Backend webhook is the source of truth.
        // You could poll your backend for order status or rely on WebSocket updates.
      }
      setIsProcessing(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 p-4 bg-white shadow rounded-lg">
      {/* <CardElement options={{style: {base: {fontSize: '16px'}}}} /> */}
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