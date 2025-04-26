// src/components/ConfirmationModal.jsx (Enhanced)
import React from 'react';

function ConfirmationModal({
  isOpen,
  onClose,          // Function to call when closing (Cancel button or overlay click)
  onConfirm,        // Function to call when confirm button is clicked
  title,            // String for the modal title
  message,          // String for the main message/question
  isLoading = false,// OPTIONAL: Boolean to show loading state (disables buttons)
  error = null,     // OPTIONAL: String containing an error message to display
  confirmText = "Yes / Confirm", // OPTIONAL: Text for the confirm button
  cancelText = "No / Cancel",   // OPTIONAL: Text for the cancel button
  confirmButtonClass = "bg-green-500 hover:bg-green-600" // OPTIONAL: Tailwind classes for confirm button styling
}) {
  if (!isOpen) return null;

  // Prevent closing via overlay click when an action is loading
  const handleOverlayClick = (e) => {
      if (e.target === e.currentTarget && !isLoading) {
          onClose();
      }
  }
  // Prevent clicks inside the modal from closing it
  const handleContentClick = (e) => {
      e.stopPropagation();
  }

  return (
    // Modal Overlay
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50 p-4 transition-opacity duration-150"
      onClick={handleOverlayClick}
    >
      {/* Modal Content Box */}
      <div
        className="bg-white p-6 rounded-lg shadow-xl max-w-sm w-full transform transition-all duration-300 scale-100" // Added basic transition classes
        onClick={handleContentClick}
      >
        {/* Title */}
        <h3 className="text-xl font-bold mb-4">{title}</h3>

        {/* Message */}
        <p className="mb-6 text-sm text-gray-700 whitespace-pre-wrap">{message}</p>

        {/* Error Display Area */}
        {error && (
          <p className="mb-4 text-sm text-red-600 bg-red-50 p-3 rounded border border-red-200">
            <span className="font-semibold">Error:</span> {error}
          </p>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end space-x-3">
          <button
            onClick={onClose}
            disabled={isLoading} // Disable if loading
            className="px-4 py-2 bg-gray-200 hover:bg-gray-300 rounded text-gray-800 font-medium text-sm disabled:opacity-50"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading} // Disable if loading
            // Apply default or passed-in button classes + disabled styles
            className={`px-4 py-2 rounded text-white font-medium text-sm disabled:opacity-50 ${isLoading ? 'bg-gray-400 cursor-wait' : confirmButtonClass}`}
          >
            {/* Show different text when loading */}
            {isLoading ? 'Processing...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmationModal;