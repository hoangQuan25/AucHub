import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import CountdownTimer from "../components/CountdownTimer";
import ConfirmationModal from "../components/ConfirmationModal";
import { FaCreditCard } from "react-icons/fa";
import { orderStatusMap } from "../constants/orderConstants";
import apiClient from "../api/apiClient";
import { useKeycloak } from "@react-keycloak/web";

import { loadStripe } from "@stripe/stripe-js";
import { Elements } from "@stripe/react-stripe-js";
import CheckoutForm from "../components/CheckoutForm";

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
  const [isConfirmFulfillmentOpen, setIsConfirmFulfillmentOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [modalError, setModalError] = useState("");

  const [clientSecret, setClientSecret] = useState(null);
  const [showCheckoutForm, setShowCheckoutForm] = useState(false);

  const [isSellerCancelOpen, setIsSellerCancelOpen] = useState(false);
  const [sellerCancelReason, setSellerCancelReason] = useState("");

  // Fetch order details
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
        const response = await apiClient.get(`/orders/${orderId}`);
        setOrder(response.data);
      } catch (err) {
        console.error("Failed to fetch order details:", err);
        setError(err.response?.data?.message || "Could not load order details.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchOrderDetails();
  }, [orderId, initialized, keycloak.authenticated]);

  const refreshOrderDetails = async () => {
    if (!orderId) return;
    try {
      const response = await apiClient.get(`/orders/${orderId}`);
      setOrder(response.data);
    } catch (err) {
      console.error("Failed to re-fetch order details:", err);
    }
  };

  /* ---------- Action handlers ---------- */
  const handleOpenPaymentAttempt = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Order details or user information is missing.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      const response = await apiClient.post("/payments/create-intent", {
        orderId: order.id,
        amount: order.currentAmountDue,
        currency: order.currency || "vnd",
        userId: keycloak.subject,
        description: `Payment for Order #${order.id.substring(0, 8)}`,
      });

      if (response.data && response.data.clientSecret) {
        setClientSecret(response.data.clientSecret);
        setShowCheckoutForm(true);
        setIsPaymentConfirmOpen(false);
      } else {
        throw new Error("Failed to initialize payment. Missing client secret.");
      }
    } catch (err) {
      console.error("Failed to create payment intent:", err);
      setModalError(
        err.response?.data?.message || err.message || "Could not initiate payment."
      );
      setIsPaymentConfirmOpen(true);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleConfirmPayment = () => {
    setIsPaymentConfirmOpen(false);
    handleOpenPaymentAttempt();
  };

  /* Buyer cancel */
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
    try {
      await apiClient.post(`/orders/${order.id}/buyer-cancel-attempt`);
      alert("Your cancellation request has been submitted.");
      setIsCancelConfirmOpen(false);
      setOrder((prev) => ({ ...prev, status: "CANCELLED" }));
    } catch (err) {
      console.error("Failed to cancel order payment attempt:", err);
      setModalError(err.response?.data?.message || "Could not cancel this order. Try again.");
    } finally {
      setIsProcessing(false);
    }
  };

  /* Seller fulfillment confirm */
  const openConfirmFulfillmentModal = () => {
    setModalError("");
    setIsConfirmFulfillmentOpen(true);
  };
  const closeConfirmFulfillmentModal = () => setIsConfirmFulfillmentOpen(false);

  const handleConfirmFulfillment = async () => {
    const isSeller = initialized && keycloak.authenticated && order && keycloak.subject === order.sellerId;
    if (!order || !keycloak.subject || !isSeller) {
      setModalError("Order details are missing or you're not authorized.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      await apiClient.post(`/orders/my-sales/${order.id}/confirm-fulfillment`);
      alert("Order fulfillment confirmed. Await shipment.");
      closeConfirmFulfillmentModal();
      await refreshOrderDetails();
    } catch (err) {
      console.error("Failed to confirm fulfillment:", err);
      setModalError(err.response?.data?.message || "Could not confirm fulfillment.");
    } finally {
      setIsProcessing(false);
    }
  };

  /* Seller cancel */
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
      await apiClient.post(`/orders/my-sales/${order.id}/cancel`, sellerCancelReason);
      closeSellerCancel();
      await refreshOrderDetails();
    } catch (err) {
      console.error("Seller cancel failed:", err);
      setModalError(err.response?.data?.message || "Could not cancel the order.");
    } finally {
      setIsProcessing(false);
    }
  };

  /* ---------- Derived helpers ---------- */
  const isSeller = initialized && keycloak.authenticated && order && keycloak.subject === order.sellerId;
  const isBuyer = initialized && keycloak.authenticated && order && keycloak.subject === order.currentBidderId;

  const finalPrice = order?.currentAmountDue || 0;
  const items = order?.items || [];
  const firstItem = items[0] || {};

  const isAwaitingBuyerPayment = order && (
    order.status === "AWAITING_WINNER_PAYMENT" || order.status === "AWAITING_NEXT_BIDDER_PAYMENT"
  );

  const isAwaitingSellerFulfillmentConfirmation = order && order.status === "AWAITING_FULFILLMENT_CONFIRMATION";

  /* ---------- Conditional early returns ---------- */
  if (isLoading && !order) return <div className="text-center p-10">Loading Order Details...</div>;
  if (error) return <div className="text-center p-10 text-red-600">{error}</div>;
  if (!order) return <div className="text-center p-10">Order data not available.</div>;

  /* ---------- Stripe Checkout ---------- */
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
              alert("Payment submitted! Awaiting confirmation.");
              setShowCheckoutForm(false);
              refreshOrderDetails();
            }}
            onError={(stripeErrorMsg) => {
              alert(`Payment Error: ${stripeErrorMsg}`);
              setShowCheckoutForm(false);
            }}
          />
        </Elements>
        <button
          onClick={() => setShowCheckoutForm(false)}
          className="mt-4 w-full px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-100"
        >
          Cancel Payment Process
        </button>
      </div>
    );
  }

  /* ---------- Main Render ---------- */
  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">
        Order Details #{(order.id || "N/A").toString().substring(0, 8)}
      </h1>

      {/* Meta information */}
      <ul className="mb-6 space-y-1 text-sm text-gray-700">
        <li><strong>Auction ID:</strong> {order.auctionId?.substring(0, 8) || "N/A"}</li>
        <li><strong>Seller:</strong> {order.sellerUsernameSnapshot} ({order.sellerId})</li>
        <li><strong>Status:</strong> {orderStatusMap[order.status] || order.status}</li>
        <li><strong>Auction Type:</strong> {order.auctionType}</li>
        <li><strong>Created At:</strong> {new Date(order.createdAt).toLocaleString()}</li>
        <li><strong>Updated At:</strong> {new Date(order.updatedAt).toLocaleString()}</li>
      </ul>

      {/* Items list */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mb-6">
        {items.map((item) => (
          <div
            key={item.productId}
            className="flex gap-4 items-center bg-white p-4 rounded-lg shadow border border-gray-200"
          >
            <img
              src={item.imageUrl || "/placeholder.png"}
              alt={item.title}
              className="w-24 h-24 object-cover rounded border"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = "/placeholder.png";
              }}
            />
            <div>
              <h3 className="font-semibold text-gray-900 mb-1">{item.title}</h3>
              <p className="text-sm text-gray-700">Qty: {item.quantity}</p>
              <p className="text-sm font-medium text-gray-800">
                Unit Price: {item.price.toLocaleString("vi-VN")} {order.currency || "VNĐ"}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Payment section */}
      {isAwaitingBuyerPayment && order.paymentDeadline && (
        <div className="mb-6 p-4 bg-orange-50 rounded border border-orange-200">
          <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
            <div>
              <p className="text-lg font-semibold text-gray-800">
                Amount Due: {finalPrice.toLocaleString("vi-VN")}
                {" "}{order.currency || "VNĐ"}
              </p>
              <div className="text-sm text-orange-700 font-medium flex items-center gap-1 mt-1">
                <span>Payment Deadline:</span>
                <CountdownTimer endTimeMillis={new Date(order.paymentDeadline).getTime()} />
              </div>
            </div>
            {isBuyer && (
              <div className="flex items-center gap-3 w-full sm:w-auto mt-3 sm:mt-0">
                <button
                  onClick={handleOpenCancelConfirm}
                  disabled={isProcessing}
                  className="flex-1 sm:flex-none px-5 py-2.5 border border-gray-400 text-gray-700 rounded hover:bg-gray-100 text-sm font-medium"
                >
                  Decline Purchase
                </button>
                <button
                  onClick={() => setIsPaymentConfirmOpen(true)}
                  disabled={isProcessing}
                  className="flex-1 sm:flex-none inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-red-600 text-white rounded hover:bg-red-700 text-sm font-bold"
                >
                  <FaCreditCard /> Make Payment
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Seller actions */}
      {isSeller && (
        <div className="px-6 py-4 border-t border-gray-200 bg-gray-100 mb-6 rounded">
          <h3 className="text-md font-semibold text-gray-700 mb-3">Seller Actions:</h3>
          <div className="flex flex-wrap gap-3">
            {order.status === "AWAITING_WINNER_PAYMENT" && (
              <button
                onClick={openSellerCancel}
                disabled={isProcessing}
                className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 text-sm font-medium"
              >
                Cancel Sale (Before Payment)
              </button>
            )}

            {isAwaitingSellerFulfillmentConfirmation && (
              <>
                <button
                  onClick={openConfirmFulfillmentModal}
                  disabled={isProcessing}
                  className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 text-sm font-medium"
                >
                  Confirm for Shipping
                </button>
                <button
                  onClick={openSellerCancel}
                  disabled={isProcessing}
                  className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 text-sm font-medium"
                >
                  Cancel Order & Issue Refund
                </button>
              </>
            )}

            {order.status === "AWAITING_SELLER_DECISION" && (
              <p className="text-sm text-yellow-800 bg-yellow-100 p-3 rounded w-full">
                This order requires your decision. Manage this from your "My Sales" page.
              </p>
            )}

            {order.status === "AWAITING_SHIPMENT" && (
              <button
                disabled
                className="px-4 py-2 bg-blue-500 text-white rounded text-sm font-medium disabled:opacity-50"
              >
                Mark as Shipped (Pending Delivery Service)
              </button>
            )}
          </div>
        </div>
      )}

      {/* Alternate bidders */}
      {(order.eligibleSecondBidderId || order.eligibleThirdBidderId) && (
        <div className="mb-6 p-4 bg-gray-50 rounded border border-gray-200">
          <h4 className="font-semibold mb-2">Alternate Bidders</h4>
          <ul className="text-sm space-y-1">
            {order.eligibleSecondBidderId && (
              <li>
                2nd Bidder: <strong>{order.eligibleSecondBidderId}</strong> — {order.eligibleSecondBidAmount?.toLocaleString("vi-VN")} {order.currency}
              </li>
            )}
            {order.eligibleThirdBidderId && (
              <li>
                3rd Bidder: <strong>{order.eligibleThirdBidderId}</strong> — {order.eligibleThirdBidAmount?.toLocaleString("vi-VN")} {order.currency}
              </li>
            )}
          </ul>
        </div>
      )}

      {/* Informational banners */}
      {order.status === "PAYMENT_SUCCESSFUL" && !isAwaitingSellerFulfillmentConfirmation && (
        <div className="mb-6 px-6 py-4 border-t border-gray-200">
          <p className="text-sm text-lime-700 font-semibold">
            Payment successful! Awaiting seller fulfillment confirmation.
          </p>
        </div>
      )}

      {order.status?.includes("CANCELLED") && (
        <div className="mb-6 px-6 py-4 border-t border-gray-200">
          <p className="text-sm text-red-700 font-semibold">
            This order has been cancelled. (Status: {orderStatusMap[order.status] || order.status})
          </p>
        </div>
      )}

      {/* ---------- Modals ---------- */}
      <ConfirmationModal
        isOpen={isPaymentConfirmOpen}
        onClose={() => {
          if (!isProcessing) setIsPaymentConfirmOpen(false);
        }}
        onConfirm={handleConfirmPayment}
        title="Confirm Payment"
        message={`Proceed to payment for ${finalPrice.toLocaleString("vi-VN")} ${order.currency || "VNĐ"}?`}
        confirmText="Yes, Proceed"
        cancelText="Cancel"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isProcessing}
        error={modalError}
      />
      <ConfirmationModal
        isOpen={isCancelConfirmOpen}
        onClose={handleCloseCancelConfirm}
        onConfirm={handleConfirmCancel}
        title="Confirm Decline Purchase"
        message="Are you sure you want to decline this purchase? This may pass the offer to the next bidder or require seller action."
        confirmText="Yes, Decline"
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
      <ConfirmationModal
        isOpen={isConfirmFulfillmentOpen}
        onClose={closeConfirmFulfillmentModal}
        onConfirm={handleConfirmFulfillment}
        title="Confirm Order Fulfillment"
        message="Are you sure you are ready to prepare this item for shipping? This action cannot be undone and will move the order to the next stage."
        confirmText="Yes, Confirm Fulfillment"
        cancelText="No, Not Yet"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isProcessing}
        error={modalError}
      />
    </div>
  );
}

export default OrderDetailPage;
