// src/components/delivery/MarkAsDeliveredFormModal.jsx
import React, { useState, useEffect } from 'react';
import { FaBoxTissue, FaCheckCircle, FaTimes } from 'react-icons/fa'; // FaBoxTissue is a placeholder, consider FaBoxOpen or similar

function MarkAsDeliveredFormModal({
  isOpen,
  onClose,
  onSubmit, // (data: { notes: string }) => void
  orderId,
  deliveryId,
  deliveryDetails, // Pass current delivery details for context display
  isLoading,
  apiError,
}) {
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (isOpen) {
      setNotes(''); // Reset notes when modal opens
    }
  }, [isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({ notes });
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-60 transition-opacity duration-300 ease-in-out"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-xl shadow-2xl w-full max-w-lg transform transition-all duration-300 ease-in-out scale-100"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <FaCheckCircle className="text-xl text-green-600" />
            <h2 className="text-lg font-semibold text-gray-800">
              Confirm Delivery
            </h2>
          </div>
          <button
            onClick={onClose}
            disabled={isLoading}
            className="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-green-500 focus:ring-opacity-50"
            aria-label="Close modal"
          >
            <FaTimes size={20} />
          </button>
        </div>

        {/* Modal Body - Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <p className="text-sm text-gray-600">
            You are about to mark the delivery for Order ID <span className="font-medium text-gray-700">#{orderId ? orderId.substring(0, 8) : 'N/A'}</span>
            {deliveryId && <>, Delivery ID <span className="font-medium text-gray-700">#{deliveryId.substring(0,8)}</span></>} as delivered.
          </p>

          {deliveryDetails && (
            <div className="text-xs p-3 bg-gray-50 rounded-md border border-gray-200">
              <p><span className="font-medium">Shipped Via:</span> {deliveryDetails.courierName || 'N/A'}</p>
              <p><span className="font-medium">Tracking #:</span> {deliveryDetails.trackingNumber || 'N/A'}</p>
              <p><span className="font-medium">Recipient:</span> {deliveryDetails.shippingRecipientName || 'N/A'}</p>
            </div>
          )}
          
          <div>
            <label htmlFor="modal-deliveredNotes" className="block text-sm font-medium text-gray-700 mb-1">
              Optional Notes (e.g., confirmed with buyer, left at doorstep)
            </label>
            <textarea
              id="modal-deliveredNotes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 sm:text-sm"
              placeholder="e.g., Buyer confirmed receipt via phone at 2:30 PM."
              disabled={isLoading}
            />
          </div>

          {apiError && (
            <p className="text-xs text-red-600 bg-red-50 p-2 rounded-md">{apiError}</p>
          )}
        </form>

        {/* Modal Footer - Actions */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-400 transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            type="submit" 
            onClick={handleSubmit}
            disabled={isLoading}
            className="px-4 py-2 text-sm font-medium text-white bg-green-600 border border-transparent rounded-md shadow-sm hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            ) : (
              'Confirm Delivery'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export default MarkAsDeliveredFormModal;