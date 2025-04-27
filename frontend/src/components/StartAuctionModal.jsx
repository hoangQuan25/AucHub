// src/components/StartAuctionModal.jsx
import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient'; // Ensure path is correct

function StartAuctionModal({ isOpen, onClose, product, onStartAuctionSubmit }) {
  // State for auction configuration
  const [duration, setDuration] = useState(30);
  const [startPrice, setStartPrice] = useState('');
  const [reservePrice, setReservePrice] = useState('');
  const [startTimeOption, setStartTimeOption] = useState('NOW');
  const [scheduledStartTime, setScheduledStartTime] = useState('');

  // State for submission status and errors
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [validationError, setValidationError] = useState(''); // For form validation errors
  const [submitError, setSubmitError] = useState('');     // For API submission errors

  // Reset form when modal opens or product changes
  useEffect(() => {
    if (isOpen) {
      setDuration(30);
      setStartPrice('');
      setReservePrice('');
      setStartTimeOption('NOW');
      setScheduledStartTime('');
      setValidationError('');
      setSubmitError('');
      setIsSubmitting(false); // Ensure submitting state is reset
    }
  }, [isOpen, product]); // Reset if product changes while modal is open too

  // --- Handle Form Submission ---
  const handleSubmit = async (e) => {
    e.preventDefault();
    setValidationError('');
    setSubmitError('');

    // --- Frontend Validation ---
    const startPriceNum = parseFloat(startPrice);
    if (isNaN(startPriceNum) || startPriceNum < 0) {
      setValidationError('Start price must be a valid non-negative number.');
      return;
    }
    const reservePriceNum = reservePrice ? parseFloat(reservePrice) : null;
    if (reservePrice && (isNaN(reservePriceNum) || reservePriceNum < 0)) {
       setValidationError('Reserve price must be a valid non-negative number if provided.');
       return;
    }
    if (reservePriceNum !== null && reservePriceNum < startPriceNum) {
        setValidationError('Reserve price cannot be lower than the start price.');
        return;
    }
     if (startTimeOption === 'SCHEDULE' && !scheduledStartTime) {
        setValidationError('Please select a scheduled start time.');
        return;
    }
     // Basic check for past scheduled time (browser handles min, but good practice)
     if (startTimeOption === 'SCHEDULE' && new Date(scheduledStartTime) < new Date()) {
        setValidationError('Scheduled start time cannot be in the past.');
        return;
     }
    // --- End Validation ---

    // --- Prepare Backend DTO Payload ---
    // Adjust fields based on your exact CreateLiveAuctionDto on the backend
    const payload = {
      productId: product.id,
      durationMinutes: parseInt(duration, 10),
      startPrice: startPriceNum,
      reservePrice: reservePriceNum, // Send null if empty/not set
      // Send null for startTime if 'NOW', otherwise send the datetime-local string
      // Spring Boot should parse YYYY-MM-DDTHH:mm into LocalDateTime
      startTime: startTimeOption === 'NOW' ? null : scheduledStartTime,
    };

    // --- API Call ---
    setIsSubmitting(true);
    console.log("Submitting Auction Config:", payload);

    try {
      // Assuming apiClient is configured with base URL like http://localhost:8088/api/v1
      // Or adjust endpoint path if needed: '/api/v1/live-auctions'
      const response = await apiClient.post('/liveauctions/new-auction', payload);

      console.log("Auction created successfully:", response.data); // response.data should be LiveAuctionDto

      // Notify parent component of success, potentially passing back the created auction details
      if (onStartAuctionSubmit) {
        onStartAuctionSubmit(response.data); // Pass created auction DTO back
      }

      onClose(); // Close modal on success

    } catch (err) {
      console.error("Failed to start auction:", err);
      // Extract more specific error message from backend response if available
      const message = err.response?.data?.message || err.message || 'Failed to start auction. Please try again.';
      setSubmitError(message); // Show API error to the user
    } finally {
      setIsSubmitting(false); // Reset submitting state
    }
  };


  if (!isOpen || !product) return null;

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
        <h2 className="text-xl font-bold mb-4 border-b pb-2">Start Auction for "{product.title}"</h2>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Display Validation Errors */}
          {validationError && <p className="text-red-500 text-sm mb-3 bg-red-50 p-2 rounded border border-red-200">{validationError}</p>}
          {/* Display API Submission Errors */}
          {submitError && <p className="text-red-500 text-sm mb-3 bg-red-50 p-2 rounded border border-red-200">{submitError}</p>}

          {/* Auction Type (Still locked to LIVE) */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">Auction Type:</label>
            <input type="text" value="Live Auction" disabled className="w-full p-2 border rounded bg-gray-100 border-gray-300" />
            {/* Hidden field if needed, but backend likely doesn't need it */}
            {/* <input type="hidden" value="LIVE" /> */}
          </div>

          {/* Duration Select */}
          <div>
            <label htmlFor="duration" className="block mb-1 font-medium text-sm text-gray-700">Duration:</label>
            <select
              id="duration" value={duration} onChange={(e) => setDuration(e.target.value)} required
              disabled={isSubmitting}
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
            >
              {/* Options... */}
              <option value={15}>15 Minutes</option>
              <option value={30}>30 Minutes</option>
              <option value={45}>45 Minutes</option>
              <option value={60}>1 Hour</option>
               <option value={1}>1 Minute (Test)</option> {/* Add short duration for testing */}
            </select>
          </div>

          {/* Pricing Inputs */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label htmlFor="startPrice" className="block mb-1 font-medium text-sm text-gray-700">Start Price (VNĐ):</label>
              <input
                id="startPrice" type="number" min="0" step="any" // Allow decimals if needed, else use step="1"
                value={startPrice} onChange={(e) => setStartPrice(e.target.value)} required
                disabled={isSubmitting}
                placeholder="e.g., 50000"
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100" />
            </div>
             <div>
              <label htmlFor="reservePrice" className="block mb-1 font-medium text-sm text-gray-700">Reserve Price (Optional, VNĐ):</label>
              <input
                id="reservePrice" type="number" min="0" step="any"
                value={reservePrice} onChange={(e) => setReservePrice(e.target.value)}
                disabled={isSubmitting}
                placeholder="Leave blank for no reserve"
                className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100" />
            </div>
          </div>

          {/* Start Time Options */}
            <div>
                <label className="block mb-1 font-medium text-sm text-gray-700">Start Time:</label>
                <div className="flex items-center space-x-4">
                    <label className="flex items-center">
                        <input type="radio" name="startTimeOption" value="NOW" checked={startTimeOption === 'NOW'} onChange={(e) => setStartTimeOption(e.target.value)} disabled={isSubmitting} className="form-radio disabled:opacity-70"/>
                        <span className={`ml-2 ${isSubmitting ? 'text-gray-500' : ''}`}>Start Now</span>
                    </label>
                     <label className="flex items-center">
                        <input type="radio" name="startTimeOption" value="SCHEDULE" checked={startTimeOption === 'SCHEDULE'} onChange={(e) => setStartTimeOption(e.target.value)} disabled={isSubmitting} className="form-radio disabled:opacity-70"/>
                        <span className={`ml-2 ${isSubmitting ? 'text-gray-500' : ''}`}>Schedule for Later</span>
                    </label>
                </div>
                {startTimeOption === 'SCHEDULE' && (
                     <div className="mt-2">
                         <label htmlFor="scheduledStartTime" className="block mb-1 font-medium text-xs text-gray-600">Scheduled Start Time:</label>
                         <input
                             id="scheduledStartTime"
                             type="datetime-local"
                             value={scheduledStartTime}
                             onChange={(e) => setScheduledStartTime(e.target.value)}
                             required={startTimeOption === 'SCHEDULE'}
                             disabled={isSubmitting}
                             className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
                             min={new Date(Date.now() + 60000).toISOString().slice(0, 16)} // Prevent scheduling less than 1 min in future
                         />
                     </div>
                 )}
            </div>

          {/* Action Buttons */}
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
              {isSubmitting ? 'Starting...' : 'Start Live Auction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default StartAuctionModal;