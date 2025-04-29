// src/components/ConfirmationModal.jsx (Enhanced & Beautified)
import React from 'react';

function ConfirmationModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  isLoading = false,
  error = null,
  confirmText = "Yes / Confirm",
  cancelText = "No / Cancel",
  confirmButtonClass = "bg-green-500 hover:bg-green-600"
}) {
  if (!isOpen) return null;

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget && !isLoading) {
      onClose();
    }
  };

  const handleContentClick = (e) => {
    e.stopPropagation();
  };

  return (
    <div
      className="fixed inset-0 flex items-center justify-center z-50 bg-black/30 backdrop-blur-sm p-4"
      onClick={handleOverlayClick}
    >
      <div
        className="relative bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full transition-transform transform-gpu scale-100 animate-fadeIn"
        onClick={handleContentClick}
      >
        {/* Close Button Top Right */}
        <button
          onClick={onClose}
          disabled={isLoading}
          className="absolute top-3 right-3 text-gray-400 hover:text-gray-600 transition disabled:opacity-50"
        >
          ✕
        </button>

        {/* Title */}
        <h3 className="text-2xl font-bold text-gray-800 mb-4">{title}</h3>

        {/* Message */}
        <p className="text-gray-600 text-base whitespace-pre-wrap mb-6">
          {message}
        </p>

        {/* Error Box */}
        {error && (
          <div className="bg-red-100 border border-red-300 text-red-700 p-4 rounded mb-6 text-sm">
            <strong className="block font-semibold mb-1">Error:</strong> {error}
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end gap-3">
          <button
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-800 font-semibold text-sm transition disabled:opacity-50"
          >
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading}
            className={`px-4 py-2 rounded-lg text-white font-semibold text-sm transition disabled:opacity-50 ${
              isLoading
                ? 'bg-gray-400 cursor-wait'
                : confirmButtonClass
            }`}
          >
            {isLoading ? 'Processing...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmationModal;
