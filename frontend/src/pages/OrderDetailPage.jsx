// src/pages/OrderDetailPage.jsx
import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ConfirmationModal from "../components/ConfirmationModal";
import apiClient from "../api/apiClient";
import { useKeycloak } from "@react-keycloak/web";
import { loadStripe } from "@stripe/stripe-js";
import { Elements } from "@stripe/react-stripe-js";
import CheckoutForm from "../components/CheckoutForm";
import { FaCreditCard, FaHistory } from "react-icons/fa";

// Import the new refactored components
import OrderHeader from "../components/order/OrderHeader";
import OrderItems from "../components/order/OrderItems";
import OrderPaymentDetails from "../components/order/OrderPaymentDetails";
import OrderSellerActions from "../components/order/OrderSellerActions";
import OrderAlternateBidders from "../components/order/OrderAlternateBidders";
import OrderInfoBanners from "../components/order/OrderInfoBanners";
import BuyerShippingInfo from "../components/order/BuyerShippingInfo";
import OrderDeliveryProgress from "../components/order/OrderDeliveryProgress";
import BuyerDeliveryActions from "../components/order/BuyerDeliverActions";
import MarkAsShippedFormModal from "../components/delivery/MarkAsShippedFormModal";
import MarkAsDeliveredFormModal from "../components/delivery/MarkAsDeliveredFormModal";
import SubmitReviewModal from "../components/SubmitReviewModal";
import SellerDecisionModal from "../components/SellerDecisionModal";
import StartAuctionModal from "../components/StartAuctionModal";

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
  const [userProfile, setUserProfile] = useState(null);
  const [deliveryDetails, setDeliveryDetails] = useState(null); // For storing fetched delivery info
  const [isLoadingDelivery, setIsLoadingDelivery] = useState(false);
  // Profile loading state is managed internally by fetchUserProfile now if not critical for main page load
  // const [profileLoading, setProfileLoading] = useState(true);

  // Modal States
  const [isPaymentConfirmOpen, setIsPaymentConfirmOpen] = useState(false);
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isConfirmFulfillmentOpen, setIsConfirmFulfillmentOpen] =
    useState(false);
  const [isSellerCancelOpen, setIsSellerCancelOpen] = useState(false);

  const [isProcessing, setIsProcessing] = useState(false); // For general actions
  const [paymentProcessing, setPaymentProcessing] = useState(false); // Specific for payment initiation
  const [modalError, setModalError] = useState("");

  const [isStartAuctionModalOpen, setIsStartAuctionModalOpen] = useState(false);
  const [productToAuction, setProductToAuction] = useState(null);

  const [clientSecret, setClientSecret] = useState(null);
  const [showCheckoutForm, setShowCheckoutForm] = useState(false);
  const [checkoutFormOptions, setCheckoutFormOptions] = useState({});
  const [sellerCancelReason, setSellerCancelReason] = useState("");

  const [isSellerDecisionModalOpen, setIsSellerDecisionModalOpen] =
    useState(false);

  const [isMarkAsShippedModalOpen, setIsMarkAsShippedModalOpen] =
    useState(false);
  const [markAsShippedError, setMarkAsShippedError] = useState("");
  const [isMarkingAsShipped, setIsMarkingAsShipped] = useState(false);

  const [isMarkAsDeliveredModalOpen, setIsMarkAsDeliveredModalOpen] =
    useState(false);
  const [isMarkingAsDelivered, setIsMarkingAsDelivered] = useState(false);
  const [markAsDeliveredError, setMarkAsDeliveredError] = useState("");
  const [deliveredNotes, setDeliveredNotes] = useState("");

  const [isConfirmingReceipt, setIsConfirmingReceipt] = useState(false);
  const [isRequestingReturn, setIsRequestingReturn] = useState(false); // If you have a separate loading for this
  const [buyerActionError, setBuyerActionError] = useState("");

  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [reviewSubmissionMessage, setReviewSubmissionMessage] = useState({
    type: "",
    text: "",
  });

  const fetchUserProfile = useCallback(async () => {
    if (initialized && keycloak.authenticated) {
      // setProfileLoading(true);
      try {
        await keycloak.updateToken(5);
        const response = await apiClient.get("/users/me");
        setUserProfile(response.data);
      } catch (err) {
        console.error("Failed to fetch user profile:", err);
      } finally {
        // setProfileLoading(false);
      }
    }
  }, [initialized, keycloak]);

  const fetchOrderAndDeliveryDetails = useCallback(async () => {
    if (!orderId || !initialized) return;
    if (!keycloak.authenticated) {
      setError("Please log in to view details.");
      setIsLoading(false);
      return;
    }
    setIsLoading(true); // For order
    setIsLoadingDelivery(true); // For delivery
    setError(null);
    try {
      await keycloak.updateToken(5);
      // Fetch Order
      const orderResponse = await apiClient.get(`/orders/${orderId}`);
      setOrder(orderResponse.data);
      console.log("Fetched order:", orderResponse.data);

      // If order status suggests delivery might exist, fetch delivery details
      const currentOrder = orderResponse.data;
      if (
        currentOrder &&
        (currentOrder.status === "AWAITING_SHIPMENT" ||
          currentOrder.status === "ORDER_SHIPPED" || // Assuming these statuses exist
          currentOrder.status === "ORDER_DELIVERED")
        // Add other relevant statuses from DeliveryStatus enum once mapped
      ) {
        try {
          // ASSUMPTION: Endpoint to get delivery by orderId
          const deliveryResponse = await apiClient.get(
            `/deliveries/by-order/${orderId}`
          );
          setDeliveryDetails(deliveryResponse.data);
        } catch (deliveryErr) {
          // It's okay if delivery details are not found initially (e.g., PENDING_PREPARATION in DeliveriesService)
          if (deliveryErr.response?.status === 404) {
            console.info(
              `No delivery record found yet for order ${orderId}. This might be expected.`
            );
            setDeliveryDetails(null); // Ensure it's null if not found
          } else {
            console.error("Failed to fetch delivery details:", deliveryErr);
            // Optionally set an error specific to delivery fetching
          }
        }
      }
    } catch (err) {
      console.error("Failed to fetch order details:", err);
      setError(err.response?.data?.message || "Could not load order details.");
    } finally {
      setIsLoading(false);
      setIsLoadingDelivery(false);
    }
  }, [orderId, initialized, keycloak]);

  useEffect(() => {
    fetchOrderAndDeliveryDetails();
    fetchUserProfile();
  }, [fetchOrderAndDeliveryDetails, fetchUserProfile]);

  const refreshAllDetails = useCallback(async () => {
    // This function will now re-fetch both order and delivery details
    await fetchOrderAndDeliveryDetails();
  }, [fetchOrderAndDeliveryDetails]);

  const handleOpenPaymentAttempt = async (useSavedCard = false) => {
    if (!order || !keycloak.subject) {
      setModalError("Order details or user information is missing.");
      return;
    }
    setPaymentProcessing(true);
    setModalError("");
    setShowCheckoutForm(false);
    setClientSecret(null);

    const firstItemTitle = order.items?.[0]?.title || "item";
    let paymentIntentRequestData = {
      orderId: order.id,
      amount: order.currentAmountDue,
      currency: order.currency || "vnd",
      userId: keycloak.subject,
      description: `Payment for Order #${order.id.substring(
        0,
        8
      )} - Product: ${firstItemTitle}`,
    };
    const paymentReturnUrl = `${window.location.origin}/orders/${order.id}?payment_intent_confirmed=true`;

    if (
      useSavedCard &&
      userProfile?.hasDefaultPaymentMethod &&
      userProfile?.stripeCustomerId &&
      userProfile?.stripeDefaultPaymentMethodId
    ) {
      paymentIntentRequestData = {
        ...paymentIntentRequestData,
        stripeCustomerId: userProfile.stripeCustomerId,
        stripePaymentMethodId: userProfile.stripeDefaultPaymentMethodId,
        confirmImmediately: true,
        offSession: false, // User is present
        returnUrl: paymentReturnUrl,
      };
    } else {
      paymentIntentRequestData = {
        ...paymentIntentRequestData,
        confirmImmediately: false,
        returnUrl: paymentReturnUrl,
      };
    }

    try {
      const response = await apiClient.post(
        "/payments/create-intent",
        paymentIntentRequestData
      );
      const intent = response.data;
      if (intent?.clientSecret) {
        setClientSecret(intent.clientSecret);
        setCheckoutFormOptions({
          clientSecret: intent.clientSecret,
          appearance: { theme: "stripe" },
        });
        if (intent.status === "succeeded") {
          alert("Payment with saved card succeeded!");
          await refreshAllDetails();
        } else if (
          intent.status === "requires_action" ||
          (useSavedCard && intent.status !== "succeeded")
        ) {
          setShowCheckoutForm(true);
        } else if (
          intent.status === "requires_payment_method" &&
          useSavedCard
        ) {
          setModalError(
            "Your saved card could not be charged. Please enter a new payment method."
          );
          setShowCheckoutForm(true);
        } else if (!useSavedCard) {
          setShowCheckoutForm(true);
        } else {
          setModalError(
            `Payment status: ${intent.status}. Please review or try again.`
          );
          setShowCheckoutForm(true); // Fallback
        }
      } else {
        throw new Error(
          "Failed to initialize payment. Missing client secret or invalid response."
        );
      }
    } catch (err) {
      console.error("Failed to create payment intent:", err);
      setModalError(
        err.response?.data?.message ||
          err.message ||
          "Could not initiate payment."
      );
      if (!useSavedCard) setIsPaymentConfirmOpen(true); // Re-open choice modal if new card init fails
    } finally {
      setPaymentProcessing(false);
      // Do not close setIsPaymentConfirmOpen(false) here if it wasn't a saved card attempt
    }
  };

  const handleConfirmPaymentChoice = (useSaved) => {
    setIsPaymentConfirmOpen(false); // Close the choice modal
    handleOpenPaymentAttempt(useSaved); // Then attempt payment
  };

  const handleOpenCancelConfirm = () => {
    setModalError("");
    setIsCancelConfirmOpen(true);
  };
  const handleCloseCancelConfirm = () => setIsCancelConfirmOpen(false);
  const handleConfirmCancel = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Missing order/user info.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      await apiClient.post(`/orders/${order.id}/buyer-cancel-attempt`);
      alert("Your cancellation request has been submitted.");
      setIsCancelConfirmOpen(false);
      await refreshAllDetails();
    } catch (err) {
      console.error("Failed to cancel order:", err);
      setModalError(
        err.response?.data?.message || "Could not cancel. Try again."
      );
    } finally {
      setIsProcessing(false);
    }
  };

  const openConfirmFulfillmentModal = () => {
    setModalError("");
    setIsConfirmFulfillmentOpen(true);
  };
  const closeConfirmFulfillmentModal = () => setIsConfirmFulfillmentOpen(false);
  const handleConfirmFulfillment = async () => {
    if (!order || !keycloak.subject || keycloak.subject !== order.sellerId) {
      setModalError("Not authorized or missing data.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      await apiClient.post(`/orders/my-sales/${order.id}/confirm-fulfillment`);
      alert("Order fulfillment confirmed.");
      closeConfirmFulfillmentModal();
      await refreshAllDetails();
    } catch (err) {
      console.error("Failed to confirm fulfillment:", err);
      setModalError(err.response?.data?.message || "Could not confirm.");
    } finally {
      setIsProcessing(false);
    }
  };

  const openSellerCancelModal = () => {
    setModalError("");
    setSellerCancelReason("");
    setIsSellerCancelOpen(true);
  };
  const closeSellerCancelModal = () => setIsSellerCancelOpen(false);
  const handleSellerCancel = async () => {
    if (!order || !keycloak.subject) {
      setModalError("Missing order/seller info.");
      return;
    }
    setIsProcessing(true);
    setModalError("");
    try {
      await apiClient.post(
        `/orders/my-sales/${order.id}/cancel`,
        sellerCancelReason,
        { headers: { "Content-Type": "text/plain" } }
      ); // Send reason as plain text
      closeSellerCancelModal();
      await refreshAllDetails();
    } catch (err) {
      console.error("Seller cancel failed:", err);
      setModalError(err.response?.data?.message || "Could not cancel order.");
    } finally {
      setIsProcessing(false);
    }
  };

  const handleOpenMarkAsShippedModal = () => {
    if (!deliveryDetails && order?.status !== "AWAITING_SHIPMENT") {
      // This check might be too strict if delivery record in PENDING_PREPARATION is not yet fetched
      // Or if the deliveryId is on the order object directly
      alert("Delivery details not available or order not ready for shipment.");
      return;
    }
    setMarkAsShippedError("");
    setIsMarkAsShippedModalOpen(true);
  };

  const handleOpenSellerDecisionModal = () => {
    if (!order || !isSeller || order.status !== "AWAITING_SELLER_DECISION") {
      console.warn("Cannot open seller decision modal: Conditions not met.");
      return;
    }
    setModalError("");
    setIsSellerDecisionModalOpen(true);
  };

  // Inside OrderDetailPage component:

  // Handler to open seller decision modal (you might already have a version of this)
  const handleOpenSellerDecisionModalOnDetail = () => {
    if (!order || !isSeller || order.status !== "AWAITING_SELLER_DECISION") {
      console.warn("Conditions not met to open seller decision modal.");
      return;
    }
    setModalError(""); // Assuming you have a shared modalError state
    setIsSellerDecisionModalOpen(true);
  };

  // Handler to close seller decision modal
  const handleCloseSellerDecisionModalOnDetail = async (
    decisionWasMade = false,
    actionTaken = null
  ) => {
    setIsSellerDecisionModalOpen(false);
    // If a decision was made AND it was NOT 'REOPEN_AUCTION' (which has its own refresh later)
    if (decisionWasMade && actionTaken !== "REOPEN_AUCTION") {
      await refreshAllDetails(); // refreshAllDetails should fetch order & delivery
    }
  };

  // Handler called by SellerDecisionModal when "Reopen Auction" is chosen
  const handleInitiateReopenAuctionOnDetail = (orderFromModal) => {
    // orderFromModal is the 'order' object currently displayed on this page
    if (
      !orderFromModal ||
      !orderFromModal.items ||
      orderFromModal.items.length === 0
    ) {
      alert("Error: Order item details are missing. Cannot reopen auction.");
      setIsSellerDecisionModalOpen(false); // Ensure decision modal is closed
      return;
    }

    const firstItem = orderFromModal.items[0];
    const productIdForReopen = firstItem.productId;
    const productTitleForReopen =
      firstItem.title || "Product (Details Missing)";
    const productImageUrlForReopen = firstItem.imageUrl;

    if (!productIdForReopen) {
      alert(
        "Error: Product ID is missing from order details. Cannot reopen auction."
      );
      setIsSellerDecisionModalOpen(false); // Ensure decision modal is closed
      return;
    }

    const productDataForAuction = {
      id: productIdForReopen,
      title: productTitleForReopen,
      imageUrls: productImageUrlForReopen ? [productImageUrlForReopen] : [],
      originalOrderId: orderFromModal.id, // ID of the current order on OrderDetailPage
      // Potentially carry over other relevant details from order or product snapshot
      // reservePrice: orderFromModal.reservePriceSnapshot,
    };

    setProductToAuction(productDataForAuction);
    setIsSellerDecisionModalOpen(false); // Close the decision modal
    setIsStartAuctionModalOpen(true); // Open the start auction modal
  };

  // Handler called by StartAuctionModal upon successful submission
  const handleStartAuctionSubmitOnDetail = async (createdAuctionDto) => {
    setIsStartAuctionModalOpen(false);
    const reopenedFromOrderInfo = productToAuction; // Contains originalOrderId
    setProductToAuction(null);

    alert(
      `New auction (ID: ${createdAuctionDto.id}) started! This order (ID: ${reopenedFromOrderInfo?.originalOrderId}) should now be updated to reflect this.`
    );
    console.log(
      `Frontend (OrderDetailPage): New auction ${createdAuctionDto.id} started. Original order ${reopenedFromOrderInfo?.originalOrderId} should be finalized by backend processes.`
    );

    // CRITICAL: Refresh the current order details.
    // The backend should have updated the status of this original order
    // (e.g., to ORDER_SUPERSEDED_BY_REOPEN) after the new auction was created.
    await refreshAllDetails();

    // Optionally navigate away, e.g., to the new auction or to My Auctions page
    // if (createdAuctionDto.type === 'LIVE') {
    //   navigate(`/live-auctions/${createdAuctionDto.id}`);
    // } else {
    //   navigate(`/timed-auctions/${createdAuctionDto.id}`);
    // }
  };

  const handleCloseSellerDecisionModal = async (decisionWasMade = false) => {
    setIsSellerDecisionModalOpen(false);
    if (decisionWasMade) {
      await refreshAllDetails();
    }
  };

  const handleConfirmMarkAsShipped = async (shippingData) => {
    // We need deliveryId. If not in deliveryDetails, and order status is AWAITING_SHIPMENT,
    // it implies the delivery record exists in DeliveriesService but might not have been fetched yet,
    // OR DeliveriesService.createDeliveryFromOrderEvent hasn't run / completed.
    // For this action, we ideally need the deliveryId.
    // Let's assume deliveryDetails contains the deliveryId.

    const currentDeliveryId = deliveryDetails?.deliveryId; // Get deliveryId from fetched delivery details

    if (!currentDeliveryId) {
      setMarkAsShippedError(
        "Delivery ID is missing. Cannot mark as shipped. Ensure delivery record exists."
      );
      // Potentially try to re-fetch delivery details here if it's expected to exist.
      return;
    }

    setIsMarkingAsShipped(true);
    setMarkAsShippedError("");
    try {
      // API Call to Deliveries Service
      // Endpoint: /deliveries/{deliveryId}/ship (as per DeliveriesServiceImpl)
      // The actual path via gateway might be /deliveries/{deliveryId}/ship
      await apiClient.post(
        `/deliveries/${currentDeliveryId}/ship`,
        shippingData
      );
      alert("Order marked as shipped successfully!");
      setIsMarkAsShippedModalOpen(false);
      await refreshAllDetails(); // Refresh both order and delivery details
    } catch (err) {
      console.error("Failed to mark order as shipped:", err);
      setMarkAsShippedError(
        err.response?.data?.message ||
          "Could not mark as shipped. Please try again."
      );
    } finally {
      setIsMarkingAsShipped(false);
    }
  };

  const handleOpenMarkAsDeliveredModal = () => {
    if (
      !deliveryDetails ||
      deliveryDetails.deliveryStatus !== "SHIPPED_IN_TRANSIT"
    ) {
      alert("Order is not in 'Shipped - In Transit' status.");
      return;
    }
    setMarkAsDeliveredError("");
    setDeliveredNotes(""); // Reset notes
    setIsMarkAsDeliveredModalOpen(true);
  };

  const handleConfirmMarkAsDelivered = async () => {
    const currentDeliveryId = deliveryDetails?.deliveryId;
    if (!currentDeliveryId) {
      setMarkAsDeliveredError("Delivery ID is missing.");
      return;
    }

    setIsMarkingAsDelivered(true);
    setMarkAsDeliveredError("");
    try {
      // API Call to Deliveries Service: POST /deliveries/{deliveryId}/mark-delivered
      // Body: UpdateToDeliveredRequestDto (which contains optional notes)
      await apiClient.post(`/deliveries/${currentDeliveryId}/mark-delivered`, {
        notes: deliveredNotes,
      });
      alert("Order successfully marked as delivered!");
      setIsMarkAsDeliveredModalOpen(false);
      await refreshAllDetails(); // Refresh order and delivery details
    } catch (err) {
      console.error("Failed to mark order as delivered:", err);
      setMarkAsDeliveredError(
        err.response?.data?.message ||
          "Could not mark as delivered. Please try again."
      );
    } finally {
      setIsMarkingAsDelivered(false);
    }
  };

  const handleConfirmReceiptByBuyer = async () => {
    if (!deliveryDetails?.deliveryId || !isBuyer) {
      setBuyerActionError(
        "Cannot confirm receipt. Delivery details missing or not authorized."
      );
      return;
    }
    setIsConfirmingReceipt(true);
    setBuyerActionError("");
    try {
      // API Call to Deliveries Service: POST /deliveries/{deliveryId}/buyer-confirm-receipt
      await apiClient.post(
        `/deliveries/${deliveryDetails.deliveryId}/buyer-confirm-receipt`
      );
      alert("Thank you for confirming receipt!");
      await refreshAllDetails(); // Refresh to get updated delivery status
    } catch (err) {
      console.error("Error confirming receipt:", err);
      setBuyerActionError(
        err.response?.data?.message ||
          "Failed to confirm receipt. Please try again."
      );
      // Potentially show error in a toast or modal
      alert(err.response?.data?.message || "Failed to confirm receipt.");
    } finally {
      setIsConfirmingReceipt(false);
    }
  };

  const handleRequestReturnByBuyer = async () => {
    if (!deliveryDetails?.deliveryId || !isBuyer) {
      setBuyerActionError(
        "Cannot request return. Delivery details missing or not authorized."
      );
      return;
    }
    // For now, this is a placeholder. A real return request would likely open a new form/modal
    // to collect reason, photos, etc., and then make an API call.
    // Let's simulate the API call part for now.
    setIsRequestingReturn(true); // Use a separate loading state if the action is complex
    setBuyerActionError("");
    console.log(
      "Buyer requesting return for delivery:",
      deliveryDetails.deliveryId
    );

    // Example: Open a new page or a more complex modal for return details
    // navigate(`/orders/${order.id}/return/create`, { state: { deliveryId: deliveryDetails.deliveryId } });
    // For now, just an alert and potential backend call:
    try {
      // Dummy request DTO for now
      const returnRequestData = {
        reason: "Item not as described (placeholder)",
        comments: "Details...",
      };
      // API Call: POST /deliveries/{deliveryDetails.deliveryId}/request-return
      await apiClient.post(
        `/deliveries/${deliveryDetails.deliveryId}/request-return`,
        returnRequestData
      );
      alert(
        "Your return request has been submitted. We will contact you shortly."
      );
      await refreshAllDetails();
    } catch (err) {
      console.error("Error submitting return request:", err);
      setBuyerActionError(
        err.response?.data?.message || "Failed to submit return request."
      );
      alert(err.response?.data?.message || "Failed to submit return request.");
    } finally {
      setIsRequestingReturn(false);
    }
  };

  const handleOpenReviewModal = () => {
    setReviewSubmissionMessage({ type: "", text: "" }); // Clear previous messages
    setIsReviewModalOpen(true);
  };

  const handleCloseReviewModal = () => {
    setIsReviewModalOpen(false);
  };

  const handleReviewSuccess = (submittedReview) => {
    console.log("Review submitted successfully:", submittedReview);
    setReviewSubmissionMessage({
      type: "success",
      text: "Thank you for your review!",
    });
    // Optionally, you might want to mark something locally so the "Leave Review" button disappears or changes
    // For instance, add a state like: const [hasReviewed, setHasReviewed] = useState(false);
    // and setHasReviewed(true) here. Then use `hasReviewed` to hide the button.
    // Or, simply rely on the backend to prevent further reviews for this order.
    // Refreshing details might not show the review immediately unless the order page itself shows reviews for its items/seller.
  };

  const handleReviewError = (errorMessage) => {
    // Error is already shown in the modal, but if you want to show it on the page after modal closes:
    // setReviewSubmissionMessage({ type: 'error', text: errorMessage });
    console.error("Review submission failed:", errorMessage);
  };

  // Derived states
  const isSeller =
    initialized &&
    keycloak.authenticated &&
    order &&
    keycloak.subject === order.sellerId;
  const isBuyer =
    initialized &&
    keycloak.authenticated &&
    order &&
    keycloak.subject === order.currentBidderId;

  const isAwaitingBuyerPayment =
    order &&
    isBuyer &&
    (order.status === "AWAITING_WINNER_PAYMENT" ||
      order.status === "AWAITING_NEXT_BIDDER_PAYMENT");

  const canSellerMakeDecision =
    isSeller && order?.status === "AWAITING_SELLER_DECISION";

  const isAwaitingSellerFulfillmentConfirmation =
    order && order.status === "AWAITING_FULFILLMENT_CONFIRMATION";

  const isInShippingPhase =
    order &&
    (order.status === "AWAITING_SHIPMENT" ||
      // Add other statuses that imply shipping is active, based on your DeliveryStatus enum
      // e.g., if you map delivery statuses back to order statuses or check deliveryDetails.status
      deliveryDetails?.deliveryStatus === "SHIPPED_IN_TRANSIT" ||
      deliveryDetails?.deliveryStatus === "DELIVERED" ||
      deliveryDetails?.deliveryStatus === "PENDING_PREPARATION" || // From Delivery entity
      deliveryDetails?.deliveryStatus === "READY_FOR_SHIPMENT"); // From Delivery entity

  const showDeliveryProgress =
    deliveryDetails &&
    (deliveryDetails.deliveryStatus === "SHIPPED_IN_TRANSIT" ||
      deliveryDetails.deliveryStatus === "DELIVERED" ||
      deliveryDetails.deliveryStatus === "ISSUE_REPORTED" ||
      // Show even for these if we want to display "Preparing" type statuses
      deliveryDetails.deliveryStatus === "PENDING_PREPARATION" ||
      deliveryDetails.deliveryStatus === "READY_FOR_SHIPMENT");

  const showBuyerDeliveryActions =
    isBuyer &&
    deliveryDetails &&
    deliveryDetails.deliveryStatus === "AWAITING_BUYER_CONFIRMATION";

  const showAlternateBidders =
    order &&
    !isInShippingPhase &&
    !isAwaitingBuyerPayment &&
    order.status !== "PAYMENT_SUCCESSFUL" &&
    order.status !== "AWAITING_FULFILLMENT_CONFIRMATION" &&
    !order.status?.includes("CANCELLED");

  const canLeaveReview =
    initialized &&
    keycloak.authenticated &&
    order &&
    keycloak.subject === order.currentBidderId &&
    order.status === "COMPLETED";

  let currentPayerBidAmount = 0;
  let buyerPremiumAmount = 0;
  const totalAmountDue = order?.currentAmountDue || 0;

  if (order && order.currentAmountDue != null) {
    if (
      order.currentBidderId === order.initialWinnerId &&
      order.initialWinningBidAmount != null
    ) {
      currentPayerBidAmount = order.initialWinningBidAmount;
    } else if (
      order.currentBidderId === order.eligibleSecondBidderId &&
      order.eligibleSecondBidAmount != null
    ) {
      currentPayerBidAmount = order.eligibleSecondBidAmount;
    } else if (
      order.currentBidderId === order.eligibleThirdBidderId &&
      order.eligibleThirdBidAmount != null
    ) {
      currentPayerBidAmount = order.eligibleThirdBidAmount;
    }
    if (currentPayerBidAmount > 0 && totalAmountDue >= currentPayerBidAmount) {
      buyerPremiumAmount = totalAmountDue - currentPayerBidAmount;
    } else if (totalAmountDue > 0 && currentPayerBidAmount === 0) {
      buyerPremiumAmount = 0;
    }
  }

  if (isLoading && !order)
    return <div className="text-center p-10">Loading Order Details...</div>;
  if (error)
    return <div className="text-center p-10 text-red-600">{error}</div>;
  if (!order)
    return (
      <div className="text-center p-10">
        Order data not available or not authorized.
      </div>
    );

  if (showCheckoutForm && clientSecret) {
    return (
      <div className="max-w-md mx-auto p-4 sm:p-6 lg:p-8">
        <h2 className="text-xl font-semibold mb-4">Complete Your Payment</h2>
        <Elements stripe={stripePromise} options={checkoutFormOptions}>
          <CheckoutForm
            orderId={order.id}
            amount={totalAmountDue}
            currency={order.currency || "vnd"}
            onSuccess={async (paymentIntent) => {
              alert("Payment confirmed successfully!");
              setShowCheckoutForm(false);
              await refreshAllDetails();
            }}
            onError={(stripeErrorMsg) =>
              alert(`Payment Error: ${stripeErrorMsg}`)
            }
          />
        </Elements>
        <button
          onClick={() => setShowCheckoutForm(false)}
          className="mt-4 w-full px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-100"
        >
          Cancel Payment
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 sm:p-6 lg:p-8">
      <OrderHeader order={order} />
      <OrderItems items={order?.items} currency={order?.currency} />

      {isAwaitingBuyerPayment && (
        <OrderPaymentDetails
          order={order}
          userProfile={userProfile}
          currentPayerBidAmount={currentPayerBidAmount}
          buyerPremiumAmount={buyerPremiumAmount}
          totalAmountDue={totalAmountDue}
          isProcessing={isProcessing}
          paymentProcessing={paymentProcessing}
          onDeclinePurchase={handleOpenCancelConfirm}
          onInitiatePayment={handleOpenPaymentAttempt} // For saved card or direct new card (if modal is skipped)
          onOpenPaymentModal={() => setIsPaymentConfirmOpen(true)} // To open the choice modal
        />
      )}

      {showBuyerDeliveryActions && (
        <BuyerDeliveryActions
          deliveryDetails={deliveryDetails}
          onConfirmReceipt={handleConfirmReceiptByBuyer}
          onRequestReturn={handleRequestReturnByBuyer}
          isLoadingConfirm={isConfirmingReceipt}
          isLoadingReturn={isRequestingReturn}
          // You could also pass buyerActionError to display in the component
        />
      )}
      {buyerActionError && showBuyerDeliveryActions && (
        <div className="my-2 p-3 bg-red-100 text-red-700 text-sm rounded-md">
          {buyerActionError}
        </div>
      )}

      {isSeller && (
        <OrderSellerActions
          order={order}
          isProcessing={isProcessing}
          deliveryDetails={deliveryDetails}
          onOpenSellerCancelModal={openSellerCancelModal}
          onOpenConfirmFulfillmentModal={openConfirmFulfillmentModal}
          isAwaitingSellerFulfillmentConfirmation={
            isAwaitingSellerFulfillmentConfirmation
          }
          onOpenMarkAsShippedModal={handleOpenMarkAsShippedModal}
          onOpenMarkAsDeliveredModal={handleOpenMarkAsDeliveredModal}
          onOpenSellerDecisionModal={handleOpenSellerDecisionModal}
          canSellerMakeDecision={canSellerMakeDecision}
        />
      )}

      {isSeller && isInShippingPhase && deliveryDetails && (
        <BuyerShippingInfo deliveryDetails={deliveryDetails} />
      )}

      {showDeliveryProgress && (
        <OrderDeliveryProgress deliveryDetails={deliveryDetails} />
      )}

      {showAlternateBidders && ( // Only show if relevant
        <OrderAlternateBidders order={order} />
      )}

      <OrderInfoBanners
        order={order}
        isAwaitingSellerFulfillmentConfirmation={
          isAwaitingSellerFulfillmentConfirmation
        }
      />

      {reviewSubmissionMessage.text && (
        <div
          className={`my-4 p-3 rounded text-sm ${
            reviewSubmissionMessage.type === "success"
              ? "bg-green-100 text-green-700"
              : "bg-red-100 text-red-700"
          }`}
        >
          {reviewSubmissionMessage.text}
        </div>
      )}

      {/* --- "Leave a Review" Button Section for Buyer --- */}
      {canLeaveReview && (
        <div className="mt-6 p-4 bg-white rounded-lg shadow">
          <h3 className="text-lg font-semibold mb-3 text-gray-800">
            Share Your Feedback
          </h3>
          <p className="text-sm text-gray-600 mb-4">
            Your order is complete. Would you like to leave a review for the
            seller, {order.sellerUsernameSnapshot || "this seller"}?
          </p>
          <button
            onClick={handleOpenReviewModal}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 px-4 rounded transition duration-150 ease-in-out"
          >
            Leave a Review
          </button>
        </div>
      )}

      {/* Modals remain here as they are controlled by OrderDetailPage's state */}
      <ConfirmationModal
        isOpen={isPaymentConfirmOpen}
        onClose={() => {
          if (!paymentProcessing) setIsPaymentConfirmOpen(false);
        }}
        title="Confirm Payment"
        isLoading={paymentProcessing}
        error={modalError}
      >
        <p className="mb-4">
          How would you like to pay for {totalAmountDue.toLocaleString("vi-VN")}{" "}
          {order?.currency || "VNĐ"}?
        </p>
        {userProfile?.hasDefaultPaymentMethod &&
          userProfile?.defaultCardLast4 && (
            <button
              onClick={() => handleConfirmPaymentChoice(true)}
              disabled={paymentProcessing}
              className="w-full mb-2 inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-teal-600 text-white rounded hover:bg-teal-700 text-sm font-bold"
            >
              <FaHistory /> Use Saved: {userProfile.defaultCardBrand} ****
              {userProfile.defaultCardLast4}
            </button>
          )}
        <button
          onClick={() => handleConfirmPaymentChoice(false)}
          disabled={paymentProcessing}
          className="w-full inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-red-600 text-white rounded hover:bg-red-700 text-sm font-bold"
        >
          <FaCreditCard /> Use New Card
        </button>
        <button
          onClick={() => setIsPaymentConfirmOpen(false)}
          disabled={paymentProcessing}
          className="w-full mt-3 px-5 py-2.5 border border-gray-300 text-gray-700 rounded hover:bg-gray-100 text-sm"
        >
          Cancel
        </button>
      </ConfirmationModal>

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
        onClose={closeSellerCancelModal}
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
              placeholder="e.g. Item unavailable"
            />
          </>
        }
      />
      <ConfirmationModal
        isOpen={isConfirmFulfillmentOpen}
        onClose={closeConfirmFulfillmentModal}
        onConfirm={handleConfirmFulfillment}
        title="Confirm Order Fulfillment"
        message="Are you sure you are ready to prepare this item for shipping? This will notify the buyer and move the order to the Awaiting Shipment stage."
        confirmText="Yes, Confirm Fulfillment"
        cancelText="No, Not Yet"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isProcessing}
        error={modalError}
      />
      <MarkAsShippedFormModal
        isOpen={isMarkAsShippedModalOpen}
        onClose={() => {
          if (!isMarkingAsShipped) {
            // Prevent closing if actively submitting
            setIsMarkAsShippedModalOpen(false);
            setMarkAsShippedError(""); // Clear error when closing
          }
        }}
        onSubmit={handleConfirmMarkAsShipped} // Your existing handler
        orderId={order?.id}
        deliveryId={deliveryDetails?.deliveryId}
        isLoading={isMarkingAsShipped}
        apiError={markAsShippedError} // Pass the error from API attempts
      />
      <MarkAsDeliveredFormModal
        isOpen={isMarkAsDeliveredModalOpen}
        onClose={() => {
          if (!isMarkingAsDelivered) {
            // Prevent closing if actively submitting
            setIsMarkAsDeliveredModalOpen(false);
            setMarkAsDeliveredError(""); // Clear error on close
          }
        }}
        onSubmit={handleConfirmMarkAsDelivered}
        orderId={order?.id}
        deliveryId={deliveryDetails?.deliveryId}
        deliveryDetails={deliveryDetails} // Pass for context display
        isLoading={isMarkingAsDelivered}
        apiError={markAsDeliveredError}
      />
      {order &&
        order.sellerId && ( // Condition relies on order and order.sellerId being present
          <SubmitReviewModal
            isOpen={isReviewModalOpen}
            onClose={handleCloseReviewModal}
            orderId={order.id}
            sellerId={order.sellerId} // This comes from the order object
            sellerName={order.sellerUsernameSnapshot || order.sellerId} // Fallback to sellerId if name snapshot isn't there
            onSuccess={handleReviewSuccess}
            onError={handleReviewError}
          />
        )}
      {order && isSellerDecisionModalOpen && canSellerMakeDecision && (
        <SellerDecisionModal
          order={order}
          isOpen={isSellerDecisionModalOpen}
          onClose={handleCloseSellerDecisionModal}
          onInitiateReopenAuction={handleInitiateReopenAuctionOnDetail}
        />
      )}
      {productToAuction &&
        isStartAuctionModalOpen &&
        isSeller && ( // isSeller ensures only seller can see/use it
          <StartAuctionModal
            isOpen={isStartAuctionModalOpen}
            onClose={() => {
              setIsStartAuctionModalOpen(false);
              setProductToAuction(null);
            }}
            product={productToAuction} // Contains { id, title, imageUrls, originalOrderId }
            onStartAuctionSubmit={handleStartAuctionSubmitOnDetail}
          />
        )}
    </div>
  );
}

export default OrderDetailPage;
