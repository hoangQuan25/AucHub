// src/pages/LiveAuctionDetailPage.jsx
import React, { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { useNotifications } from "../context/NotificationContext";
import apiClient from "../api/apiClient";
import CountdownTimer from "../components/CountdownTimer"; // Assuming extracted
import ConfirmationModal from "../components/ConfirmationModal";
import CollapsibleSection from "../components/CollapsibleSection";
import AuctionRules from "../components/AuctionRules";
import AuctionChatPanel from "../components/AuctionChatPanel";
import { FaChevronLeft, FaChevronRight, FaEye } from "react-icons/fa"; // Icons for arrows
import SockJS from "sockjs-client/dist/sockjs"; // Use specific path for wider compatibility
import { Client } from "@stomp/stompjs";

function LiveAuctionDetailPage() {
  const { auctionId } = useParams();
  const { keycloak, initialized } = useKeycloak();
  const { followAuction, followedAuctionIds } = useNotifications();
  const isFollowing = followedAuctionIds.has(auctionId);
  const navigate = useNavigate();

  const [auctionDetails, setAuctionDetails] = useState(null);
  const [bidHistory, setBidHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [isBidding, setIsBidding] = useState(false);
  const [customBidAmount, setCustomBidAmount] = useState("");
  const [bidError, setBidError] = useState("");
  const [pendingBidAmount, setPendingBidAmount] = useState(null);
  const [isBidConfirmOpen, setIsBidConfirmOpen] = useState(false);
  const [bidConfirmError, setBidConfirmError] = useState("");

  const [wsStatus, setWsStatus] = useState("Connecting...");
  const ws = useRef(null);
  const [viewerCount, setViewerCount] = useState(0);
  const [viewerError, setViewerError] = useState(false);
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
  const viewersSubRef = useRef(null);

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
      setViewerError(false);

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

      viewersSubRef.current = client.subscribe(
        `/topic/auctions/${auctionId}/viewers`,
        (msg) => {
          try {
            const { count } = JSON.parse(msg.body);
            setViewerCount(count);
          } catch {
            /* ignore bad payload */
          }
        }
      );

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
      setViewerError(true);
    };

    client.onWebSocketClose = (event) => {
      console.log(
        `WebSocket Closed: Code=${event?.code}, Reason=${event?.reason}, Clean=${event?.wasClean}`
      );
      setWsStatus(`Closed (${event?.code || "Unknown"})`);
      setViewerError(true);
      // Client will attempt to reconnect automatically based on reconnectDelay
      // Clear subscription ref if connection is closed
      subscriptionRef.current = null;
      viewersSubRef.current = null;
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

      if (viewersSubRef.current) {
        viewersSubRef.current.unsubscribe(); // ← NEW
        viewersSubRef.current = null;
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
  // ─── Handlers to open/close the bid-confirmation modal ────────────
  const promptBid = (amount) => {
    setBidConfirmError("");
    setPendingBidAmount(amount);
    setIsBidConfirmOpen(true);
  };
  const handleCloseBidConfirm = () => {
    setIsBidConfirmOpen(false);
    setPendingBidAmount(null);
  };

  // ─── When the user confirms, actually place the bid ─────────────
  const handleConfirmBid = async () => {
    if (pendingBidAmount == null) return;
    setIsBidConfirmOpen(false);
    setIsBidding(true);
    setBidError("");
    const payload = { amount: pendingBidAmount };

    try {
      await apiClient.post(`/liveauctions/${auctionId}/bids`, payload);
      followAuction(auctionId, "LIVE");
    } catch (err) {
      console.error("Failed to place bid:", err);
      setBidConfirmError(
        err.response?.data?.message || err.message || "Failed to place bid."
      );
    } finally {
      setIsBidding(false);
      setPendingBidAmount(null); // Clear pending bid amount
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
  const isSeller = loggedInUserId && auctionDetails.sellerId === loggedInUserId;
  // Simplified canBid for button state - specific amount check happens in handler
  const canPlaceBid =
    initialized &&
    keycloak.authenticated &&
    auctionDetails.status === "ACTIVE" &&
    !isSeller;
  const images = auctionDetails.productImageUrls || [];
  const isAuctionActive = auctionDetails.status === "ACTIVE";
  const isAuctionScheduled = auctionDetails.status === "SCHEDULED";

  const chatDisabled = ["SOLD", "CANCELLED", "RESERVE_NOT_MET"].includes(
    auctionDetails.status
  );
  const finalChatNotice =
    auctionDetails.status === "SOLD"
      ? "Auction has ended."
      : auctionDetails.status === "CANCELLED"
      ? "Auction has been cancelled."
      : auctionDetails.status === "RESERVE_NOT_MET"
      ? "Auction ended without meeting reserve price."
      : null;

  // --- ** THIS IS THE UPDATED RENDER LOGIC ** ---
  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 p-4 max-w-7xl mx-auto">
      {/* LEFT */}
      <div className="lg:col-span-4 space-y-4">
        <h2 className="text-2xl font-bold">
          {auctionDetails.productTitleSnapshot || "Product Title Missing"}
        </h2>

        {/* Image */}
        <div className="relative h-80 rounded bg-gray-100 border overflow-hidden">
          {images.length > 0 ? (
            <>
              <img
                src={images[currentImageIndex]}
                alt={`Image ${currentImageIndex + 1}`}
                className="w-full h-full object-contain"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = "/placeholder.png";
                }}
              />
              {images.length > 1 && (
                <>
                  <button
                    onClick={handlePrevImage}
                    className="absolute top-1/2 left-2 transform -translate-y-1/2 bg-black bg-opacity-40 text-white p-2 rounded-full hover:bg-opacity-60"
                  >
                    <FaChevronLeft size="1em" />
                  </button>
                  <button
                    onClick={handleNextImage}
                    className="absolute top-1/2 right-2 transform -translate-y-1/2 bg-black bg-opacity-40 text-white p-2 rounded-full hover:bg-opacity-60"
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

        {/* Description Section */}
        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
          <CollapsibleSection title="Overview" defaultOpen>
            <div className="text-sm text-gray-700 space-y-2 px-4 pb-4">
              <p className="whitespace-pre-wrap">
                {auctionDetails.productDescription || "No description."}
              </p>
              <p>
                <strong>Condition:</strong>{" "}
                {auctionDetails.productCondition?.replace("_", " ") || "N/A"}
              </p>
              <p>
                <strong>Categories:</strong>{" "}
                {auctionDetails.productCategories
                  ?.map((c) => c.name)
                  .join(", ") || "N/A"}
              </p>
              <p className="text-xs text-gray-500">
                Seller: {auctionDetails.sellerUsernameSnapshot}
              </p>
            </div>
          </CollapsibleSection>

          <AuctionRules />
        </div>
      </div>

      {/* MIDDLE */}
      <div className="lg:col-span-4 flex flex-col gap-4 max-h-screen overflow-hidden">
        <div className="text-xs text-right text-gray-500">
          WebSocket: {wsStatus}
        </div>
        <div className="flex items-center justify-end text-xs text-gray-500 space-x-1">
          <FaEye />
          <span>{viewerCount}</span>
          {viewerError && (
            <span className="text-red-600 ml-2">
              (disconnected—please reload)
            </span>
          )}
        </div>

        <div className="bg-white p-4 rounded shadow border flex-shrink-0">
          <div className="flex justify-between items-start border-b pb-2">
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
                {isAuctionActive
                  ? "Time Remaining"
                  : isAuctionScheduled
                  ? "Auction Status"
                  : "Auction Ended"}
              </div>
              {isAuctionActive ? ( // Only show end timer if ACTIVE
                <CountdownTimer
                  endTimeMillis={new Date(auctionDetails.endTime).getTime()}
                />
              ) : isAuctionScheduled ? ( // Show "Not Started" if SCHEDULED
                <span className="font-bold text-xl text-gray-500">
                  Not Yet Started
                </span>
              ) : (
                // Show final status if ended
                <span className="font-bold text-xl text-gray-500">
                  {auctionDetails.status === "SOLD"
                    ? "SOLD"
                    : auctionDetails.status === "RESERVE_NOT_MET"
                    ? "NOT SOLD"
                    : auctionDetails.status === "CANCELLED"
                    ? "CANCELLED"
                    : "--:--"}
                </span>
              )}
            </div>
          </div>

          {/* Current Bid Info */}
          <div className="text-center my-4">
            <p className="text-sm text-gray-600 mb-1">Current Bid</p>
            <p className="text-4xl font-bold text-indigo-700">
              {(
                auctionDetails.currentBid ??
                auctionDetails.startPrice ??
                0
              ).toLocaleString("vi-VN")}{" "}
              VNĐ
            </p>
            <p className="text-xs text-gray-500">
              Leading:{" "}
              {auctionDetails.highestBidderUsernameSnapshot ? (
                isUserHighestBidder ? (
                  <span className="text-green-600 font-semibold">You</span>
                ) : (
                  auctionDetails.highestBidderUsernameSnapshot
                )
              ) : (
                "No bids yet"
              )}
            </p>
          </div>

          {/* Bidding Actions (for non-sellers) */}
          {!isSeller && keycloak.authenticated && (
            <>
              <button
                onClick={() => promptBid(auctionDetails.nextBidAmount)}
                disabled={!canPlaceBid || isBidding}
                className="w-full py-3 px-4 bg-green-600 hover:bg-green-700 text-white font-bold rounded text-lg disabled:opacity-60"
              >
                {isBidding
                  ? "Placing Bid..."
                  : `Bid ${
                      auctionDetails.nextBidAmount?.toLocaleString("vi-VN") ||
                      "N/A"
                    } VNĐ`}
              </button>
              {/* Custom Bid Input */}
              <div className="mt-3 flex items-center gap-2">
                <input
                  type="number"
                  value={customBidAmount}
                  onChange={(e) => setCustomBidAmount(e.target.value)}
                  min={auctionDetails.nextBidAmount || 0}
                  placeholder={`Or input bid, min ${
                    auctionDetails.nextBidAmount?.toLocaleString("vi-VN") ||
                    "..."
                  } VNĐ`}
                  className="flex-1 px-3 py-2 border rounded text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                />
                <button
                  onClick={() => {
                    const bidNum = parseInt(customBidAmount);
                    const minBid = auctionDetails.nextBidAmount;
                    if (isNaN(bidNum) || bidNum < minBid) {
                      setBidError(
                        `Bid must be ≥ ${minBid.toLocaleString("vi-VN")} VNĐ.`
                      );
                      setTimeout(() => setBidError(""), 3000);
                    } else {
                      promptBid(bidNum);
                    }
                  }}
                  disabled={isBidding || !customBidAmount}
                  className="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded text-sm disabled:opacity-60"
                >
                  Bid
                </button>
              </div>
              {bidError && (
                <p className="text-center text-xs text-red-500 mt-2">
                  {bidError}
                </p>
              )}
              <p className="text-center text-xs text-gray-500 mt-2">
                (Next required bid:{" "}
                {auctionDetails?.nextBidAmount?.toLocaleString("vi-VN") ||
                  "N/A"}{" "}
                VNĐ)
              </p>
            </>
          )}
          {isSeller && (
            <p className="text-center text-sm text-gray-600 py-4">
              You cannot bid on your own auction.
            </p>
          )}
          {!keycloak.authenticated && (
            <p className="text-center text-sm text-orange-600 py-4">
              Please log in to place a bid.
            </p>
          )}

          {/* Seller Actions (Hammer/Cancel) */}
          {isSeller && (
            <div className="flex justify-between items-center gap-4 mt-4 pt-4 border-t">
              <button
                onClick={promptCancelAuction}
                disabled={!isAuctionActive || isCancelling || isHammering}
                className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded text-sm disabled:opacity-60"
              >
                {" "}
                {isCancelling ? "Cancelling..." : "Cancel Auction"}{" "}
              </button>
              <button
                onClick={promptHammerDown}
                disabled={
                  !isAuctionActive ||
                  isCancelling ||
                  isHammering ||
                  !auctionDetails.highestBidderId ||
                  (auctionDetails.reservePrice && !auctionDetails.reserveMet)
                }
                className="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded text-sm disabled:opacity-60"
              >
                {" "}
                {isHammering ? "Ending..." : "Hammer Down"}{" "}
              </button>
            </div>
          )}

          {/* == SCHEDULED State == */}
          {isAuctionScheduled && (
            <div className="text-center my-8 py-4">
              <p className="text-lg font-semibold text-gray-700 mb-2">
                Auction Starts In:
              </p>
              {/* Countdown to START time */}
              <CountdownTimer
                endTimeMillis={new Date(auctionDetails.startTime).getTime()}
              />
              <p className="text-sm text-gray-500 mt-2">
                Starting Price:{" "}
                {auctionDetails.startPrice.toLocaleString("vi-VN")} VNĐ
              </p>
              {/* Seller Cancel Button */}
              {isSeller && (
                <div className="mt-6 pt-4 border-t">
                  <button
                    onClick={promptCancelAuction}
                    disabled={isCancelling}
                    className="w-full sm:w-auto px-6 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded text-sm disabled:opacity-60"
                  >
                    {isCancelling
                      ? "Cancelling..."
                      : "Cancel Scheduled Auction"}
                  </button>
                </div>
              )}
            </div>
          )}

          {auctionDetails.status === "SOLD" && (
            <div className="flex items-center gap-3 p-4 bg-green-50 border border-green-300 rounded-lg shadow-sm">
              <div className="text-xl">🏆</div>
              <div>
                <p className="font-semibold text-green-800">
                  {auctionDetails.winnerId === keycloak.subject
                    ? "Congratulations, you won!"
                    : "Auction Complete"}
                </p>
                <p className="text-green-700 text-sm">
                  Winner:{" "}
                  <strong>
                    {auctionDetails.highestBidderUsernameSnapshot}
                  </strong>{" "}
                  with{" "}
                  <strong>
                    {auctionDetails.winningBid.toLocaleString("vi-VN")} VNĐ
                  </strong>
                </p>
              </div>
            </div>
          )}

          {/* Bid History - scrollable */}
          <div className="bg-white p-4 rounded shadow border overflow-hidden flex flex-col max-h-[300px]">
            <h4 className="font-semibold mb-2 text-sm border-b pb-1">
              Bid History
            </h4>

            <div className="overflow-y-auto flex-1">
              {bidHistory.length === 0 ? (
                <p className="text-xs text-gray-500">No bids placed yet.</p>
              ) : (
                <ul className="divide-y divide-dashed divide-gray-200 text-sm">
                  {bidHistory.map((bid, i) => {
                    const isYou =
                      bid.bidderUsernameSnapshot ===
                      keycloak.tokenParsed?.preferred_username;
                    return (
                      <li
                        key={bid.id || i}
                        className="flex items-center justify-between py-2 px-1 hover:bg-gray-50 transition"
                      >
                        <span
                          className={`w-1/3 truncate ${
                            isYou ? "text-blue-600 font-bold" : "text-gray-700"
                          }`}
                        >
                          {isYou ? "You" : bid.bidderUsernameSnapshot}
                        </span>
                        <span className="w-1/3 text-center font-medium text-gray-800">
                          {bid.amount.toLocaleString("vi-VN")} VNĐ
                        </span>
                        <span className="w-1/3 text-right text-gray-500 text-xs">
                          {new Date(bid.bidTime).toLocaleTimeString()}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="lg:col-span-4 flex flex-col max-h-screen h-[calc(100vh-2rem)] overflow-hidden">
        <div className="bg-white rounded shadow border flex flex-col h-full overflow-hidden">
          {/* Chat Title */}
          <div className="px-4 py-2 border-b text-sm font-semibold text-gray-700 bg-gray-50">
            💬 Chat Window
          </div>

          {/* Chat Panel with full height */}
          <div className="flex-grow overflow-hidden">
            {chatDisabled ? (
              <div className="flex items-center justify-center h-full text-sm text-gray-600">
                {finalChatNotice || "Chat is disabled."}
              </div>
            ) : (
              <AuctionChatPanel auctionId={auctionId} />
            )}
          </div>
        </div>
      </div>

      {/* ─── Bid Confirmation Modal ──────────────────────────────────── */}
      <ConfirmationModal
        isOpen={isBidConfirmOpen}
        onClose={handleCloseBidConfirm}
        onConfirm={handleConfirmBid}
        title={
          auctionDetails.highestBidderId === keycloak.subject
            ? "You’re already the highest bidder"
            : "Confirm Your Bid"
        }
        message={
          auctionDetails.highestBidderId === keycloak.subject
            ? `You are currently leading at ${auctionDetails.currentBid?.toLocaleString(
                "vi-VN"
              )} VNĐ. Do you still want to place another bid at ${pendingBidAmount?.toLocaleString(
                "vi-VN"
              )} VNĐ?`
            : `Place a bid of ${pendingBidAmount?.toLocaleString("vi-VN")} VNĐ?`
        }
        confirmText="Yes, Place Bid"
        cancelText="No, Cancel"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isBidding}
        error={bidConfirmError}
      />

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
