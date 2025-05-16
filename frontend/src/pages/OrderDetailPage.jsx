// src/pages/OrderDetailPage.jsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import CountdownTimer from "../components/CountdownTimer";
import ConfirmationModal from "../components/ConfirmationModal";
import { FaCreditCard } from "react-icons/fa";
import { orderStatusMap } from "../constants/orderConstants";
import apiClient from "../api/apiClient"; // Your API client
import { useKeycloak } from "@react-keycloak/web"; // To get current user ID

// Stripe Imports - will be used later
import { loadStripe } from "@stripe/stripe-js";
import { Elements } from "@stripe/react-stripe-js";
import CheckoutForm from "../components/CheckoutForm"; // We will create this component

// Replace with your actual Stripe Test Publishable Key
// IMPORTANT: Store this in an environment variable (e.g., process.env.REACT_APP_STRIPE_PUBLISHABLE_KEY)
const STRIPE_PUBLISHABLE_KEY =
  "pk_test_51RN788QoAglQPjjvhupJXkisXj7R7wt7epc8hYTUbDBTCxumwAownPBKNMM8NfNVza13yVVf6SrfAnmAxoiJtfRw00cIVf2LIl";
const stripePromise = loadStripe(STRIPE_PUBLISHABLE_KEY);

function OrderDetailPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { keycloak, initialized } = useKeycloak();

  const [order, setOrder] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  const [isPaymentConfirmOpen, setIsPaymentConfirmOpen] = useState(false);
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [modalError, setModalError] = useState("");

  // New state for Stripe Payment Intent client secret
  const [clientSecret, setClientSecret] = useState(null);
  const [showCheckoutForm, setShowCheckoutForm] = useState(false);

  const [isSellerCancelOpen, setIsSellerCancelOpen] = useState(false);
  const [sellerCancelReason, setSellerCancelReason] = useState("");

  useEffect(() => {
    if (!orderId || !initialized) {
      setIsLoading(false);
      return;
    }
    if (!keycloak.authenticated) {
      setError("Please log in to view order details.");
      setIsLoading(false);
      return;
    }

    const fetchOrderDetails = async () => {
      setIsLoading(true);
      setError(null);
      try {
        // This endpoint needs to be created in your Orders Service
        const response = await apiClient.get(`/orders/${orderId}`); // Path via gateway
        setOrder(response.data);
      } catch (err) {
        console.error("Failed to fetch order details:", err);
        setError(
          err.response?.data?.message || "Could not load order details."
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrderDetails();
  }, [orderId, initialized, keycloak.authenticated]);

  const handleOpenPaymentAttempt = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Order details or user information is missing.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      // Call your backend Payment Service to create a PaymentIntent
      const response = await apiClient.post("/payments/create-intent", {
        // Path via gateway
        orderId: order.id,
        amount: order.currentAmountDue, // Assuming this is in smallest currency unit (e.g., Dong)
        currency: "vnd", // Your default currency
        userId: keycloak.subject,
        description: `Payment for Order #${order.id.substring(
          0,
          8
        )} - Auction: ${order.items[0]?.title || "item"}`,
      });

      if (response.data && response.data.clientSecret) {
        setClientSecret(response.data.clientSecret);
        setShowCheckoutForm(true); // Show the Stripe CheckoutForm
        setIsPaymentConfirmOpen(false); // Close the confirmation modal
      } else {
        throw new Error("Failed to initialize payment. Missing client secret.");
      }
    } catch (err) {
      console.error("Failed to create payment intent:", err);
      setModalError(
        err.response?.data?.message ||
          err.message ||
          "Could not initiate payment."
      );
      // Keep modal open or show error differently
      setIsPaymentConfirmOpen(true); // Keep modal open to show error or close it
    } finally {
      setIsProcessing(false);
    }
  };

  // This is triggered by the confirmation modal for "Make Payment"
  const handleConfirmPayment = () => {
    setIsPaymentConfirmOpen(false); // Close this modal
    handleOpenPaymentAttempt(); // Proceed to attempt creating payment intent & showing form
  };

  const handleOpenCancelConfirm = () => {
    setModalError("");
    setIsCancelConfirmOpen(true);
  };
  const handleCloseCancelConfirm = () => setIsCancelConfirmOpen(false);

  const handleConfirmCancel = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Cannot cancel: Missing order or user information.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    console.log("Attempting buyer cancellation for order:", orderId);
    try {
      // This endpoint needs to be created in your Orders Service
      // It should verify the user is the currentBidderId for the order.
      await apiClient.post(`/orders/${order.id}/buyer-cancel-attempt`);
      alert(
        "Your request to cancel the payment for this order has been submitted."
      );
      setIsCancelConfirmOpen(false);
      // Re-fetch order details to reflect new status, or navigate away
      // For now, let's simulate a status change and disable payment
      setOrder((prev) => ({ ...prev, status: "CANCELLED" })); // Or a specific "buyer_cancelled" status
    } catch (err) {
      console.error("Failed to cancel order payment attempt:", err);
      setModalError(
        err.response?.data?.message ||
          "Could not cancel this order payment. Please try again."
      );
      // Keep modal open to show error
    } finally {
      setIsProcessing(false);
    }
  };

  // --- Render Logic ---
  if (isLoading && !order)
    return <div className="text-center p-10">Loading Order Details...</div>;
  if (error)
    return <div className="text-center p-10 text-red-600">{error}</div>;
  if (!order)
    return <div className="text-center p-10">Order data not available.</div>;

  const isSeller =
    initialized &&
    keycloak.authenticated &&
    keycloak.subject === order.sellerId;

  const openSellerCancel = () => {
    setModalError("");
    setSellerCancelReason("");
    setIsSellerCancelOpen(true);
  };
  const closeSellerCancel = () => setIsSellerCancelOpen(false);

  const handleSellerCancel = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Missing order or seller info.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      await apiClient.post(
        `/orders/my-sales/${order.id}/cancel`,
        sellerCancelReason // sends plain text body; adjust if DTO required
      );
      closeSellerCancel();
      // re-fetch details so UI updates
      const { data } = await apiClient.get(`/orders/${order.id}`);
      setOrder(data);
    } catch (err) {
      console.error("Seller cancel failed:", err);
      setModalError(
        err.response?.data?.message || "Could not cancel the order."
      );
    } finally {
      setIsProcessing(false);
    }
  };

  const item =
    order.items && order.items.length > 0
      ? order.items[0]
      : {
          title: "N/A",
          imageUrl: "/placeholder.png",
          variation: "",
          quantity: 0,
          price: 0,
        };
  const requiresPayment =
    order.status === "PENDING_PAYMENT" ||
    order.status === "AWAITING_WINNER_PAYMENT" ||
    order.status === "AWAITING_NEXT_BIDDER_PAYMENT";
  const finalPrice = order.currentAmountDue || order.totalPrice || 0; // Use currentAmountDue from OrderDetailDto

  // If showing CheckoutForm, render it instead of the main details for payment
  if (showCheckoutForm && clientSecret) {
    const appearance = { theme: "stripe" };
    const options = { clientSecret, appearance };

    return (
      <div className="max-w-md mx-auto p-4 sm:p-6 lg:p-8">
        <h2 className="text-xl font-semibold mb-4">Complete Your Payment</h2>
        <Elements stripe={stripePromise} options={options}>
          <CheckoutForm
            orderId={order.id}
            amount={finalPrice}
            currency={order.currency || "vnd"}
            onSuccess={() => {
              alert(
                "Payment submitted! Awaiting final confirmation from server."
              );
              setShowCheckoutForm(false);
              // Navigate or refetch order details to update status
              // For now, navigate back to my orders
              navigate("/my-orders");
            }}
            onError={(stripeErrorMsg) => {
              alert(`Payment Error: ${stripeErrorMsg}`); // Show error from Stripe.js
              setShowCheckoutForm(false); // Hide form, allow retry
            }}
          />
        </Elements>
        <button
          onClick={() => setShowCheckoutForm(false)} // Allow user to cancel out of Stripe form
          className="mt-4 w-full px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-100"
        >
          Cancel Payment
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">
        Order Details #{(order.id || "N/A").toString().substring(0, 8)}
      </h1>

      <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
          <div>
            <span className="text-sm text-gray-600">
              Order for auction: {order.auctionId?.substring(0, 8) || "N/A"}
            </span>
            <p className="font-semibold text-gray-800">
              Seller: {order.sellerName || "N/A"}
            </p>
          </div>
          <span
            className={`text-sm font-medium px-3 py-1 rounded-full ${
              requiresPayment
                ? "bg-orange-100 text-orange-800"
                : order.status === "DELIVERING"
                ? "bg-blue-100 text-blue-800"
                : order.status === "COMPLETED"
                ? "bg-green-100 text-green-800"
                : order.status === "CANCELLED" ||
                  order.status === "ORDER_CANCELLED_BY_SELLER" ||
                  order.status === "ORDER_CANCELLED_NO_PAYMENT_FINAL"
                ? "bg-red-100 text-red-800"
                : "bg-gray-100 text-gray-800"
            }`}
          >
            {orderStatusMap[order.status] || order.status}
          </span>
        </div>

        <div className="p-6 flex flex-col sm:flex-row items-start gap-6">
          <img
            src={item.imageUrl}
            alt={item.title}
            className="w-full sm:w-32 h-auto sm:h-32 object-cover rounded border border-gray-200 flex-shrink-0"
            onError={(e) => {
              e.target.onerror = null;
              e.target.src = "/placeholder.png";
            }}
          />
          <div className="flex-grow">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">
              {item.title}
            </h2>
            {item.variation && (
              <p className="text-sm text-gray-600 mb-1">{item.variation}</p>
            )}
            {item.quantity && (
              <p className="text-sm text-gray-600">Quantity: {item.quantity}</p>
            )}
            <p className="text-lg font-bold text-gray-800 mt-3">
              {item.price.toLocaleString("vi-VN")} VNĐ
            </p>
            {(order.buyerPremium || finalPrice - item.price > 0) && ( // Show premium if exists or implied
              <p className="text-sm text-gray-600">
                + Buyer Premium:{" "}
                {(finalPrice - item.price).toLocaleString("vi-VN")} VNĐ
              </p>
            )}
          </div>
        </div>

        {requiresPayment && order.paymentDeadline && (
          <div className="px-6 py-5 border-t border-gray-200 bg-orange-50">
            <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
              <div>
                <p className="text-lg font-semibold text-gray-800">
                  Total Amount Due: {finalPrice.toLocaleString("vi-VN")} VNĐ
                </p>
                <div className="text-sm text-orange-700 font-medium flex items-center gap-1 mt-1">
                  <span>Payment Deadline:</span>
                  <CountdownTimer
                    endTimeMillis={new Date(order.paymentDeadline).getTime()}
                  />
                </div>
              </div>
              {!isSeller && requiresPayment && order.paymentDeadline && (
                <div className="flex items-center gap-3 w-full sm:w-auto mt-3 sm:mt-0">
                  <button
                    onClick={handleOpenCancelConfirm}
                    disabled={isProcessing}
                    className="flex-1 sm:flex-none px-5 py-2.5 border border-gray-400 text-gray-700 rounded hover:bg-gray-100 transition duration-150 ease-in-out text-sm font-medium"
                  >
                    Cancel Order
                  </button>
                  <button
                    onClick={() => setIsPaymentConfirmOpen(true)} // Open confirmation modal first
                    disabled={isProcessing}
                    className="flex-1 sm:flex-none inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-red-600 text-white rounded hover:bg-red-700 transition duration-150 ease-in-out text-sm font-bold"
                  >
                    <FaCreditCard /> Make Payment
                  </button>
                </div>
              )}
              {isSeller && order.status === "AWAITING_WINNER_PAYMENT" && (
                <div className="px-6 py-5 border-t border-gray-200 bg-yellow-50 text-center">
                  <button
                    onClick={openSellerCancel}
                    disabled={isProcessing}
                    className="px-5 py-2 bg-red-600 text-white rounded hover:bg-red-700"
                  >
                    Cancel Sale
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
        {/* Add sections for shipping info, etc. for other statuses */}
        {order.status === "DELIVERING" && (
          <div className="px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-blue-700">Your order is on its way!</p>
            {/* Add tracking info here */}
          </div>
        )}
        {order.status === "COMPLETED" && (
          <div className="px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-green-700">
              Order completed. Thank you!
            </p>
          </div>
        )}
        {order.status === "CANCELLED" && (
          <div className="px-6 py-4 border-t border-gray-200">
            <p className="text-sm text-red-700">
              This order has been cancelled.
            </p>
          </div>
        )}
      </div>

      <ConfirmationModal
        isOpen={isPaymentConfirmOpen}
        onClose={() => {
          if (!isProcessing) setIsPaymentConfirmOpen(false);
        }}
        onConfirm={handleConfirmPayment} // This now calls handleOpenPaymentAttempt
        title="Confirm Payment"
        message={`Proceed to payment for ${finalPrice.toLocaleString(
          "vi-VN"
        )} VNĐ?`}
        confirmText="Yes, Proceed"
        cancelText="Cancel"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isProcessing}
        error={modalError} // Show error from create-intent call here
      />
      <ConfirmationModal
        isOpen={isCancelConfirmOpen}
        onClose={handleCloseCancelConfirm}
        onConfirm={handleConfirmCancel}
        title="Confirm Order Cancellation"
        message="Are you sure you want to cancel your obligation to pay for this order?"
        confirmText="Yes, Cancel My Payment"
        cancelText="No, Keep Order"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={isProcessing}
        error={modalError}
      />
      <ConfirmationModal
        isOpen={isSellerCancelOpen}
        onClose={closeSellerCancel}
        onConfirm={handleSellerCancel}
        title="Cancel Sale"
        confirmText="Yes, Cancel Sale"
        cancelText="No, Keep Sale"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={isProcessing}
        error={modalError}
        message={
          <>
            <p>Please enter a reason for cancelling this order:</p>
            <textarea
              value={sellerCancelReason}
              onChange={(e) => setSellerCancelReason(e.target.value)}
              rows={3}
              className="w-full mt-2 p-2 border rounded"
              placeholder="e.g. Customer didn’t pay on time"
            />
          </>
        }
      />
    </div>
  );
}

export default OrderDetailPage;
