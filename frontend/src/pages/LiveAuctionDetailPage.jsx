// src/pages/LiveAuctionDetailPage.jsx
import React, { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient";
import CountdownTimer from "../components/CountdownTimer"; // Assuming extracted
import ConfirmationModal from "../components/ConfirmationModal";
import CollapsibleSection from "../components/CollapsibleSection";
import AuctionRules from "../components/AuctionRules";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa"; // Icons for arrows
import SockJS from "sockjs-client/dist/sockjs"; // Use specific path for wider compatibility
import { Client } from "@stomp/stompjs";

function LiveAuctionDetailPage() {
  const { auctionId } = useParams();
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();

  const [auctionDetails, setAuctionDetails] = useState(null);
  const [bidHistory, setBidHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isBidding, setIsBidding] = useState(false);
  const [bidError, setBidError] = useState("");
  const [wsStatus, setWsStatus] = useState("Connecting...");
  const ws = useRef(null);
  const loggedInUserId = initialized ? keycloak.subject : null;

  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isHammerConfirmOpen, setIsHammerConfirmOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState("");
  const [isHammering, setIsHammering] = useState(false);
  const [hammerError, setHammerError] = useState("");

  // --- Ref to hold the STOMP client instance ---
  const stompClientRef = useRef(null);
  // Ref to hold the subscription object to allow unsubscribing
  const subscriptionRef = useRef(null);

  // --- State for Image Carousel ---
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // --- Fetch Initial Auction Details (useEffect) ---
  useEffect(() => {
    // Reset state when auctionId changes or component mounts
    setAuctionDetails(null);
    setBidHistory([]);
    setCurrentImageIndex(0);
    setIsLoading(true);
    setError("");
    setWsStatus("Connecting..."); // Reset WS status too

    if (!auctionId || !initialized) {
      setIsLoading(false); // Stop loading if we can't fetch yet
      return;
    }

    console.log(`FETCHING initial details for auction ${auctionId}...`);
    apiClient
      .get(`/liveauctions/${auctionId}/details`)
      .then((response) => {
        console.log(
          "FETCH SUCCESS: Initial details raw response.data:",
          JSON.stringify(response.data, null, 2)
        );
        if (response.data && response.data.id) {
          // Check if data looks valid
          setAuctionDetails(response.data);
          setBidHistory(response.data.recentBids || []);
          // setCurrentImageIndex(0); // Already reset above
          console.log("FETCH SUCCESS: auctionDetails state SHOULD BE set.");
        } else {
          console.error(
            "FETCH SUCCESS BUT INVALID DATA: Response data is missing key fields or empty."
          );
          setError("Received invalid or empty data for auction details.");
          setAuctionDetails(null);
        }
      })
      .catch((err) => {
        console.error("FETCH FAILED:", err);
        if (err.response) {
          console.error("FETCH FAILED - Response Data:", err.response.data);
          console.error("FETCH FAILED - Response Status:", err.response.status);
          console.error(
            "FETCH FAILED - Response Headers:",
            err.response.headers
          );
        } else if (err.request) {
          console.error("FETCH FAILED - No response received:", err.request);
        } else {
          console.error(
            "FETCH FAILED - Error setting up request:",
            err.message
          );
        }
        setError(
          err.response?.data?.message ||
            `Could not load auction ${auctionId}. Details in console.`
        );
        setAuctionDetails(null);
      })
      .finally(() => {
        setIsLoading(false);
        console.log("FETCH finished.");
      });
  }, [auctionId, initialized]); // Rerun when auctionId or initialized status changes

  // --- WebSocket Connection (useEffect) ---
  // --- MODIFIED: WebSocket/STOMP Connection (useEffect) ---
  useEffect(() => {
    // Connect only if we have auctionId and user is authenticated
    if (!auctionId || !initialized || !keycloak.authenticated) {
      setWsStatus("Not Connected (Prerequisites not met)");
      // Ensure any existing client is deactivated if auth state changes
      if (stompClientRef.current?.active) {
        console.log(
          "Deactivating existing STOMP client due to unmet prerequisites."
        );
        stompClientRef.current.deactivate();
      }
      return;
    }

    // Prevent multiple connections if one is already active/activating
    if (stompClientRef.current?.active) {
      console.log("STOMP client already active.");
      return;
    }

    console.log(`Setting up STOMP connection for auction ${auctionId}...`);
    setWsStatus("Connecting...");

    // --- STOMP Client Configuration ---
    const client = new Client({
      // Use SockJS as the transport
      webSocketFactory: () => {
        // URL points to the STOMP endpoint configured in the backend WebSocketStompConfig
        // Typically served by the backend service itself (or Gateway if proxying STOMP)
        // We connect to the Gateway which proxies to LiveAuctions' /ws endpoint
        const gatewayHost = "localhost:8072"; // Your Gateway host/port
        const sockjsUrl = `${window.location.protocol}//${gatewayHost}/ws`; // Use http/https based on current page
        console.log(`Creating SockJS connection to: ${sockjsUrl}`);
        return new SockJS(sockjsUrl);
      },
      // Optional: Headers sent during STOMP CONNECT frame
      // Authentication often handled via cookies or query params in SockJS URL,
      // but connect headers can sometimes be used if backend is configured.
      // connectHeaders: {
      //   Authorization: `Bearer ${keycloak.token}`, // MAY NOT WORK depending on server config
      // },
      debug: (str) => {
        // Optional: Enable STOMP protocol debugging
        console.log("STOMP DEBUG:", str);
      },
      reconnectDelay: 5000, // Attempt reconnect every 5 seconds
      heartbeatIncoming: 4000, // Expect server heartbeats
      heartbeatOutgoing: 4000, // Send client heartbeats
    });

    // --- STOMP Event Handlers ---
    client.onConnect = (frame) => {
      console.log(`STOMP Connected: ${frame}`);
      setWsStatus("Connected");

      // Define the destination topic to subscribe to
      const destination = `/topic/auctions/${auctionId}`;
      console.log(`Subscribing to STOMP destination: ${destination}`);

      // Subscribe and store the subscription reference
      subscriptionRef.current = client.subscribe(destination, (message) => {
        try {
          const stateUpdate = JSON.parse(message.body); // LiveAuctionStateDto
          console.log("STOMP message received:", stateUpdate);

          /* 1️⃣  Merge the aggregate auction fields  */
          setAuctionDetails((prev) => {
            if (!prev || prev.id !== stateUpdate.auctionId) return prev; // ignore stray msgs

            return {
              ...prev,
              status: stateUpdate.status,
              currentBid: stateUpdate.currentBid,
              highestBidderId: stateUpdate.highestBidderId,
              highestBidderUsernameSnapshot: stateUpdate.highestBidderUsername,
              nextBidAmount: stateUpdate.nextBidAmount,
              timeLeftMs: stateUpdate.timeLeftMs,
              endTime: stateUpdate.endTime ?? prev.endTime,
              reserveMet: stateUpdate.reserveMet,

              // NEW winner info (populated when status === "SOLD")
              winnerId: stateUpdate.winnerId ?? prev.winnerId,
              winningBid: stateUpdate.winningBid ?? prev.winningBid,
            };
          });

          /* 2️⃣  Append the new bid (if the event carried one) */
          if (stateUpdate.newBid) {
            setBidHistory((prev) => [stateUpdate.newBid, ...prev].slice(0, 20));
          }
        } catch (e) {
          console.error("Failed to parse STOMP message body:", message.body, e);
        }
      });

      console.log("STOMP subscription active.");
    };

    client.onStompError = (frame) => {
      console.error("STOMP Broker reported error: " + frame.headers["message"]);
      console.error("STOMP Additional details: " + frame.body);
      setWsStatus(`Error (STOMP: ${frame.headers["message"]})`);
      // Optionally attempt reconnect or display error to user
    };

    client.onWebSocketError = (error) => {
      console.error("WebSocket Error: ", error);
      setWsStatus("Error (WebSocket)");
      // Underlying transport error
    };

    client.onWebSocketClose = (event) => {
      console.log(
        `WebSocket Closed: Code=${event?.code}, Reason=${event?.reason}, Clean=${event?.wasClean}`
      );
      setWsStatus(`Closed (${event?.code || "Unknown"})`);
      // Client will attempt to reconnect automatically based on reconnectDelay
      // Clear subscription ref if connection is closed
      subscriptionRef.current = null;
      stompClientRef.current = null; // Clear client ref on close? Or let reconnect handle? Let's clear.
    };

    // --- Activate the client ---
    client.activate();

    // Store client instance in ref
    stompClientRef.current = client;

    // --- Cleanup function ---
    return () => {
      console.log("Running STOMP cleanup effect...");
      if (subscriptionRef.current) {
        console.log(
          `Unsubscribing from STOMP destination (sub id: ${subscriptionRef.current.id})`
        );
        try {
          subscriptionRef.current.unsubscribe();
        } catch (e) {
          console.error("Error during unsubscribe:", e);
        }
        subscriptionRef.current = null;
      }
      if (stompClientRef.current?.active) {
        console.log("Deactivating STOMP client...");
        try {
          stompClientRef.current.deactivate();
          console.log("STOMP client deactivated.");
        } catch (e) {
          console.error("Error during STOMP client deactivation:", e);
        }
      } else {
        console.log("STOMP client already inactive or null.");
      }
      stompClientRef.current = null; // Clear ref on cleanup
    };
    // Dependencies: Rerun effect if auctionId changes or user auth status changes
  }, [auctionId, initialized, keycloak.authenticated]);

  // --- Place Bid Handler (handlePlaceBid) ---
  const handlePlaceBid = async () => {
    if (!auctionDetails || isBidding || !keycloak.authenticated) {
      console.warn(
        "Bid attempt prevented: Missing details, already bidding, or not authenticated."
      );
      return;
    }
    if (auctionDetails.status !== "ACTIVE") {
      setBidError("Auction is not currently active.");
      setTimeout(() => setBidError(""), 3000);
      return;
    }
    const currentUsername = keycloak.tokenParsed?.preferred_username;
    if (auctionDetails.highestBidderUsername === currentUsername) {
      setBidError("You are already the highest bidder.");
      setTimeout(() => setBidError(""), 3000);
      return;
    }
    if (!auctionDetails.nextBidAmount) {
      setBidError("Next bid amount is not available.");
      setTimeout(() => setBidError(""), 3000);
      return;
    }

    setIsBidding(true);
    setBidError("");
    const bidAmount = auctionDetails.nextBidAmount;
    const payload = { amount: bidAmount };
    console.log(
      `Attempting to place bid: ${bidAmount} for auction ${auctionId}`
    );
    try {
      await apiClient.post(`/liveauctions/${auctionId}/bids`, payload);
      console.log(`Bid placement request for ${bidAmount} sent successfully.`);
      // UI update comes via WebSocket
    } catch (err) {
      console.error("Failed to place bid:", err);
      const message =
        err.response?.data?.message || err.message || "Failed to place bid.";
      setBidError(message);
    } finally {
      setIsBidding(false);
    }
  };

  // --- Image Carousel Handlers ---
  const handleNextImage = (e) => {
    e.stopPropagation();
    if (auctionDetails?.productImageUrls?.length > 0) {
      setCurrentImageIndex(
        (prevIndex) => (prevIndex + 1) % auctionDetails.productImageUrls.length
      );
    }
  };
  const handlePrevImage = (e) => {
    e.stopPropagation();
    if (auctionDetails?.productImageUrls?.length > 0) {
      setCurrentImageIndex(
        (prevIndex) =>
          (prevIndex - 1 + auctionDetails.productImageUrls.length) %
          auctionDetails.productImageUrls.length
      );
    }
  };

  // -- Cancel Action --
  const promptCancelAuction = () => {
    setCancelError(""); // Clear previous errors
    setIsCancelConfirmOpen(true);
  };

  const handleCloseCancelConfirm = () => {
    setIsCancelConfirmOpen(false);
  };

  const handleConfirmCancel = async () => {
    setIsCancelling(true);
    setCancelError("");
    console.log(`Attempting to CANCEL auction ${auctionId}`);
    try {
      // Assumes backend endpoint exists: POST /api/v1/liveauctions/{auctionId}/cancel
      await apiClient.post(`/liveauctions/${auctionId}/cancel`);
      console.log(`Auction ${auctionId} cancel request sent successfully.`);
      // Rely on WebSocket to update the status to CANCELLED
      setIsCancelConfirmOpen(false); // Close modal on success
    } catch (err) {
      console.error("Failed to cancel auction:", err);
      const message =
        err.response?.data?.message ||
        err.message ||
        "Failed to cancel auction.";
      setCancelError(message); // Show error within the confirmation modal
    } finally {
      setIsCancelling(false);
    }
  };

  // -- Hammer Down Action --
  const promptHammerDown = () => {
    // Double-check conditions just in case (though button should be disabled)
    if (
      !auctionDetails?.highestBidderId ||
      (auctionDetails?.reservePrice != null && !auctionDetails?.reserveMet)
    ) {
      console.warn("Hammer down prompted but conditions not met.");
      return;
    }
    setHammerError(""); // Clear previous errors
    setIsHammerConfirmOpen(true);
  };

  const handleCloseHammerConfirm = () => {
    setIsHammerConfirmOpen(false);
  };

  const handleConfirmHammer = async () => {
    setIsHammering(true);
    setHammerError("");
    console.log(`Attempting to HAMMER DOWN auction ${auctionId}`);
    try {
      // Uses existing backend endpoint: POST /api/v1/liveauctions/{auctionId}/hammer
      await apiClient.post(`/liveauctions/${auctionId}/hammer`);
      console.log(
        `Auction ${auctionId} hammer down request sent successfully.`
      );
      // Rely on WebSocket to update the status to SOLD
      setIsHammerConfirmOpen(false); // Close modal on success
    } catch (err) {
      console.error("Failed to hammer down auction:", err);
      const message =
        err.response?.data?.message ||
        err.message ||
        "Failed to end auction now.";
      setHammerError(message); // Show error within the confirmation modal
    } finally {
      setIsHammering(false);
    }
  };

  // --- Render Logic ---
  if (isLoading)
    return <div className="text-center p-10">Loading Auction Details...</div>;
  if (error)
    return <div className="text-center p-10 text-red-600">{error}</div>;
  // Check if auctionDetails is loaded before trying to access its properties
  if (!auctionDetails)
    return (
      <div className="text-center p-10">
        Auction data not available or invalid.
      </div>
    );

  // Derived state for rendering checks
  const isUserHighestBidder =
    initialized &&
    keycloak.authenticated &&
    auctionDetails.highestBidderUsername ===
      keycloak.tokenParsed?.preferred_username;
  const canBid =
    initialized &&
    keycloak.authenticated &&
    auctionDetails.status === "ACTIVE" &&
    !isUserHighestBidder &&
    auctionDetails.nextBidAmount != null;
  // Use the correct field for images from the DTO
  const images = auctionDetails.productImageUrls || []; // Ensure images is an array
  const isHighest = auctionDetails.highestBidderId === keycloak.subject;

  // --- ** THIS IS THE UPDATED RENDER LOGIC ** ---
  return (
    <div className="flex flex-col md:flex-row gap-6 p-4 max-w-7xl mx-auto">
      {/* Left Side: Product Info */}
      <div className="w-full md:w-1/2 lg:w-2/5">
        {/* Use title snapshot from auctionDetails */}
        <h2 className="text-2xl font-bold mb-3">
          {auctionDetails.productTitleSnapshot || "Product Title Missing"}
        </h2>

        {/* Image Carousel using auctionDetails.allProductImageUrls */}
        <div className="relative h-80 rounded mb-4 bg-gray-100 border">
          {images.length > 0 ? (
            <>
              <img
                key={currentImageIndex}
                src={images[currentImageIndex]}
                alt={`${
                  auctionDetails.productTitleSnapshot || "Auction Item"
                } - Image ${currentImageIndex + 1}`}
                className="w-full h-full object-contain rounded"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = "/placeholder.png";
                }}
              />
              {images.length > 1 && (
                <>
                  <button
                    onClick={handlePrevImage}
                    className="absolute top-1/2 left-2 transform -translate-y-1/2 bg-black bg-opacity-40 text-white p-2 rounded-full hover:bg-opacity-60 focus:outline-none transition-opacity"
                    aria-label="Previous Image"
                  >
                    <FaChevronLeft size="1em" />
                  </button>
                  <button
                    onClick={handleNextImage}
                    className="absolute top-1/2 right-2 transform -translate-y-1/2 bg-black bg-opacity-40 text-white p-2 rounded-full hover:bg-opacity-60 focus:outline-none transition-opacity"
                    aria-label="Next Image"
                  >
                    <FaChevronRight size="1em" />
                  </button>
                  <div className="absolute bottom-2 left-1/2 transform -translate-x-1/2 bg-black bg-opacity-50 text-white text-xs px-2 py-0.5 rounded">
                    {currentImageIndex + 1} / {images.length}
                  </div>
                </>
              )}
            </>
          ) : (
            <div className="flex items-center justify-center h-full text-gray-500">
              No Image Available
            </div>
          )}
        </div>

        {/* Collapsible sections */}
        <div className="bg-white rounded-xl shadow-md border border-gray-200 divide-y divide-gray-100 overflow-hidden">
          <CollapsibleSection title="Overview" defaultOpen>
            <div className="space-y-2 text-sm text-gray-700 leading-relaxed">
              <p className="whitespace-pre-wrap">
                {auctionDetails.productDescription || "No description."}
              </p>

              <p>
                <strong className="font-semibold text-gray-900">
                  Condition:
                </strong>{" "}
                {auctionDetails.productCondition
                  ? auctionDetails.productCondition.replace("_", " ")
                  : "N/A"}
              </p>
              <p>
                <strong className="font-semibold text-gray-900">
                  Categories:
                </strong>{" "}
                {auctionDetails.productCategories?.length
                  ? auctionDetails.productCategories
                      .map((c) => c.name)
                      .join(", ")
                  : "N/A"}
              </p>
              <p className="text-xs text-gray-500 mt-2">
                Seller: {auctionDetails.sellerUsernameSnapshot || "N/A"}
              </p>
            </div>
          </CollapsibleSection>

          <AuctionRules />
        </div>
      </div>

      {/* Right Side: Bidding Info & Actions */}
      <div className="w-full md:w-1/2 lg:w-3/5">
        {/* WebSocket Status Indicator */}
        <div className="text-xs text-right mb-1 text-gray-500">
          WebSocket: {wsStatus}
        </div>

        <div className="bg-white p-4 rounded shadow-sm border mb-4">
          {/* Status Bar */}
          <div className="flex justify-between items-center border-b pb-2 mb-3">
            <span
              className={`text-sm font-medium ${
                auctionDetails.reserveMet ? "text-green-600" : "text-orange-600"
              }`}
            >
              {auctionDetails.reserveMet
                ? "✔ Reserve Met"
                : auctionDetails.reservePrice
                ? "Reserve Not Met"
                : "No Reserve"}
            </span>
            <div className="text-right">
              <div className="text-xs text-gray-500">
                {auctionDetails.status !== "ACTIVE"
                  ? "Auction Ended"
                  : "Time Remaining"}
              </div>
              {auctionDetails.status === "ACTIVE" ? (
                <CountdownTimer
                  endTimeMillis={new Date(auctionDetails.endTime).getTime()}
                  onEnd={() =>
                    console.log(
                      "Timer visually ended - waiting for server state"
                    )
                  }
                />
              ) : (
                <span className="font-bold text-xl text-gray-500">
                  {/* Display final status if ended */}
                  {auctionDetails.status === "SOLD"
                    ? "SOLD"
                    : auctionDetails.status === "RESERVE_NOT_MET"
                    ? "NOT SOLD"
                    : auctionDetails.status === "CANCELLED"
                    ? "CANCELLED"
                    : "--:--"}
                </span>
              )}
              {auctionDetails.reserveMet && (
                <span className="block text-xs text-green-600 mt-1">
                  Reserve met – auction will definitely sell
                </span>
              )}
            </div>
          </div>

          {/* Bidding Area */}
          <div className="text-center my-4">
            <p className="text-sm text-gray-600 mb-1">Current Bid</p>
            <p className="text-4xl font-bold text-indigo-700">
              {/* Display winning bid if sold, otherwise current/start */}
              {(auctionDetails.status === "SOLD"
                ? auctionDetails.winningBid
                : auctionDetails.currentBid ?? auctionDetails.startPrice ?? 0
              ).toLocaleString("vi-VN")}{" "}
              VNĐ
            </p>
            <p className="text-center text-xs text-gray-500 mb-4">
              Leading:{" "}
              {auctionDetails.highestBidderUsernameSnapshot ? (
                isHighest ? (
                  <span className="text-green-600 font-semibold">You</span>
                ) : (
                  auctionDetails.highestBidderUsernameSnapshot
                )
              ) : (
                "No bids yet"
              )}
            </p>
          </div>

          {/* Bidding Button */}
          {auctionDetails.sellerId !== keycloak.subject && (
            <button
              onClick={handlePlaceBid}
              disabled={!canBid || isBidding} // Disable based on calculated state
              className="w-full py-3 px-4 bg-green-600 hover:bg-green-700 text-white font-bold rounded text-lg disabled:opacity-60 disabled:cursor-not-allowed transition duration-150"
            >
              {isBidding
                ? "Placing Bid..."
                : !auctionDetails?.nextBidAmount
                ? "Bidding Unavailable"
                : `Bid ${auctionDetails.nextBidAmount.toLocaleString(
                    "vi-VN"
                  )} VNĐ`}
            </button>
          )}
          {auctionDetails.sellerId === keycloak.subject &&
            (auctionDetails.status === "SCHEDULED" ||
              auctionDetails.status === "ACTIVE") && ( // Only show for seller when ACTIVE
              <div className="flex justify-center items-center space-x-4 mt-4 pt-4 border-t">
                {/* Cancel Button */}
                <button
                  onClick={promptCancelAuction}
                  disabled={isCancelling || isHammering} // Disable if any action is pending
                  className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded text-sm shadow disabled:opacity-60 disabled:cursor-not-allowed transition duration-150"
                >
                  {isCancelling ? "Cancelling..." : "Cancel Auction"}
                </button>

                {/* Hammer Down Button */}
                <button
                  onClick={promptHammerDown}
                  disabled={
                    isCancelling ||
                    isHammering || // Disable if any action is pending
                    !auctionDetails.highestBidderId || // Disable if no bids yet
                    (auctionDetails.reservePrice != null &&
                      !auctionDetails.reserveMet) // Disable if reserve exists and not met
                  }
                  title={
                    // Add tooltip explaining why it might be disabled
                    !auctionDetails.highestBidderId
                      ? "Cannot end auction before the first bid"
                      : auctionDetails.reservePrice != null &&
                        !auctionDetails.reserveMet
                      ? "Reserve price not met yet"
                      : "End the auction now and sell to the highest bidder"
                  }
                  className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded text-sm shadow disabled:opacity-60 disabled:cursor-not-allowed transition duration-150"
                >
                  {isHammering ? "Ending..." : "Hammer Down"}
                </button>
              </div>
            )}
          {/* --- END MODIFIED Seller Action Area --- */}

          {bidError && (
            <p className="text-center text-xs text-red-500 mt-2">{bidError}</p>
          )}
          <p className="text-center text-xs text-gray-500 mt-2">
            (Next required bid:{" "}
            {auctionDetails?.nextBidAmount?.toLocaleString("vi-VN") || "N/A"}{" "}
            VNĐ)
          </p>
        </div>

        {auctionDetails.status === "SOLD" && (
          <div className="flex items-center gap-3 mt-4 p-4 bg-green-50 border border-green-300 rounded-lg shadow-sm">
            <div className="flex-shrink-0">
              {/* Trophy Icon (emoji or replace with real icon if you want) */}
              🏆
            </div>
            <div className="flex-1">
              <p className="text-green-800 font-semibold">
                Congratulations
                {auctionDetails.winnerId === keycloak.subject
                  ? ", you won!"
                  : "!"}
              </p>
              <p className="text-green-700 text-sm">
                {auctionDetails.winnerId === keycloak.subject ? (
                  <>
                    You won with{" "}
                    <strong>
                      {auctionDetails.winningBid.toLocaleString("vi-VN")}
                    </strong>{" "}
                    VNĐ!
                  </>
                ) : (
                  <>
                    Winner:{" "}
                    <strong>
                      {auctionDetails.highestBidderUsernameSnapshot}
                    </strong>{" "}
                    with{" "}
                    <strong>
                      {auctionDetails.winningBid.toLocaleString("vi-VN")}
                    </strong>{" "}
                    VNĐ
                  </>
                )}
              </p>
            </div>
          </div>
        )}

        {/* Bid History */}
        <div className="bg-white p-4 rounded shadow-sm border max-h-60 overflow-y-auto">
          <h4 className="font-semibold mb-2 text-sm border-b pb-1">
            Bid History
          </h4>
          {bidHistory.length === 0 ? (
            <p className="text-xs text-gray-500">No bids placed yet.</p>
          ) : (
            <ul className="space-y-1 text-xs">
              {/* Use bidHistory state */}
              {bidHistory.map((bid, index) => (
                <li
                  key={bid.id || index}
                  className="flex justify-between border-b border-dashed border-gray-200 py-0.5"
                >
                  <span>
                    {(bid.bidderUsername ?? bid.bidderUsernameSnapshot) ===
                    keycloak.tokenParsed?.preferred_username ? (
                      <span className="font-semibold text-blue-600">You</span>
                    ) : (
                      bid.bidderUsername ?? bid.bidderUsernameSnapshot
                    )}
                  </span>
                  <span className="font-medium">
                    {bid.amount.toLocaleString("vi-VN")} VNĐ
                  </span>
                  <span className="text-gray-500">
                    {new Date(bid.bidTime).toLocaleTimeString()}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
      {/* Cancel Confirmation */}
      <ConfirmationModal
        isOpen={isCancelConfirmOpen}
        onClose={handleCloseCancelConfirm}
        onConfirm={handleConfirmCancel}
        title="Confirm Cancellation"
        message={`Are you sure you want to cancel this auction for "${
          auctionDetails?.productTitleSnapshot || "this item"
        }"?\n\nThis action cannot be undone, and the item will not be sold through this auction.`}
        confirmText="Yes, Cancel Auction"
        cancelText="No, Keep Auction"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={isCancelling}
        error={cancelError}
      />

      {/* Hammer Down Confirmation */}
      <ConfirmationModal
        isOpen={isHammerConfirmOpen}
        onClose={handleCloseHammerConfirm}
        onConfirm={handleConfirmHammer}
        title="Confirm Hammer Down"
        message={`Sell "${
          auctionDetails?.productTitleSnapshot || "this item"
        }" now to bidder '${
          auctionDetails?.highestBidderUsernameSnapshot || "N/A"
        }' for ${
          auctionDetails?.currentBid?.toLocaleString("vi-VN") || "N/A"
        } VNĐ?\n\nThis will immediately end the auction.`}
        confirmText="Yes, Sell Now"
        cancelText="No, Continue Auction"
        confirmButtonClass="bg-blue-600 hover:bg-blue-700"
        isLoading={isHammering}
        error={hammerError}
      />
    </div>
  );
}

export default LiveAuctionDetailPage;
