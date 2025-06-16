// src/pages/TimedAuctionDetailPage.jsx
import React, { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient";
import CountdownTimer from "../components/CountdownTimer";
import ConfirmationModal from "../components/ConfirmationModal";
import CollapsibleSection from "../components/CollapsibleSection";
import AuctionRules from "../components/AuctionRules";
import {
  FaChevronLeft,
  FaChevronRight,
  FaCreditCard,
  FaUserCircle,
  FaEdit,
  FaTrash,
} from "react-icons/fa";

const getBidIncrement = (currentBidValue) => {
  const currentBid = Number(currentBidValue) || 0; // Ensure it's a number

  if (currentBid >= 50000000) return 2000000;
  if (currentBid >= 20000000) return 1000000;
  if (currentBid >= 10000000) return 500000;
  if (currentBid >= 5000000) return 200000;
  if (currentBid >= 3000000) return 100000;
  if (currentBid >= 1000000) return 50000;
  if (currentBid >= 300000) return 10000;
  if (currentBid >= 100000) return 5000;
  if (currentBid >= 50000) return 1000;
  return 500; // Default lowest increment
};

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

function CommentDisplay({ comment, onReply, currentUserId, onEdit, onDelete }) {
  const canReply = onReply !== null; // Check if reply function is provided

  const isOwner = currentUserId && currentUserId === comment.userId;

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
      {isOwner && (
        <div className="flex items-center gap-3">
          <button
            onClick={() => onEdit(comment)}
            className="text-gray-500 hover:text-blue-600"
            title="Edit comment"
          >
            <FaEdit />
          </button>
          <button
            onClick={() => onDelete(comment)}
            className="text-gray-500 hover:text-red-600"
            title="Delete comment"
          >
            <FaTrash />
          </button>
        </div>
      )}
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
            <CommentDisplay
              key={reply.id}
              comment={reply}
              onReply={onReply}
              currentUserId={currentUserId}
              onEdit={onEdit}
              onDelete={onDelete}
            /> // Allow replying to replies
          ))}
        </div>
      )}
    </div>
  );
}
// --- End Helper ---

function TimedAuctionDetailPage() {
  const { auctionId } = useParams();
  const prevAuctionIdRef = useRef();

  const { keycloak, initialized } = useKeycloak();
  const [isNavigatingToPayment, setIsNavigatingToPayment] = useState(false);
  const [navigationError, setNavigationError] = useState("");
  const navigate = useNavigate();

  const handleGoToPayment = () => {
    setIsNavigatingToPayment(true);
    setNavigationError("");
    navigate("/my-orders");
  };

  const [userProfile, setUserProfile] = useState(null);
  const [isProfileLoading, setIsProfileLoading] = useState(false); // To prevent multiple fetches or show loading

  const [isPaymentMethodModalOpen, setIsPaymentMethodModalOpen] =
    useState(false);

  const [auctionDetails, setAuctionDetails] = useState(null);
  const [bidHistory, setBidHistory] = useState([]); // Visible bid history
  const [comments, setComments] = useState([]); // Comments with nested replies
  const [isLoadingDetails, setIsLoadingDetails] = useState(true);
  const [isLoadingComments, setIsLoadingComments] = useState(true);
  const [errorDetails, setErrorDetails] = useState("");
  const [errorComments, setErrorComments] = useState("");

  const [isBidding, setIsBidding] = useState(false);
  const [maxBidOptions, setMaxBidOptions] = useState([]);
  const [isBidConfirmOpen, setIsBidConfirmOpen] = useState(false);
  const [selectedMaxBid, setSelectedMaxBid] = useState("");
  const [bidError, setBidError] = useState("");

  const [myMaxBid, setMyMaxBid] = useState(null);
  const [isLoadingMyMaxBid, setIsLoadingMyMaxBid] = useState(false);
  const [errorMyMaxBid, setErrorMyMaxBid] = useState("");

  const [commentInput, setCommentInput] = useState("");
  const [isPostingComment, setIsPostingComment] = useState(false);
  const [commentError, setCommentError] = useState("");
  const [replyingTo, setReplyingTo] = useState(null); // { id: parentId, username: parentUsername }

  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelError, setCancelError] = useState("");

  const [isHammerConfirmOpen, setIsHammerConfirmOpen] = useState(false);
  const [isHammering, setIsHammering] = useState(false); // Renamed for clarity
  const [hammerError, setHammerError] = useState("");

  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false);

  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // State để quản lý việc xóa bình luận
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [commentToDelete, setCommentToDelete] = useState(null);
  const [isDeletingComment, setIsDeletingComment] = useState(false);
  const [deleteCommentError, setDeleteCommentError] = useState("");

  // State để quản lý việc sửa bình luận
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [commentToEdit, setCommentToEdit] = useState(null);
  const [editedCommentText, setEditedCommentText] = useState("");
  const [isEditingComment, setIsEditingComment] = useState(false);
  const [editCommentError, setEditCommentError] = useState("");

  const detailsIntervalRef = useRef(null);
  const commentsIntervalRef = useRef(null);

  const loggedInUserId = initialized ? keycloak.subject : null;
  const isUserHighestBidder =
    loggedInUserId && auctionDetails?.highestBidderId === loggedInUserId;

  const canBid =
    initialized &&
    keycloak.authenticated &&
    auctionDetails?.status === "ACTIVE";

  const isSeller =
    loggedInUserId && auctionDetails?.sellerId === loggedInUserId;

  const images = auctionDetails?.productImageUrls || [];

  const isAuctionActive = auctionDetails?.status === "ACTIVE";
  const isAuctionScheduled = auctionDetails?.status === "SCHEDULED"; // <<< NEW
  const isAuctionSold = auctionDetails?.status === "SOLD";

  const isUserBanned =
    userProfile &&
    userProfile.banEndsAt &&
    new Date(userProfile.banEndsAt) > new Date();

  const fetchUserProfileForBidding = useCallback(async () => {
    if (
      initialized &&
      keycloak.authenticated &&
      !userProfile && // Check current userProfile state from component scope
      !isProfileLoading // Check current isProfileLoading state from component scope
    ) {
      setIsProfileLoading(true);
      console.log("Fetching user profile for bidding check (useCallback)...");
      try {
        await keycloak.updateToken(5); // Depends on keycloak.updateToken function
        const response = await apiClient.get("/users/me");
        setUserProfile(response.data); // Depends on setUserProfile setter
      } catch (err) {
        console.error("Failed to fetch user profile for bidding check:", err);
        setUserProfile(null);
      } finally {
        setIsProfileLoading(false); // Depends on setIsProfileLoading setter
      }
    } else if (!keycloak.authenticated) {
      if (userProfile !== null) {
        setUserProfile(null);
      }
    }
  }, [
    initialized,
    keycloak.authenticated, // Use the boolean value
    keycloak.updateToken,
    setUserProfile, // State setters are stable
    setIsProfileLoading, // State setters are stable
  ]);

  const fetchAuctionDetails = useCallback(async () => {
    const isInitialLoad = isLoadingDetails;

    if (!auctionId || !initialized) {
      if (isInitialLoad) setIsLoadingDetails(false);
      return;
    }

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
  }, [auctionId, initialized]); // Depends only on auctionId and auth readiness

  const fetchMyMaxBid = useCallback(async () => {
    if (!auctionId || !initialized || !keycloak.authenticated || isSeller) {
      setMyMaxBid(null); // Clear any previous max bid if conditions aren't met
      return;
    }
    setIsLoadingMyMaxBid(true);
    setErrorMyMaxBid("");
    try {
      console.log(`Workspaceing my max bid for auction ${auctionId}`);
      const response = await apiClient.get(
        `/timedauctions/${auctionId}/my-max-bid`
      );
      if (response.data && response.data.myMaxBid !== undefined) {
        // Check for the specific field in response
        setMyMaxBid(response.data.myMaxBid);
      } else {
        setMyMaxBid(null); // User might not have a max bid for this auction
      }
    } catch (err) {
      if (err.response && err.response.status === 404) {
        setMyMaxBid(null); // No max bid found for this user/auction
        console.log("No max bid found for current user on this auction.");
      } else {
        console.error("Failed to fetch my max bid:", err);
      }
    } finally {
      setIsLoadingMyMaxBid(false);
    }
  }, [auctionId, initialized, keycloak.authenticated, isSeller]); // isSeller is derived from loggedInUserId and auctionDetails.sellerId

  const fetchComments = useCallback(async () => {
    const isInitialLoad = isLoadingComments; // Capture initial load state

    if (!auctionId) {
      if (isInitialLoad) setIsLoadingComments(false);
      return;
    }

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
  }, [auctionId, initialized]); // Depends only on auctionId

  useEffect(() => {
    const prevAuctionId = prevAuctionIdRef.current;
    const auctionIdChanged = prevAuctionId !== auctionId;
    prevAuctionIdRef.current = auctionId;

    clearInterval(detailsIntervalRef.current);
    clearInterval(commentsIntervalRef.current);

    if (auctionId && initialized) {
      if (auctionIdChanged) {
        setIsLoadingDetails(true);
        setIsLoadingComments(true);
        setErrorDetails("");
        setCommentError("");
        setAuctionDetails(null);
        setComments([]);
      }

      fetchAuctionDetails();
      fetchComments();

      detailsIntervalRef.current = setInterval(fetchAuctionDetails, 15 * 1000);
      commentsIntervalRef.current = setInterval(fetchComments, 30 * 1000);

      if (keycloak.authenticated) {
        fetchUserProfileForBidding();
        fetchMyMaxBid();
      } else {
        if (userProfile !== null) setUserProfile(null);
        if (myMaxBid !== null) setMyMaxBid(null);
      }
    } else {
      setIsLoadingDetails(false);
      setIsLoadingComments(false);
      if (userProfile !== null) setUserProfile(null);
      if (myMaxBid !== null) setMyMaxBid(null);
      if (auctionDetails !== null) setAuctionDetails(null);
      if (comments.length > 0) setComments([]);
    }

    return () => {
      clearInterval(detailsIntervalRef.current);
      clearInterval(commentsIntervalRef.current);
    };
  }, [
    auctionId,
    initialized,
    keycloak.authenticated,
    fetchAuctionDetails,
    fetchComments,
    fetchUserProfileForBidding,
    fetchMyMaxBid,
  ]);

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
  const initiateBidProcess = () => {
    if (!initialized || !keycloak.authenticated) {
      setBidError("Please log in to place a bid.");
      return;
    }

    if (!userProfile) {
      // Check if userInfo is loaded
      setBidError("User details are still loading. Please try again shortly.");
      return;
    }

    // 1. Address Check (using userInfo)
    const { firstName, lastName, streetAddress, city, postalCode, country } =
      userProfile;
    const isAddressComplete =
      firstName &&
      firstName.trim() !== "" &&
      lastName &&
      lastName.trim() !== "" &&
      streetAddress &&
      streetAddress.trim() !== "" &&
      city &&
      city.trim() !== "" &&
      postalCode &&
      postalCode.trim() !== "" &&
      country &&
      country.trim() !== "";

    if (!isAddressComplete) {
      setIsAddressModalOpen(true); // Open the address modal
      return;
    }

    // 2. Payment Method Check (using userInfo.hasDefaultPaymentMethod from UserDto)
    if (!userProfile.hasDefaultPaymentMethod) {
      console.log(
        "User has no default payment method. Prompting to update profile."
      );
      setIsPaymentMethodModalOpen(true); // Open the payment method modal
      return;
    }

    // If all checks pass, open the bid confirmation modal
    if (!selectedMaxBid) {
      setBidError("Please select a bid amount.");
      return;
    }
    setBidError(""); // Clear any previous bid error
    setIsBidConfirmOpen(true);
  };

  const handlePlaceBid = async () => {
    if (
      !initialized ||
      !keycloak.authenticated ||
      !selectedMaxBid ||
      auctionDetails?.status !== "ACTIVE"
    ) {
      setBidError(
        "Cannot place bid at this time. Ensure you are logged in, an amount is selected, and auction is active."
      );
      return;
    }
    if (isBidding) return;

    setBidError("");
    const maxBidNum = Number(selectedMaxBid);

    if (isNaN(maxBidNum) || maxBidNum <= 0) {
      setBidError("Invalid bid amount selected.");
      return;
    }

    setIsBidding(true);
    const payload = { maxBid: maxBidNum };
    try {
      await apiClient.post(`/timedauctions/${auctionId}/bids`, payload);
      fetchAuctionDetails(); // Refresh auction details
      fetchMyMaxBid(); // Refresh user's max bid info
    } catch (err) {
      console.error("Failed to place max bid:", err);
      setBidError(
        err.response?.data?.message || err.message || "Failed to place bid."
      );
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

  const handleInitiateDelete = (comment) => {
    setCommentToDelete(comment);
    setDeleteCommentError("");
    setIsDeleteConfirmOpen(true);
  };

  const handleCloseDeleteConfirm = () => {
    setIsDeleteConfirmOpen(false);
    setCommentToDelete(null);
  };

  const handleConfirmDelete = async () => {
    if (!commentToDelete) return;
    setIsDeletingComment(true);
    setDeleteCommentError("");
    try {
      await apiClient.delete(
        `/timedauctions/${auctionId}/comments/${commentToDelete.id}`
      );
      handleCloseDeleteConfirm();
      fetchComments(); // Tải lại bình luận sau khi xóa
    } catch (err) {
      setDeleteCommentError(
        err.response?.data?.message || "Could not delete comment."
      );
    } finally {
      setIsDeletingComment(false);
    }
  };

  const handleInitiateEdit = (comment) => {
    setCommentToEdit(comment);
    setEditedCommentText(comment.commentText);
    setEditCommentError("");
    setIsEditModalOpen(true);
  };

  const handleCloseEditModal = () => {
    setIsEditModalOpen(false);
    setCommentToEdit(null);
    setEditedCommentText("");
  };

  const handleConfirmEdit = async () => {
    if (!commentToEdit || !editedCommentText.trim()) return;
    setIsEditingComment(true);
    setEditCommentError("");
    try {
      await apiClient.put(
        `/timedauctions/${auctionId}/comments/${commentToEdit.id}`,
        {
          commentText: editedCommentText.trim(),
        }
      );
      handleCloseEditModal();
      fetchComments(); // Tải lại bình luận sau khi sửa
    } catch (err) {
      setEditCommentError(
        err.response?.data?.message || "Could not save changes."
      );
    } finally {
      setIsEditingComment(false);
    }
  };

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

  return (
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
              {/* MODIFIED CONTENT FOR OVERVIEW SECTION */}
              <div className="px-4 pb-4 pt-3 space-y-4">
                {/* Seller Information - Prominent and Linked */}
                {auctionDetails.sellerUsernameSnapshot && (
                  <div className="mb-4 pb-4 border-b border-gray-200">
                    <span className="text-xs text-gray-500 block mb-0.5">
                      Item sold by:
                    </span>
                    <Link
                      to={`/seller/${auctionDetails.sellerUsernameSnapshot}`} // Link to seller profile
                      className="text-lg font-semibold text-indigo-700 hover:text-indigo-900 hover:underline flex items-center group transition-colors duration-150"
                      title={`View profile of ${auctionDetails.sellerUsernameSnapshot}`}
                    >
                      <FaUserCircle
                        className="mr-2 text-indigo-500 group-hover:text-indigo-700 transition-colors duration-150"
                        size="1.25em"
                      />
                      {auctionDetails.sellerUsernameSnapshot}
                    </Link>
                  </div>
                )}

                {/* Description */}
                {auctionDetails.productDescription && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500 mb-1">
                      Description
                    </h4>
                    <p className="text-sm text-gray-800 whitespace-pre-wrap leading-relaxed">
                      {auctionDetails.productDescription}
                    </p>
                  </div>
                )}

                {/* Condition */}
                {auctionDetails.productCondition && (
                  <div>
                    <h4 className="text-sm font-medium text-gray-500 mb-0.5">
                      Condition
                    </h4>
                    <p className="text-sm text-gray-800 capitalize">
                      {" "}
                      {/* Capitalize for better display e.g. LIKE_NEW -> Like new */}
                      {auctionDetails.productCondition
                        .replace(/_/g, " ")
                        .toLowerCase()}
                    </p>
                  </div>
                )}

                {/* Categories */}
                {auctionDetails.productCategories &&
                  auctionDetails.productCategories.length > 0 && (
                    <div>
                      <h4 className="text-sm font-medium text-gray-500 mb-1">
                        Categories
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {auctionDetails.productCategories.map((cat) => (
                          <Link
                            key={cat.id}
                            to={`/search?categories=${cat.id}`} // Link to search results for this category
                            className="bg-gray-100 text-gray-700 hover:bg-indigo-100 hover:text-indigo-700 text-xs font-medium px-3 py-1 rounded-full border border-gray-300 transition-colors duration-150"
                            title={`More in ${cat.name}`}
                          >
                            {cat.name}
                          </Link>
                        ))}
                      </div>
                    </div>
                  )}

                {/* Fallback if no specific details are present but seller is */}
                {!auctionDetails.productDescription &&
                  !auctionDetails.productCondition &&
                  (!auctionDetails.productCategories ||
                    auctionDetails.productCategories.length === 0) &&
                  auctionDetails.sellerUsernameSnapshot && (
                    <p className="text-sm text-gray-500 italic">
                      Detailed information will be provided by the seller.
                    </p>
                  )}
              </div>
            </CollapsibleSection>
            <AuctionRules /> {/* This remains the same */}
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
                className={`inline-flex items-center gap-2 text-sm font-semibold px-3 py-1 rounded-full
    ${
      auctionDetails.reserveMet
        ? "bg-green-100 text-green-700 border border-green-300"
        : auctionDetails.reservePrice
        ? "bg-orange-100 text-orange-700 border border-orange-300"
        : "bg-gray-100 text-gray-600 border border-gray-300"
    }
  `}
              >
                {auctionDetails.reserveMet ? (
                  <>
                    <span className="text-green-500 text-lg">✔</span>
                    Reserve Met
                  </>
                ) : auctionDetails.reservePrice ? (
                  <>
                    <span className="text-orange-400 text-lg">!</span>
                    Reserve Not Met
                  </>
                ) : (
                  <>
                    <span className="text-gray-400 text-lg">—</span>
                    No Reserve
                  </>
                )}
              </span>
              <div className="text-right">
                <div className="text-xs text-gray-500">
                  {
                    isAuctionActive
                      ? "Time Remaining"
                      : isAuctionScheduled
                      ? "Auction Starts At" // Label for scheduled
                      : `Auction ${auctionDetails.status
                          .toLowerCase()
                          .replace("_", " ")}` // e.g., "Auction cancelled"
                  }
                </div>

                {isAuctionActive ? (
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
                ) : isAuctionScheduled ? (
                  // For scheduled, show the actual start time, not a countdown to end time
                  <span className="font-semibold text-lg text-gray-700">
                    {new Date(auctionDetails.startTime).toLocaleTimeString([], {
                      day: "2-digit",
                      month: "short",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                ) : (
                  // For CANCELLED, SOLD, RESERVE_NOT_MET, ENDED etc.
                  <span
                    className={`font-semibold text-lg ${
                      auctionDetails.status === "SOLD"
                        ? "text-green-600"
                        : auctionDetails.status === "CANCELLED"
                        ? "text-red-600"
                        : "text-gray-700"
                    }`}
                  >
                    {auctionDetails.status.replace("_", " ")}
                  </span>
                )}
              </div>
            </div>

            {/* Current Bid Info */}
            {(isAuctionActive ||
              auctionDetails.status === "SOLD" ||
              auctionDetails.status === "RESERVE_NOT_MET" ||
              auctionDetails.status === "CANCELLED") &&
              !isAuctionScheduled && (
                <div className="text-center my-4">
                  <p className="text-sm text-gray-600 mb-1">Current Bid</p>
                  <p className="text-4xl font-bold text-indigo-700">
                    {(auctionDetails.status === "SOLD"
                      ? auctionDetails.winningBid
                      : auctionDetails.currentBid ??
                        auctionDetails.startPrice ??
                        0
                    ).toLocaleString("vi-VN")}{" "}
                    VNĐ
                  </p>
                  <p className="text-xs text-gray-500">
                    Leading:{" "}
                    {auctionDetails.highestBidderUsernameSnapshot ? (
                      isUserHighestBidder ? (
                        <span className="text-green-600 font-semibold">
                          You
                        </span>
                      ) : (
                        auctionDetails.highestBidderUsernameSnapshot
                      )
                    ) : (
                      "No bids yet"
                    )}
                  </p>
                </div>
              )}

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
                      onClick={initiateBidProcess}
                      disabled={
                        !canBid || isBidding || !selectedMaxBid || isUserBanned
                      }
                      className="px-5 py-2 bg-green-600 hover:bg-green-700 text-white font-semibold rounded text-sm disabled:opacity-60"
                    >
                      {isBidding ? "Placing..." : "Place Max Bid"}
                    </button>
                  </div>
                  {isUserBanned && (
                    <div className="flex items-center justify-center mt-4 p-3 bg-red-50 border border-red-300 rounded-lg shadow-sm">
                      <span className="mr-3 text-2xl text-red-400">⛔</span>
                      <div>
                        <p className="font-semibold text-red-700 text-sm mb-1">
                          You are banned from bidding
                        </p>
                        <p className="text-xs text-red-600">
                          {userProfile?.banEndsAt ? (
                            <>
                              Ban lifts:{" "}
                              <span className="font-bold">
                                {new Date(userProfile.banEndsAt).toLocaleString(
                                  "vi-VN"
                                )}
                              </span>
                            </>
                          ) : (
                            "Until further notice."
                          )}
                        </p>
                      </div>
                    </div>
                  )}
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
                  {myMaxBid !== null && !isSeller && keycloak.authenticated && (
                    <div className="mt-3 pt-3 border-t border-dashed">
                      <p className="text-sm font-medium text-gray-700">
                        Your Current Max Bid:
                        <span className="ml-2 font-bold text-purple-600">
                          {Number(myMaxBid).toLocaleString("vi-VN")} VNĐ
                        </span>
                      </p>
                      <p className="text-xs text-gray-500">
                        The system will bid up to this amount for you.
                      </p>
                    </div>
                  )}
                  {isLoadingMyMaxBid && !isSeller && (
                    <p className="text-xs text-gray-500 mt-2">
                      Loading your max bid...
                    </p>
                  )}
                </div>
              )}

            {/* Seller Cancel - Hammer Button */}

            {isAuctionScheduled && (
              <div className="text-center my-8 py-4">
                <p className="text-lg font-semibold text-gray-700 mb-2">
                  Auction Starts In:
                </p>
                <CountdownTimer
                  endTimeMillis={new Date(auctionDetails.startTime).getTime()}
                />
                <p className="text-sm text-gray-500 mt-3">
                  Starting Price:{" "}
                  <span className="font-semibold">
                    {(auctionDetails.startPrice ?? 0).toLocaleString("vi-VN")}{" "}
                    VNĐ
                  </span>
                </p>
              </div>
            )}

            {isSeller && (isAuctionActive || isAuctionScheduled) && (
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
                    isHammering ||
                    (auctionDetails.reservePrice && !auctionDetails.reserveMet)
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
                  <div className="mt-4 border-t pt-4">
                    <p className="font-bold text-green-600 mb-2">
                      Congratulations, you won!
                    </p>
                    <button
                      onClick={handleGoToPayment}
                      disabled={isNavigatingToPayment}
                      className="inline-flex items-center gap-2 py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded disabled:opacity-60"
                    >
                      <FaCreditCard />
                      {isNavigatingToPayment
                        ? "Loading Payment..."
                        : "Go to Payment"}
                    </button>
                    {navigationError && (
                      <p className="text-red-600 text-sm mt-2">
                        {navigationError}
                      </p>
                    )}
                  </div>
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
                currentUserId={loggedInUserId}
                onEdit={handleInitiateEdit}
                onDelete={handleInitiateDelete}
              />
            ))}
        </div>
      </section>
      {/* --- End Comments Section --- */}
      <ConfirmationModal
        isOpen={isPaymentMethodModalOpen}
        onClose={() => setIsPaymentMethodModalOpen(false)}
        onConfirm={() => {
          navigate("/profile"); // Navigate to user profile page
          setIsPaymentMethodModalOpen(false);
        }}
        title="Payment Method Required"
        message="To place a bid, you need a payment method saved to your profile. Please go to your profile to add or update your payment information."
        confirmText="Go to Profile"
        cancelText="Later" // Or "Close"
        confirmButtonClass="bg-indigo-600 hover:bg-indigo-700" // Style as needed
      />
      <ConfirmationModal
        isOpen={isAddressModalOpen}
        onClose={() => setIsAddressModalOpen(false)}
        onConfirm={() => {
          navigate("/profile"); // Ensure '/profile' is your correct user profile page route
          setIsAddressModalOpen(false);
        }}
        title="Shipping Address Required"
        message="To place a bid, your shipping address (including first name, last name, street, city, postal code, and country) must be complete. Please update your profile."
        confirmText="Go to Profile"
        cancelText="Cancel"
        confirmButtonClass="bg-blue-600 hover:bg-blue-700"
      />
      <ConfirmationModal
        isOpen={isBidConfirmOpen}
        onClose={() => setIsBidConfirmOpen(false)}
        onConfirm={async () => {
          setIsBidConfirmOpen(false);
          await handlePlaceBid();
        }}
        title="Confirm Your Bid"
        message={`Are you sure you want to place a maximum bid of ${Number(
          selectedMaxBid
        ).toLocaleString("vi-VN")} VNĐ?`}
        confirmText="Yes, Place Bid"
        cancelText="No, Cancel"
        confirmButtonClass="bg-green-600 hover:bg-green-700"
        isLoading={isBidding}
        error={bidError}
      />
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
        message={`Are you sure you want to end this auction now?\n\nThe current leading bid is ${
          auctionDetails?.currentBid?.toLocaleString("vi-VN") ?? "N/A"
        } VNĐ by '${
          auctionDetails?.highestBidderUsernameSnapshot ?? "N/A"
        }'.\n\nThis will sell the item immediately at the current bid.`}
        confirmText="Yes, End Auction Now"
        cancelText="No, Continue Auction"
        confirmButtonClass="bg-blue-600 hover:bg-blue-700" // Blue for hammer?
        isLoading={isHammering}
        error={hammerError}
      />
      <ConfirmationModal
        isOpen={isDeleteConfirmOpen}
        onClose={handleCloseDeleteConfirm}
        onConfirm={handleConfirmDelete}
        title="Confirm Deletion"
        message="Are you sure you want to delete this comment? All replies to this comment will also be removed. This action cannot be undone."
        confirmText="Yes, Delete"
        cancelText="No, Cancel"
        confirmButtonClass="bg-red-600 hover:bg-red-700"
        isLoading={isDeletingComment}
        error={deleteCommentError}
      />
      <ConfirmationModal
        isOpen={isEditModalOpen}
        onClose={handleCloseEditModal}
        onConfirm={handleConfirmEdit}
        title="Edit Your Comment"
        message={
          <div>
            <textarea
              value={editedCommentText}
              onChange={(e) => setEditedCommentText(e.target.value)}
              rows="4"
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        }
        confirmText="Save Changes"
        cancelText="Cancel"
        confirmButtonClass="bg-indigo-600 hover:bg-indigo-700"
        isLoading={isEditingComment}
        error={editCommentError}
      />
    </div> // End Page Container
  );
}

export default TimedAuctionDetailPage;
