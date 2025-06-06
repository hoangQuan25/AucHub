// src/components/StartAuctionModal.jsx
import React, { useState, useEffect } from "react";
import ConfirmationModal from "./ConfirmationModal";
import apiClient from "../api/apiClient"; // Ensure path is correct

// Helper function to get minimum date string for input fields
const getMinDateTimeLocal = (offsetMillis = 0) => {
  const now = new Date();
  const futureDate = new Date(now.getTime() + offsetMillis);
  // Convert to YYYY-MM-DDTHH:mm format required by datetime-local
  const year = futureDate.getFullYear();
  const month = (futureDate.getMonth() + 1).toString().padStart(2, "0");
  const day = futureDate.getDate().toString().padStart(2, "0");
  const hours = futureDate.getHours().toString().padStart(2, "0");
  const minutes = futureDate.getMinutes().toString().padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

function StartAuctionModal({ isOpen, onClose, product, onStartAuctionSubmit }) {
  const [auctionType, setAuctionType] = useState("LIVE");

  // State for auction configuration
  const [durationMinutes, setDurationMinutes] = useState(30); // Default for LIVE

  // --- NEW: State for Timed Auction End Time ---
  const [timedAuctionEndTime, setTimedAuctionEndTime] = useState(""); // Store YYYY-MM-DDTHH:mm string

  const [startPrice, setStartPrice] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [startTimeOption, setStartTimeOption] = useState("NOW");
  const [scheduledStartTime, setScheduledStartTime] = useState("");

  // State for submission status and errors
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isConfirmingSubmit, setIsConfirmingSubmit] = useState(false);
  const [auctionDataToConfirm, setAuctionDataToConfirm] = useState(null);
  const [validationError, setValidationError] = useState("");
  const [submitError, setSubmitError] = useState("");

  // Calculate minimum allowed end time for timed auctions (+24 hours)
  const minTimedEndTime = getMinDateTimeLocal(24 * 60 * 60 * 1000); // 24 hours in ms
  // Calculate minimum allowed start time for scheduled auctions (+ buffer)
  const minScheduledStartTime = getMinDateTimeLocal(5 * 60 * 1000); // 1 min in ms buffer

  // Reset form when modal opens or product changes
  useEffect(() => {
    if (isOpen) {
      // Reset to defaults
      setAuctionType("LIVE");
      setDurationMinutes(30);
      // Set default timed end time (e.g., 7 days from now) when modal opens
      setTimedAuctionEndTime(getMinDateTimeLocal(7 * 24 * 60 * 60 * 1000)); // Default 7 days
      setStartPrice("");
      setReservePrice("");
      setStartTimeOption("NOW");
      setScheduledStartTime("");
      setValidationError("");
      setSubmitError("");
      setIsSubmitting(false);
      setIsConfirmingSubmit(false);
      setAuctionDataToConfirm(null);
    }
  }, [isOpen, product]);

  // --- Handle Form Submission ---
  const handleSubmit = (e) => {
    e.preventDefault();
    setValidationError("");

    // Basic price validation
    const startPriceNum = parseFloat(startPrice);
    if (isNaN(startPriceNum) || startPriceNum < 0) {
      setValidationError("Start price must be a valid non-negative number.");
      return;
    }
    const reservePriceNum = reservePrice ? parseFloat(reservePrice) : null;
    if (reservePrice && (isNaN(reservePriceNum) || reservePriceNum < 0)) {
      setValidationError(
        "Reserve price must be a valid non-negative number if provided."
      );
      return;
    }
    if (reservePriceNum !== null && reservePriceNum < startPriceNum) {
      setValidationError("Reserve price cannot be lower than the start price.");
      return;
    }

    // Start time validation
    let effectiveStartTime = null; // Will hold the JS Date object or null
    if (startTimeOption === "SCHEDULE") {
      if (!scheduledStartTime) {
        setValidationError("Please select a scheduled start time.");
        return;
      }
      effectiveStartTime = new Date(scheduledStartTime);
      const now = new Date();
      // Check against min allowed start time (now + buffer)
      if (
        effectiveStartTime.getTime() < new Date(minScheduledStartTime).getTime()
      ) {
        setValidationError(
          "Scheduled start time must be at least a few minutes in the future."
        );
        return;
      }
    } else {
      effectiveStartTime = new Date(); // If starting now, use current time for comparison
    }

    // --- Type-specific Validation & Data Prep ---
    let finalEndTime = null; // Will hold the ISO string for backend or derived value
    let formattedDurationOrEndTime = "";

    if (auctionType === "LIVE") {
      const durationMins = parseInt(durationMinutes, 10);
      if (isNaN(durationMins) || durationMins <= 0) {
        setValidationError(
          "Please select a valid duration for the live auction."
        );
        return;
      }
      // For LIVE, backend expects durationMinutes, not endTime
      // We don't need to calculate finalEndTime here for the payload
      formattedDurationOrEndTime = `Duration: ${durationMinutes} Minute${
        durationMinutes == 1 ? "" : "s"
      }`;
    } else {
      // TIMED auction
      if (!timedAuctionEndTime) {
        setValidationError(
          "Please select an end date and time for the timed auction."
        );
        return;
      }
      const selectedEndDate = new Date(timedAuctionEndTime);
      const minEndDate = new Date(minTimedEndTime); // Min end date (now + 24h)

      // Use the *effective* start time (now or scheduled) for the 24h check relative to start
      const minEndDateFromStart = new Date(
        effectiveStartTime.getTime() + 24 * 60 * 60 * 1000
      );

      if (selectedEndDate.getTime() < minEndDateFromStart.getTime()) {
        setValidationError(
          "Timed auction end time must be at least 24 hours after the start time."
        );
        return;
      }

      finalEndTime = selectedEndDate.toISOString(); // Send ISO string to backend
      formattedDurationOrEndTime = `Ends: ${selectedEndDate.toLocaleString(
        "en-GB"
      )}`;
    }
    // --- End Type-specific Validation ---

    // --- Prepare Data for Confirmation & Backend ---
    const dataForConfirmation = {
      // Core data
      productId: product.id,
      startPrice: startPriceNum,
      reservePrice: reservePriceNum,
      startTime: startTimeOption === "NOW" ? null : scheduledStartTime,
      // Type & EndTime/Duration
      auctionType: auctionType,
      // Send EITHER endTime OR durationMinutes, backend needs adaptation
      endTime: auctionType === "TIMED" ? finalEndTime : null, // Only for TIMED
      durationMinutes:
        auctionType === "LIVE" ? parseInt(durationMinutes, 10) : null, // Only for LIVE
      originalOrderId: product.originalOrderId,

      // Formatted values for display
      formatted: {
        auctionType: auctionType === "LIVE" ? "Live Auction" : "Timed Auction",
        durationOrEndTime: formattedDurationOrEndTime, // Use the combined formatted string
        startPrice: startPriceNum.toLocaleString("vi-VN", {
          style: "currency",
          currency: "VND",
        }),
        reservePrice:
          reservePriceNum !== null
            ? reservePriceNum.toLocaleString("vi-VN", {
                style: "currency",
                currency: "VND",
              })
            : "None",
        startTime:
          startTimeOption === "NOW"
            ? "Start Immediately"
            : `Scheduled: ${new Date(scheduledStartTime).toLocaleString(
                "en-GB"
              )}`,
      },
    };

    setAuctionDataToConfirm(dataForConfirmation);
    setIsConfirmingSubmit(true);
  };

  // --- Execute API call after confirmation ---
  const executeStartAuction = async () => {
    if (!auctionDataToConfirm) return;

    setIsSubmitting(true);
    setSubmitError("");

    let apiUrl = "";
    let payload = {};
    let basePayload = { // Define base payload properties
        productId: auctionDataToConfirm.productId,
        startPrice: auctionDataToConfirm.startPrice,
        reservePrice: auctionDataToConfirm.reservePrice,
        ...(auctionDataToConfirm.startTime && { startTime: `${auctionDataToConfirm.startTime}:00` })
    };

    // Conditionally add originalOrderId if it exists
    if (auctionDataToConfirm.originalOrderId) {
        basePayload.originalOrderId = auctionDataToConfirm.originalOrderId;
    }

    if (auctionDataToConfirm.auctionType === "LIVE") {
      apiUrl = "/liveauctions/new-auction"; // Live auction endpoint
      payload = {
        ...basePayload, // Spread base payload
        durationMinutes: auctionDataToConfirm.durationMinutes,
      };
    } else { // TIMED auction
      apiUrl = "/timedauctions/timed-auctions"; // Timed auction endpoint
      payload = {
        ...basePayload, // Spread base payload
        endTime: auctionDataToConfirm.endTime,
      };
    }

    console.log(
      `Submitting ${auctionDataToConfirm.auctionType} Auction Config (Confirmed):`,
      payload
    );
    console.log(`API URL: ${apiUrl}`);

    try {
      const response = await apiClient.post(apiUrl, payload);
      console.log("Auction created successfully:", response.data);

      if (onStartAuctionSubmit) {
        onStartAuctionSubmit(response.data);
      }
      onClose();
    } catch (err) {
      console.error("Failed to start auction:", err);
      const message =
        err.response?.data?.message ||
        err.message ||
        "Failed to start auction. Please try again.";
      setSubmitError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Confirmation modal handlers (no change needed)
  const handleConfirmFinalSubmit = () => executeStartAuction();
  const handleCloseFinalConfirmModal = () => setIsConfirmingSubmit(false);

  if (!isOpen || !product) return null;

  // --- Generate Confirmation Message ---
  const confirmationMessage = auctionDataToConfirm
    ? `Please review the auction details:\n
Product:         ${product.title}
Type:            ${auctionDataToConfirm.formatted.auctionType}
${auctionDataToConfirm.formatted.durationOrEndTime}
Start Price:     ${auctionDataToConfirm.formatted.startPrice}
Reserve Price:   ${auctionDataToConfirm.formatted.reservePrice}
Start Time:      ${auctionDataToConfirm.formatted.startTime}`
    : "";

  // --- Render Component ---
  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto relative">
        {/* Close Button */}
        <button
          onClick={onClose}
          disabled={isSubmitting}
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
          {validationError && (
            <p className="text-red-500 text-sm mb-3 bg-red-50 p-2 rounded border border-red-200">
              {validationError}
            </p>
          )}

          {/* Auction Type Selection (No change) */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">
              Auction Type:
            </label>
            <div className="flex items-center space-x-4">
              <label className="flex items-center">
                <input
                  type="radio"
                  name="auctionType"
                  value="LIVE"
                  checked={auctionType === "LIVE"}
                  onChange={(e) => setAuctionType(e.target.value)}
                  disabled={isSubmitting}
                  className="form-radio disabled:opacity-70"
                />
                <span className={`ml-2 ${isSubmitting ? "text-gray-500" : ""}`}>
                  Live Auction
                </span>
              </label>
              <label className="flex items-center">
                <input
                  type="radio"
                  name="auctionType"
                  value="TIMED"
                  checked={auctionType === "TIMED"}
                  onChange={(e) => setAuctionType(e.target.value)}
                  disabled={isSubmitting}
                  className="form-radio disabled:opacity-70"
                />
                <span className={`ml-2 ${isSubmitting ? "text-gray-500" : ""}`}>
                  Timed Auction
                </span>
              </label>
            </div>
          </div>

          {/* --- Conditional Duration / End Time Input --- */}
          {auctionType === "LIVE" && (
            <div>
              <label
                htmlFor="durationMinutes"
                className="block mb-1 font-medium text-sm text-gray-700"
              >
                Duration:
              </label>
              <select
                id="durationMinutes"
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(e.target.value)}
                required
                disabled={isSubmitting}
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
              >
                <option value={15}>15 Minutes</option>
                <option value={30}>30 Minutes</option>
                <option value={45}>45 Minutes</option>
                <option value={60}>1 Hour</option>
                <option value={5}>5 Minutes (Test)</option>
                <option value={1}>1 Minute (Test)</option>
              </select>
            </div>
          )}
          {auctionType === "TIMED" && (
            <div>
              <label
                htmlFor="timedAuctionEndTime"
                className="block mb-1 font-medium text-sm text-gray-700"
              >
                Auction End Time:
              </label>
              <input
                id="timedAuctionEndTime"
                type="datetime-local"
                value={timedAuctionEndTime}
                onChange={(e) => setTimedAuctionEndTime(e.target.value)}
                required
                disabled={isSubmitting}
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
                min={minTimedEndTime} // Set minimum end time (+24h from now)
              />
              <p className="text-xs text-gray-500 mt-1">
                Must be at least 24 hours after the start time.
              </p>
            </div>
          )}

          {/* Pricing Inputs (No change) */}
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
                step="any"
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

          {/* Start Time Options (Added min attribute) */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">
              Start Time:
            </label>
            <div className="flex items-center space-x-4">
              {/* Radio buttons... */}
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
                  min={minScheduledStartTime} // Prevent scheduling in the past/too soon
                />
              </div>
            )}
          </div>

          {/* Action Buttons (No change) */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200 mt-6">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="inline-flex justify-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition ease-in-out duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="inline-flex justify-center rounded-md border border-transparent bg-green-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-offset-2 transition ease-in-out duration-150 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? "Starting..." : "Review & Start Auction"}
            </button>
          </div>
        </form>
      </div>

      {/* Final Confirmation Modal (No change) */}
      {auctionDataToConfirm && (
        <ConfirmationModal
          isOpen={isConfirmingSubmit}
          onClose={handleCloseFinalConfirmModal}
          onConfirm={handleConfirmFinalSubmit}
          title="Confirm Auction Details"
          message={confirmationMessage} // Updated message format handles type/endTime
          confirmText="Confirm & Start Auction"
          cancelText="Edit Details"
          confirmButtonClass="bg-green-600 hover:bg-green-700"
          isLoading={isSubmitting}
          error={submitError}
        />
      )}
    </div>
  );
}

export default StartAuctionModal;
