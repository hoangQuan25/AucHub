// src/pages/TimedAuctionDetailPage.jsx
import React, { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient";
import CountdownTimer from "../components/CountdownTimer";
import ConfirmationModal from "../components/ConfirmationModal";
import CollapsibleSection from "../components/CollapsibleSection";
import AuctionRules from "../components/AuctionRules";
// Import or create a CommentSection component later
// import CommentSection from '../components/CommentSection';
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";

const getBidIncrement = (currentBidValue) => {
  const currentBid = Number(currentBidValue) || 0; // Ensure it's a number

  if (currentBid >= 10000000) return 2000000;
  if (currentBid >= 5000000) return 1000000;
  if (currentBid >= 2000000) return 500000;
  if (currentBid >= 1000000) return 200000;
  if (currentBid >= 500000) return 100000;
  if (currentBid >= 200000) return 50000;
  if (currentBid >= 100000) return 20000;
  if (currentBid >= 50000) return 10000;
  return 5000; // Default lowest increment
};

// Helper to generate dropdown options
const generateBidOptions = (startAmount, numOptions = 20) => {
  if (isNaN(Number(startAmount)) || startAmount < 0) {
    return []; // Return empty if start amount is invalid
  }
  const options = [];
  let currentLevel = Number(startAmount);
  options.push(currentLevel); // Add the minimum next bid as the first option

  for (let i = 1; i < numOptions; i++) {
    const increment = getBidIncrement(currentLevel);
    currentLevel += increment;
    options.push(currentLevel);
  }
  return options;
};

// --- Helper: Simple Comment Display (replace with dedicated component later) ---
function CommentDisplay({ comment, onReply }) {
  // Basic display, assumes comment has id, usernameSnapshot, commentText, createdAt, replies[]
  const canReply = onReply !== null; // Check if reply function is provided

  return (
    <div
      className={`p-3 rounded-lg ${
        comment.parentId
          ? "ml-6 bg-gray-50 border border-gray-200"
          : "bg-gray-100"
      }`}
    >
      <p className="font-semibold text-sm text-gray-800 mb-1">
        {comment.usernameSnapshot}
      </p>
      <p className="whitespace-pre-wrap leading-relaxed text-sm">
        {comment.commentText}
      </p>
      <div className="flex justify-between items-center mt-2">
        <p className="text-xs text-gray-500">
          {new Date(comment.createdAt).toLocaleString()}
        </p>
        {canReply &&
          !comment.parentId && ( // Only allow replying to top-level comments for now? Or nested? Let's allow all.
            <button
              onClick={() => onReply(comment.id, comment.usernameSnapshot)}
              className="text-xs text-indigo-600 hover:underline"
            >
              Reply
            </button>
          )}
      </div>
      {/* Render Replies (one level deep) */}
      {comment.replies && comment.replies.length > 0 && (
        <div className="mt-2 space-y-2 border-l-2 border-indigo-200 pl-4">
          {comment.replies.map((reply) => (
            <CommentDisplay key={reply.id} comment={reply} onReply={onReply} /> // Allow replying to replies
          ))}
        </div>
      )}
    </div>
  );
}
// --- End Helper ---

function TimedAuctionDetailPage() {
  const { auctionId } = useParams();
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();

  // --- State ---
  const [auctionDetails, setAuctionDetails] = useState(null);
  const [bidHistory, setBidHistory] = useState([]); // Visible bid history
  const [comments, setComments] = useState([]); // Comments with nested replies
  const [isLoadingDetails, setIsLoadingDetails] = useState(true);
  const [isLoadingComments, setIsLoadingComments] = useState(true);
  const [errorDetails, setErrorDetails] = useState("");
  const [errorComments, setErrorComments] = useState("");

  // Bidding State
  const [isBidding, setIsBidding] = useState(false);
  const [maxBidOptions, setMaxBidOptions] = useState([]);
  const [selectedMaxBid, setSelectedMaxBid] = useState("");
  const [bidError, setBidError] = useState("");

  // Comment State
  const [commentInput, setCommentInput] = useState("");
  const [isPostingComment, setIsPostingComment] = useState(false);
  const [commentError, setCommentError] = useState("");
  const [replyingTo, setReplyingTo] = useState(null); // { id: parentId, username: parentUsername }

  // Seller Actions State (Keep Cancel for now)
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState("");

  // --- NEW State for Hammer/End Early Action ---
  const [isHammerConfirmOpen, setIsHammerConfirmOpen] = useState(false);
  const [isHammering, setIsHammering] = useState(false); // Renamed for clarity
  const [hammerError, setHammerError] = useState("");

  // Image Carousel State
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // Polling Interval Refs
  const detailsIntervalRef = useRef(null);
  const commentsIntervalRef = useRef(null);

  const loggedInUserId = initialized ? keycloak.subject : null;

  // --- Fetching Functions with Corrected useCallback Dependencies ---
  const fetchAuctionDetails = useCallback(async () => {
    // Capture initial load state at the start of the function execution
    const isInitialLoad = isLoadingDetails;

    if (!auctionId || !initialized) {
      // If called during polling when prerequisites fail, reset loading if it was somehow true
      if (isInitialLoad) setIsLoadingDetails(false);
      return;
    }

    // Don't set loading true for polling calls
    // setErrorDetails(""); // Maybe clear only on initial load success?

    try {
      console.log(
        `${
          isInitialLoad ? "Fetching" : "Polling"
        } details for auction ${auctionId}...`
      );
      const response = await apiClient.get(`/timedauctions/${auctionId}`);
      if (response.data && response.data.id) {
        setAuctionDetails(response.data);
        setBidHistory(response.data.recentBids || []);
        // Clear error on successful fetch/poll
        setErrorDetails("");
      } else {
        console.error("Invalid data structure received for auction details.");
        if (isInitialLoad) setErrorDetails("Received invalid auction data.");
      }
    } catch (err) {
      console.error("Failed to fetch auction details:", err);
      if (isInitialLoad) {
        // Only set error on initial load failure
        setErrorDetails(
          err.response?.data?.message || `Could not load auction ${auctionId}.`
        );
      }
    } finally {
      // Set loading false only AFTER the initial load completes
      if (isInitialLoad) setIsLoadingDetails(false);
    }
    // *** REMOVE isLoadingDetails from dependency array ***
  }, [auctionId, initialized]); // Depends only on auctionId and auth readiness

  const fetchComments = useCallback(async () => {
    const isInitialLoad = isLoadingComments; // Capture initial load state

    if (!auctionId) {
      if (isInitialLoad) setIsLoadingComments(false);
      return;
    }
    // setIsLoadingComments(true); // Don't set loading on subsequent polls
    // setCommentError("");

    try {
      console.log(
        `${
          isInitialLoad ? "Fetching" : "Polling"
        } comments for auction ${auctionId}...`
      );
      const response = await apiClient.get(
        `/timedauctions/${auctionId}/comments`
      );
      setComments(response.data || []);
      setCommentError(""); // Clear error on success
    } catch (err) {
      console.error("Failed to fetch comments:", err);
      if (isInitialLoad) {
        // Only set error on initial load failure
        setCommentError(
          err.response?.data?.message || "Could not load comments."
        );
      }
    } finally {
      if (isInitialLoad) setIsLoadingComments(false);
    }
    // *** REMOVE isLoadingComments from dependency array ***
  }, [auctionId, initialized]); // Depends only on auctionId

  // --- Initial Fetch & Polling Setup (useEffect) ---
  useEffect(() => {
    // Clear previous intervals if auctionId changes
    clearInterval(detailsIntervalRef.current);
    clearInterval(commentsIntervalRef.current);

    if (auctionId && initialized) {
      // Set initial loading states *ONCE* when dependencies change
      setIsLoadingDetails(true);
      setIsLoadingComments(true);
      setErrorDetails("");
      setCommentError("");
      setAuctionDetails(null);
      setComments([]);

      // Initial fetch
      fetchAuctionDetails();
      fetchComments();

      // Setup polling intervals
      // Use the stable function references from useCallback
      detailsIntervalRef.current = setInterval(fetchAuctionDetails, 15 * 1000);
      commentsIntervalRef.current = setInterval(fetchComments, 30 * 1000);
    } else {
      setIsLoadingDetails(false);
      setIsLoadingComments(false);
    }

    // Cleanup
    return () => {
      console.log("Clearing auction detail/comment intervals");
      clearInterval(detailsIntervalRef.current);
      clearInterval(commentsIntervalRef.current);
    };
    // Dependencies remain the same, but fetchAuctionDetails/fetchComments are now stable
  }, [auctionId, initialized, fetchAuctionDetails, fetchComments]);

  useEffect(() => {
    if (auctionDetails?.nextBidAmount != null) {
      // Use != null to include 0
      const nextBid =
        auctionDetails.nextBidAmount ?? auctionDetails.startPrice ?? 0;
      const options = generateBidOptions(nextBid, 50); // Generate 25 options
      setMaxBidOptions(options);
      // Set the default selected value to the first option (the minimum next bid)
      if (options.length > 0) {
        setSelectedMaxBid(options[0]);
      } else {
        setSelectedMaxBid(""); // Clear selection if no options
      }
    } else {
      setMaxBidOptions([]); // Clear options if nextBidAmount is missing
      setSelectedMaxBid("");
    }
  }, [auctionDetails?.nextBidAmount, auctionDetails?.startPrice]);

  // --- Place Max Bid Handler ---
  const handlePlaceBid = async () => {
    if (
      !auctionDetails ||
      isBidding ||
      !keycloak.authenticated ||
      !selectedMaxBid
    )
      return;

    setBidError(""); // Clear previous error
    const maxBidNum = Number(selectedMaxBid);

    // Validation
    if (isNaN(maxBidNum) || maxBidNum <= 0) {
      setBidError("Invalid bid amount selected."); // Should ideally not happen
      return;
    }
    if (auctionDetails.status !== "ACTIVE") {
      setBidError("Auction is not currently active.");
      return;
    }

    setIsBidding(true);
    const payload = { maxBid: maxBidNum };
    console.log(
      `Attempting to place max bid: ${maxBidNum} for auction ${auctionId}`
    );

    try {
      await apiClient.post(`/timedauctions/${auctionId}/bids`, payload);
      console.log(
        `Max bid placement request for ${maxBidNum} sent successfully.`
      );
      fetchAuctionDetails();
    } catch (err) {
      console.error("Failed to place max bid:", err);
      const message =
        err.response?.data?.message || err.message || "Failed to place bid.";
      setBidError(message);
    } finally {
      setIsBidding(false);
    }
  };

  // --- Post Comment Handler ---
  const handlePostComment = async () => {
    if (!commentInput.trim() || !keycloak.authenticated) return;

    setIsPostingComment(true);
    setCommentError("");

    const payload = {
      commentText: commentInput.trim(),
      parentId: replyingTo ? replyingTo.id : null, // Include parentId if replying
    };

    try {
      await apiClient.post(`/timedauctions/${auctionId}/comments`, payload);
      setCommentInput(""); // Clear input
      setReplyingTo(null); // Clear reply state
      fetchComments(); // Refresh comments immediately
    } catch (err) {
      console.error("Failed to post comment:", err);
      const message =
        err.response?.data?.message || err.message || "Failed to post comment.";
      setCommentError(message);
    } finally {
      setIsPostingComment(false);
    }
  };

  // Handler to set the reply state
  const handleSetReply = (parentId, parentUsername) => {
    setReplyingTo({ id: parentId, username: parentUsername });
    // Optionally focus the input field here
    // commentInputRef.current?.focus();
  };
  const cancelReply = () => {
    setReplyingTo(null);
  };

  // --- Image Carousel Handlers (same as before) ---
  const handleNextImage = (e) => {
    e?.stopPropagation();
    if (auctionDetails?.productImageUrls?.length > 0) {
      setCurrentImageIndex(
        (prevIndex) => (prevIndex + 1) % auctionDetails.productImageUrls.length
      );
    }
  };
  const handlePrevImage = (e) => {
    e?.stopPropagation();
    if (auctionDetails?.productImageUrls?.length > 0) {
      setCurrentImageIndex(
        (prevIndex) =>
          (prevIndex - 1 + auctionDetails.productImageUrls.length) %
          auctionDetails.productImageUrls.length
      );
    }
  };

  // --- Cancel Auction Logic (adapted, remove hammer) ---
  const promptCancelAuction = () => {
    setCancelError("");
    setIsCancelConfirmOpen(true);
  };
  const handleCloseCancelConfirm = () => setIsCancelConfirmOpen(false);
  const handleConfirmCancel = async () => {
    setIsCancelling(true);
    setCancelError("");
    try {
      // Use correct endpoint for timed auctions
      await apiClient.post(`/timedauctions/${auctionId}/cancel`);
      setIsCancelConfirmOpen(false);
      fetchAuctionDetails(); // Refresh details after cancel request
    } catch (err) {
      console.error("Failed to cancel auction:", err);
      setCancelError(
        err.response?.data?.message || "Failed to cancel auction."
      );
    } finally {
      setIsCancelling(false);
    }
  };

  // --- NEW Handlers for Hammer/End Early Action ---
  const promptEndAuctionEarly = useCallback(() => {
    // Basic check before showing prompt
    if (
      !auctionDetails ||
      auctionDetails.status !== "ACTIVE" ||
      !auctionDetails.highestBidderId
    ) {
      console.warn("End early prompted but conditions not met.");
      // Optionally show a different message to the seller
      return;
    }
    setHammerError(""); // Clear previous errors
    setIsHammerConfirmOpen(true);
  }, [auctionDetails]); // Depends on auctionDetails

  const handleCloseHammerConfirm = useCallback(() => {
    setIsHammerConfirmOpen(false);
  }, []);

  const handleConfirmEndAuctionEarly = useCallback(async () => {
    setIsHammering(true);
    setHammerError("");
    console.log(`Attempting to END auction ${auctionId} early (hammer)`);
    try {
      // Use the hammer endpoint for timed auctions
      await apiClient.post(`/timedauctions/${auctionId}/hammer`);
      console.log(`Auction ${auctionId} end early request sent successfully.`);
      setIsHammerConfirmOpen(false); // Close modal on success
      fetchAuctionDetails(); // Refresh details immediately
    } catch (err) {
      console.error("Failed to end auction early:", err);
      const message =
        err.response?.data?.message ||
        err.message ||
        "Failed to end auction now.";
      setHammerError(message); // Show error within the confirmation modal
    } finally {
      setIsHammering(false);
    }
  }, [auctionId, fetchAuctionDetails]);

  // --- Render Logic ---
  // Initial Loading State
  if (isLoadingDetails && auctionDetails === null) {
    // Show loading only on initial fetch
    return <div className="text-center p-10">Loading Auction Details...</div>;
  }
  // Initial Error State
  if (errorDetails && auctionDetails === null) {
    // Show error only if initial fetch failed
    return <div className="text-center p-10 text-red-600">{errorDetails}</div>;
  }
  // Data Not Available after load
  if (!auctionDetails) {
    return <div className="text-center p-10">Auction data not available.</div>;
  }

  // Derived state for rendering checks
  const isUserHighestBidder =
    loggedInUserId && auctionDetails.highestBidderId === loggedInUserId;
  const canBid =
    initialized && keycloak.authenticated && auctionDetails.status === "ACTIVE"; // Anyone can place a max bid if active
  const isSeller = loggedInUserId && auctionDetails.sellerId === loggedInUserId;
  const images = auctionDetails.productImageUrls || [];

  return (
    // Use a main container that allows comments section below
    <div className="max-w-7xl mx-auto p-4">
      {/* Top Section: Grid for Product Info and Bidding/History */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-8">
        {/* LEFT Column: Product Details */}
        <div className="lg:col-span-5 space-y-4">
          <h2 className="text-2xl font-bold break-words">
            {auctionDetails.productTitleSnapshot || "Product Title"}
          </h2>

          {/* Image Carousel */}
          <div className="relative h-80 rounded bg-gray-100 border overflow-hidden">
            {images.length > 0 ? (
              <>
                <img
                  src={images[currentImageIndex]}
                  alt={`Image ${currentImageIndex + 1}`}
                  className="w-full h-full object-contain"
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
                No Image
              </div>
            )}
          </div>

          {/* Overview & Rules */}
          <div className="bg-white rounded-xl shadow-sm border divide-y">
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
        </div>{" "}
        {/* End Left Column */}
        {/* RIGHT Column: Bidding and History */}
        <div className="lg:col-span-7 space-y-4">
          {/* Bidding Panel */}
          <div className="bg-white p-4 rounded shadow border space-y-4">
            {/* Status and Timer */}
            <div className="flex justify-between items-start border-b pb-2">
              <span
                className={`text-sm font-medium ${
                  auctionDetails.reserveMet
                    ? "text-green-600"
                    : auctionDetails.reservePrice
                    ? "text-orange-600"
                    : "text-gray-500"
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
                {/* Use updated CountdownTimer */}
                <CountdownTimer
                  endTimeMillis={new Date(auctionDetails.endTime).getTime()}
                  endedText={
                    auctionDetails.status === "SOLD"
                      ? "SOLD"
                      : auctionDetails.status === "RESERVE_NOT_MET"
                      ? "Not Sold"
                      : auctionDetails.status === "CANCELLED"
                      ? "Cancelled"
                      : "Ended"
                  }
                />
              </div>
            </div>

            {/* Current Bid Info */}
            <div className="text-center my-4">
              <p className="text-sm text-gray-600 mb-1">Current Bid</p>
              <p className="text-4xl font-bold text-indigo-700">
                {(auctionDetails.status === "SOLD"
                  ? auctionDetails.winningBid
                  : auctionDetails.currentBid ?? auctionDetails.startPrice ?? 0
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

            {/* Proxy Bidding Input */}
            {auctionDetails.status === "ACTIVE" &&
              !isSeller &&
              keycloak.authenticated && (
                <div className="space-y-2 pt-2">
                  <label
                    htmlFor="maxBidSelect"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Set Maximum Bid (Proxy):
                  </label>
                  <div className="flex items-center gap-2">
                    {/* --- DROPDOWN --- */}
                    <select
                      id="maxBidSelect"
                      value={selectedMaxBid}
                      onChange={(e) =>
                        setSelectedMaxBid(Number(e.target.value))
                      } // Store selection as number
                      disabled={isBidding || maxBidOptions.length === 0}
                      className="flex-1 px-3 py-2 border rounded text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-70 disabled:bg-gray-100"
                    >
                      {maxBidOptions.length === 0 && (
                        <option value="">Calculating...</option>
                      )}
                      {maxBidOptions.map((optionValue) => (
                        <option key={optionValue} value={optionValue}>
                          {optionValue.toLocaleString("vi-VN")} VNĐ
                        </option>
                      ))}
                    </select>
                    {/* --- END DROPDOWN --- */}

                    <button
                      onClick={handlePlaceBid} // Handler needs update
                      disabled={!canBid || isBidding || !selectedMaxBid} // Disable if no option selected
                      className="px-5 py-2 bg-green-600 hover:bg-green-700 text-white font-semibold rounded text-sm disabled:opacity-60"
                    >
                      {isBidding ? "Placing..." : "Place Max Bid"}
                    </button>
                  </div>
                  {bidError && (
                    <p className="text-xs text-red-500 mt-1">{bidError}</p>
                  )}
                  <p className="text-xs text-gray-500 mt-1">
                    Select the highest amount you're willing to pay. The system
                    will bid for you.
                    {/* Display next required bid still useful context */}
                    Next required bid is{" "}
                    {auctionDetails.nextBidAmount?.toLocaleString("vi-VN") ??
                      "N/A"}{" "}
                    VNĐ.
                  </p>
                </div>
              )}

            {/* Seller Cancel - Hammer Button */}
            {isSeller &&
              (auctionDetails.status === "SCHEDULED" ||
                auctionDetails.status === "ACTIVE") && (
                <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mt-4 pt-4 border-t">
                  {/* Cancel Button */}
                  <button
                    onClick={promptCancelAuction}
                    disabled={isCancelling || isHammering} // Disable if processing either action
                    className="flex-1 w-full sm:w-auto px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-semibold rounded text-sm disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    {isCancelling ? "Cancelling..." : "Cancel Auction"}
                  </button>
                  {/* End Early (Hammer) Button */}
                  <button
                    onClick={promptEndAuctionEarly}
                    // Disable if not ACTIVE, if no bids yet, or if already processing another action
                    disabled={
                      auctionDetails.status !== "ACTIVE" ||
                      !auctionDetails.highestBidderId ||
                      isCancelling ||
                      isHammering
                    }
                    className="flex-1 w-full sm:w-auto px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded text-sm disabled:opacity-60 disabled:cursor-not-allowed"
                    title={
                      !auctionDetails.highestBidderId
                        ? "Cannot end early without bids"
                        : auctionDetails.status !== "ACTIVE"
                        ? "Can only end early when active"
                        : "End auction now at current bid"
                    }
                  >
                    {isHammering ? "Hammering..." : "Hammer down"}
                  </button>
                </div>
              )}

            {/* Winner Display */}
            {auctionDetails.status === "SOLD" && (
              <div className="mt-4 p-4 bg-green-50 border border-green-300 rounded-lg text-center">
                <p className="font-semibold text-green-800 text-lg mb-1">
                  🏆 Auction Complete 🏆
                </p>
                <p className="text-sm text-green-700">
                  Winner:{" "}
                  <strong>
                    {auctionDetails.highestBidderUsernameSnapshot}
                  </strong>
                </p>
                <p className="text-sm text-green-700">
                  Winning Bid:{" "}
                  <strong>
                    {auctionDetails.winningBid.toLocaleString("vi-VN")} VNĐ
                  </strong>
                </p>
                {auctionDetails.winnerId === loggedInUserId && (
                  <p className="mt-2 font-bold text-green-600">
                    Congratulations, you won!
                  </p>
                )}
              </div>
            )}
            {/* Other ended statuses */}
            {(auctionDetails.status === "RESERVE_NOT_MET" ||
              auctionDetails.status === "CANCELLED") && (
              <div className="mt-4 p-4 bg-yellow-50 border border-yellow-300 rounded-lg text-center">
                <p className="font-semibold text-yellow-800 text-lg mb-1">
                  {auctionDetails.status === "CANCELLED"
                    ? "Auction Cancelled"
                    : "Auction Ended"}
                </p>
                <p className="text-sm text-yellow-700">
                  {auctionDetails.status === "RESERVE_NOT_MET"
                    ? "The reserve price was not met."
                    : "This auction was cancelled by the seller."}
                </p>
              </div>
            )}
          </div>{" "}
          {/* End Bidding Panel */}
          {/* Bid History Panel */}
          <div className="bg-white p-4 rounded shadow border overflow-hidden flex flex-col min-h-[200px] max-h-[300px]">
            <h4 className="font-semibold mb-2 text-sm border-b pb-1 flex-shrink-0">
              Bid History
            </h4>
            <div className="overflow-y-auto flex-grow">
              {bidHistory.length === 0 ? (
                <p className="text-xs text-gray-500 text-center py-4">
                  No bids placed yet.
                </p>
              ) : (
                <ul className="divide-y divide-dashed divide-gray-200 text-sm">
                  {bidHistory.map((bid, i) => {
                    const isYou =
                      loggedInUserId && bid.bidderId === loggedInUserId;
                    return (
                      <li
                        key={bid.id || i}
                        className="flex items-center justify-between py-2 px-1 hover:bg-gray-50"
                      >
                        <span
                          className={`w-1/3 truncate ${
                            isYou ? "text-blue-600 font-bold" : "text-gray-700"
                          }`}
                        >
                          {isYou ? "You" : bid.bidderUsernameSnapshot}
                          {bid.isAutoBid && (
                            <span title="Automatic Proxy Bid"> 🤖</span>
                          )}{" "}
                          {/* Indicate Auto Bid */}
                        </span>
                        <span className="w-1/3 text-center font-medium text-gray-800">
                          {bid.amount.toLocaleString("vi-VN")} VNĐ
                        </span>
                        <span className="w-1/3 text-right text-gray-500 text-xs">
                          {new Date(bid.bidTime).toLocaleTimeString([], {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>{" "}
          {/* End Bid History */}
        </div>{" "}
        {/* End Right Column */}
      </div>{" "}
      {/* End Top Grid */}
      {/* --- NEW: Comments Section (Full Width Below) --- */}
      <section className="mt-8 bg-white p-4 md:p-6 rounded shadow border">
        <h3 className="text-xl font-semibold mb-4 border-b pb-2">
          Comments & Questions
        </h3>
        {isLoadingComments && (
          <div className="text-center p-4">Loading comments...</div>
        )}
        {errorComments && (
          <div className="text-red-600 text-center p-4">{errorComments}</div>
        )}

        {/* Post Comment Form */}
        {keycloak.authenticated &&
          auctionDetails.status !== "CANCELLED" && ( // Allow comments unless cancelled?
            <div className="mb-6 border-b pb-4">
              <h4 className="text-md font-medium mb-2">
                Leave a Comment
                {replyingTo ? ` (Replying to ${replyingTo.username})` : ""}:
              </h4>
              {replyingTo && (
                <button
                  onClick={cancelReply}
                  className="text-xs text-red-500 hover:underline mb-2"
                >
                  (Cancel Reply)
                </button>
              )}
              <textarea
                value={commentInput}
                onChange={(e) => setCommentInput(e.target.value)}
                placeholder={
                  replyingTo
                    ? `Write your reply...`
                    : `Ask a question or leave a comment...`
                }
                rows="3"
                disabled={isPostingComment}
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
              />
              {commentError && (
                <p className="text-xs text-red-500 mt-1">{commentError}</p>
              )}
              <div className="text-right mt-2">
                <button
                  onClick={handlePostComment}
                  disabled={isPostingComment || !commentInput.trim()}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded text-sm disabled:opacity-60"
                >
                  {isPostingComment
                    ? "Posting..."
                    : replyingTo
                    ? "Post Reply"
                    : "Post Comment"}
                </button>
              </div>
            </div>
          )}
        {!keycloak.authenticated && auctionDetails.status !== "CANCELLED" && (
          <p className="text-center text-sm text-orange-600 mb-6">
            Please log in to leave a comment.
          </p>
        )}

        {/* Display Comments */}
        <div className="space-y-4">
          {!isLoadingComments && !errorComments && comments.length === 0 && (
            <p className="text-center text-gray-500 py-4">No comments yet.</p>
          )}
          {!isLoadingComments &&
            !errorComments &&
            comments.length > 0 &&
            comments.map((comment) => (
              <CommentDisplay
                key={comment.id}
                comment={comment}
                onReply={keycloak.authenticated ? handleSetReply : null}
              /> // Pass reply handler only if logged in
            ))}
        </div>
      </section>
      {/* --- End Comments Section --- */}
      {/* Cancel Confirmation Modal */}
      <ConfirmationModal
        isOpen={isCancelConfirmOpen}
        onClose={handleCloseCancelConfirm}
        onConfirm={handleConfirmCancel}
        title="Confirm Cancellation"
        message={`Are you sure you want to cancel this auction for "${
          auctionDetails?.productTitleSnapshot || "this item"
        }"? This action cannot be undone.`}
        confirmText="Yes, Cancel Auction"
        cancelText="No, Keep Auction"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={isCancelling}
        error={cancelError}
      />
      <ConfirmationModal
        isOpen={isHammerConfirmOpen}
        onClose={handleCloseHammerConfirm}
        onConfirm={handleConfirmEndAuctionEarly}
        title="Confirm End Auction Early"
        message={`Are you sure you want to end this auction now?\n\nThe current leading bid is ${auctionDetails?.currentBid?.toLocaleString('vi-VN') ?? 'N/A'} VNĐ by '${auctionDetails?.highestBidderUsernameSnapshot ?? 'N/A'}'.\n\nThis will sell the item immediately at the current bid.`}
        confirmText="Yes, End Auction Now"
        cancelText="No, Continue Auction"
        confirmButtonClass="bg-blue-600 hover:bg-blue-700" // Blue for hammer?
        isLoading={isHammering}
        error={hammerError}
      />
    </div> // End Page Container
  );
}

export default TimedAuctionDetailPage;
