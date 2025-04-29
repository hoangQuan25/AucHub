// src/components/StartAuctionModal.jsx
import React, { useState, useEffect } from "react";
import ConfirmationModal from "./ConfirmationModal";
import apiClient from "../api/apiClient"; // Ensure path is correct

function StartAuctionModal({ isOpen, onClose, product, onStartAuctionSubmit }) {
  // State for auction configuration
  const [duration, setDuration] = useState(30);
  const [startPrice, setStartPrice] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [startTimeOption, setStartTimeOption] = useState("NOW");
  const [scheduledStartTime, setScheduledStartTime] = useState("");

  // State for submission status and errors
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isConfirmingSubmit, setIsConfirmingSubmit] = useState(false); // Is the final confirmation modal open?
  const [auctionDataToConfirm, setAuctionDataToConfirm] = useState(null);
  const [validationError, setValidationError] = useState(""); // For form validation errors
  const [submitError, setSubmitError] = useState(""); // For API submission errors

  const [fastFinish, setFastFinish] = useState(false);

  // Reset form when modal opens or product changes
  useEffect(() => {
    if (isOpen) {
      setDuration(30);
      setStartPrice("");
      setReservePrice("");
      setStartTimeOption("NOW");
      setScheduledStartTime("");
      setValidationError("");
      setSubmitError("");
      setIsSubmitting(false); // Ensure submitting state is reset
    }
  }, [isOpen, product]); // Reset if product changes while modal is open too

  // --- Handle Form Submission ---
  // --- MODIFIED Handle Form Submission ---
  const handleSubmit = (e) => { // No longer async directly
    e.preventDefault();
    setValidationError("");
    // Don't clear submitError here - it might be relevant if shown in confirm modal
    // setSubmitError("");

    // --- Frontend Validation (Keep this block as is) ---
    const startPriceNum = parseFloat(startPrice);
    if (isNaN(startPriceNum) || startPriceNum < 0) {
      setValidationError("Start price must be a valid non-negative number.");
      return;
    }
    const reservePriceNum = reservePrice ? parseFloat(reservePrice) : null;
    if (reservePrice && (isNaN(reservePriceNum) || reservePriceNum < 0)) {
      setValidationError("Reserve price must be a valid non-negative number if provided.");
      return;
    }
    if (reservePriceNum !== null && reservePriceNum < startPriceNum) {
      setValidationError("Reserve price cannot be lower than the start price.");
      return;
    }
    if (startTimeOption === "SCHEDULE" && !scheduledStartTime) {
      setValidationError("Please select a scheduled start time.");
      return;
    }
    if (
      startTimeOption === "SCHEDULE" &&
      new Date(scheduledStartTime) < new Date()
    ) {
      setValidationError("Scheduled start time cannot be in the past.");
      return;
    }
    // --- End Validation ---

    // --- Prepare Data for Confirmation & Backend ---
    const dataForConfirmation = {
      productId: product.id,
      durationMinutes: parseInt(duration, 10),
      startPrice: startPriceNum,
      reservePrice: reservePriceNum,
      startTime: startTimeOption === "NOW" ? null : scheduledStartTime,
      // Add formatted versions for display in the confirmation message
      formatted: {
          duration: `${duration} Minutes` + (duration == 1 ? " (Test)" : duration == 60 ? " (1 Hour)" : ""),
          startPrice: startPriceNum.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' }), // Format for display
          reservePrice: reservePriceNum !== null ? reservePriceNum.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' }) : "None", // Format or "None"
          startTime: startTimeOption === "NOW" ? "Start Immediately" : `Scheduled: ${new Date(scheduledStartTime).toLocaleString('en-GB')}` // Format date/time nicely
      }
    };

    // --- Store data and open confirmation modal ---
    setAuctionDataToConfirm(dataForConfirmation);
    setIsConfirmingSubmit(true); // Open the confirmation modal

    // NOTE: We DO NOT call the API here anymore.
  };

  // --- NEW Function to execute the API call after confirmation ---
  const executeStartAuction = async () => {
    if (!auctionDataToConfirm) return; // Safety check

    setIsSubmitting(true); // Set loading state ON
    setSubmitError(""); // Clear previous submission errors before trying again

    // Prepare payload from the confirmed data (exclude the 'formatted' part)
    const payload = {
        productId: auctionDataToConfirm.productId,
        durationMinutes: auctionDataToConfirm.durationMinutes,
        startPrice: auctionDataToConfirm.startPrice,
        reservePrice: auctionDataToConfirm.reservePrice,
        startTime: auctionDataToConfirm.startTime,
    };

    console.log("Submitting Auction Config (Confirmed):", payload);

    try {
      const response = await apiClient.post("/liveauctions/new-auction", payload);
      console.log("Auction created successfully:", response.data);

      if (onStartAuctionSubmit) {
        onStartAuctionSubmit(response.data); // Pass created DTO back
      }
      onClose(); // Close the main StartAuctionModal on success

    } catch (err) {
      console.error("Failed to start auction:", err);
      const message = err.response?.data?.message || err.message || "Failed to start auction. Please try again.";
      setSubmitError(message); // Show API error
      // Keep the confirmation modal open (or let it close?) - keeping it open allows retry
      // If we kept it open, the error will show inside the ConfirmationModal via its `error` prop.
    } finally {
      setIsSubmitting(false); // Set loading state OFF
      // Don't clear auctionDataToConfirm here, it might be needed if submit fails and user retries confirm
    }
  };

  // --- NEW Handlers for the final confirmation modal ---
  const handleConfirmFinalSubmit = () => {
    // User clicked "Confirm & Start" in the confirmation modal
    // The API call will be made now.
    executeStartAuction();
    // Keep the confirmation modal open while `isSubmitting` is true.
    // `ConfirmationModal`'s `isLoading` prop will handle disabling buttons.
  };

  const handleCloseFinalConfirmModal = () => {
    // User clicked "Edit Details" or closed the confirmation modal
    setIsConfirmingSubmit(false);
    // We keep auctionDataToConfirm so if they re-submit the main form without changes, it's still there.
    // Alternatively, clear it if you want them to always re-trigger validation:
    // setAuctionDataToConfirm(null);
  };

  if (!isOpen || !product) return null;

  const confirmationMessage = auctionDataToConfirm ? `Please review the auction details:\n
Product:         ${product.title}
Duration:        ${auctionDataToConfirm.formatted.duration}
Start Price:     ${auctionDataToConfirm.formatted.startPrice}
Reserve Price:   ${auctionDataToConfirm.formatted.reservePrice}
Start Time:      ${auctionDataToConfirm.formatted.startTime}` : "";

  return (
    // --- Modal Structure (Tailwind CSS) ---
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isSubmitting} // Disable close button during submission? Optional.
          className="absolute top-3 right-4 text-gray-600 hover:text-gray-900 text-2xl font-bold disabled:opacity-50"
        >
          &times;
        </button>
        {/* Title */}
        <h2 className="text-xl font-bold mb-4 border-b pb-2">
          Start Auction for "{product.title}"
        </h2>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Display Validation Errors */}
          {validationError && (
            <p className="text-red-500 text-sm mb-3 bg-red-50 p-2 rounded border border-red-200">
              {validationError}
            </p>
          )}
          {/* Display API Submission Errors */}
          {submitError && (
            <p className="text-red-500 text-sm mb-3 bg-red-50 p-2 rounded border border-red-200">
              {submitError}
            </p>
          )}

          {/* Auction Type (Still locked to LIVE) */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">
              Auction Type:
            </label>
            <input
              type="text"
              value="Live Auction"
              disabled
              className="w-full p-2 border rounded bg-gray-100 border-gray-300"
            />
            {/* Hidden field if needed, but backend likely doesn't need it */}
            {/* <input type="hidden" value="LIVE" /> */}
          </div>

          {/* Duration Select */}
          <div>
            <label
              htmlFor="duration"
              className="block mb-1 font-medium text-sm text-gray-700"
            >
              Duration:
            </label>
            <select
              id="duration"
              value={duration}
              onChange={(e) => setDuration(e.target.value)}
              required
              disabled={isSubmitting}
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
            >
              {/* Options... */}
              <option value={15}>15 Minutes</option>
              <option value={30}>30 Minutes</option>
              <option value={45}>45 Minutes</option>
              <option value={60}>1 Hour</option>
              <option value={1}>1 Minute (Test)</option>{" "}
              {/* Add short duration for testing */}
            </select>
          </div>

          {/* Pricing Inputs */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label
                htmlFor="startPrice"
                className="block mb-1 font-medium text-sm text-gray-700"
              >
                Start Price (VNĐ):
              </label>
              <input
                id="startPrice"
                type="number"
                min="0"
                step="any" // Allow decimals if needed, else use step="1"
                value={startPrice}
                onChange={(e) => setStartPrice(e.target.value)}
                required
                disabled={isSubmitting}
                placeholder="e.g., 50000"
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
              />
            </div>
            <div>
              <label
                htmlFor="reservePrice"
                className="block mb-1 font-medium text-sm text-gray-700"
              >
                Reserve Price (Optional, VNĐ):
              </label>
              <input
                id="reservePrice"
                type="number"
                min="0"
                step="any"
                value={reservePrice}
                onChange={(e) => setReservePrice(e.target.value)}
                disabled={isSubmitting}
                placeholder="Leave blank for no reserve"
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
              />
            </div>
          </div>

          {/* Start Time Options */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">
              Start Time:
            </label>
            <div className="flex items-center space-x-4">
              <label className="flex items-center">
                <input
                  type="radio"
                  name="startTimeOption"
                  value="NOW"
                  checked={startTimeOption === "NOW"}
                  onChange={(e) => setStartTimeOption(e.target.value)}
                  disabled={isSubmitting}
                  className="form-radio disabled:opacity-70"
                />
                <span className={`ml-2 ${isSubmitting ? "text-gray-500" : ""}`}>
                  Start Now
                </span>
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  name="startTimeOption"
                  value="SCHEDULE"
                  checked={startTimeOption === "SCHEDULE"}
                  onChange={(e) => setStartTimeOption(e.target.value)}
                  disabled={isSubmitting}
                  className="form-radio disabled:opacity-70"
                />
                <span className={`ml-2 ${isSubmitting ? "text-gray-500" : ""}`}>
                  Schedule for Later
                </span>
              </label>
            </div>
            {startTimeOption === "SCHEDULE" && (
              <div className="mt-2">
                <label
                  htmlFor="scheduledStartTime"
                  className="block mb-1 font-medium text-xs text-gray-600"
                >
                  Scheduled Start Time:
                </label>
                <input
                  id="scheduledStartTime"
                  type="datetime-local"
                  value={scheduledStartTime}
                  onChange={(e) => setScheduledStartTime(e.target.value)}
                  required={startTimeOption === "SCHEDULE"}
                  disabled={isSubmitting}
                  className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
                  min={new Date(Date.now() + 60000).toISOString().slice(0, 16)} // Prevent scheduling less than 1 min in future
                />
              </div>
            )}
            <label className="flex items-center gap-2 mt-4">
              <input
                type="checkbox"
                checked={fastFinish}
                onChange={() => setFastFinish(!fastFinish)}
                className="form-checkbox h-4 w-4 text-indigo-600"
              />
              <span className="text-sm">
                Accelerate closing to&nbsp;
                <strong>2 min</strong> once reserve is met
              </span>
            </label>
          </div>

          {/* Action Buttons (Submit button now triggers confirmation) */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting} // Disable if final API call is in progress
              className="inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition ease-in-out duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              type="submit" // Still type="submit" to trigger form's onSubmit
              disabled={isSubmitting} // Disable if final API call is in progress
              className="inline-flex justify-center rounded-md border border-transparent bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2 transition ease-in-out duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {/* Text can indicate review step now */}
              {isSubmitting ? "Starting..." : "Review & Start Auction"}
            </button>
          </div>
        </form>
      </div>
      {/* --- NEW: Render the Final Confirmation Modal --- */}
      {auctionDataToConfirm && (
        <ConfirmationModal
          isOpen={isConfirmingSubmit}
          onClose={handleCloseFinalConfirmModal} // Go back to editing
          onConfirm={handleConfirmFinalSubmit}     // Trigger the actual API call
          title="Confirm Auction Details"
          message={confirmationMessage} // Use the dynamically generated message
          confirmText="Confirm & Start Auction" // Clear final action text
          cancelText="Edit Details"          // Clear "go back" action text
          confirmButtonClass="bg-green-600 hover:bg-green-700" // Green for go
          isLoading={isSubmitting} // Show loading state during the API call
          error={submitError}      // Display API submission errors directly here
        />
      )}
      {/* --- END NEW --- */}
    </div>
  );
}

export default StartAuctionModal;
