// src/components/delivery/MarkAsShippedFormModal.jsx
import React, { useState, useEffect } from 'react';
import { FaShippingFast, FaTimes } from 'react-icons/fa'; 

function MarkAsShippedFormModal({
  isOpen,
  onClose,
  onSubmit, // (shippingData) => void
  orderId,    // For display
  deliveryId, // For display or context
  isLoading,
  apiError,   // To display errors from submission attempts
}) {
  const [courierName, setCourierName] = useState('');
  const [trackingNumber, setTrackingNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [formError, setFormError] = useState('');

  // Reset form when modal opens or order/delivery context changes
  useEffect(() => {
    if (isOpen) {
      setCourierName('');
      setTrackingNumber('');
      setNotes('');
      setFormError('');
    }
  }, [isOpen]);

  const handleSubmit = (e) => {
    e.preventDefault();
    setFormError(''); // Clear previous form error
    if (!courierName.trim()) {
      setFormError('Courier Name is required.');
      return;
    }
    if (!trackingNumber.trim()) {
      setFormError('Tracking Number is required.');
      return;
    }
    onSubmit({ courierName, trackingNumber, notes });
  };

  if (!isOpen) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black bg-opacity-60 transition-opacity duration-300 ease-in-out"
      onClick={onClose} // Close on overlay click
    >
      <div
        className="bg-white rounded-xl shadow-2xl w-full max-w-lg transform transition-all duration-300 ease-in-out scale-100"
        onClick={(e) => e.stopPropagation()} // Prevent closing when clicking inside modal
      >
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <FaShippingFast className="text-xl text-blue-600" />
            <h2 className="text-lg font-semibold text-gray-800">
              Mark as Shipped & Add Tracking
            </h2>
          </div>
          <button
            onClick={onClose}
            disabled={isLoading}
            className="text-gray-400 hover:text-gray-600 transition-colors p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50"
            aria-label="Close modal"
          >
            <FaTimes size={20} />
          </button>
        </div>

        {/* Modal Body - Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          <p className="text-sm text-gray-600">
            Order ID: <span className="font-medium text-gray-700">{orderId ? orderId.substring(0, 8) : 'N/A'}</span> <br />
            {deliveryId && <>Delivery ID: <span className="font-medium text-gray-700">{deliveryId.substring(0,8)}</span></>}
          </p>
          
          <div>
            <label htmlFor="modal-courierName" className="block text-sm font-medium text-gray-700 mb-1">
              Courier Name <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="modal-courierName"
              value={courierName}
              onChange={(e) => setCourierName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="e.g., Vietnam Post, GiaoHangNhanh"
              disabled={isLoading}
              required
            />
          </div>

          <div>
            <label htmlFor="modal-trackingNumber" className="block text-sm font-medium text-gray-700 mb-1">
              Tracking Number <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="modal-trackingNumber"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="e.g., VN123456789XY"
              disabled={isLoading}
              required
            />
          </div>

          <div>
            <label htmlFor="modal-shippingNotes" className="block text-sm font-medium text-gray-700 mb-1">
              Optional Notes for Buyer
            </label>
            <textarea
              id="modal-shippingNotes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="e.g., Expected delivery in 3-5 days. Package contains fragile items."
              disabled={isLoading}
            />
          </div>

          {formError && (
            <p className="text-xs text-red-600 bg-red-50 p-2 rounded-md">{formError}</p>
          )}
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
            type="submit" // Important: This button will now trigger the form's onSubmit
            onClick={handleSubmit} // Also call handleSubmit directly in case form tag doesn't capture it
            disabled={isLoading || !courierName.trim() || !trackingNumber.trim()}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md shadow-sm hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            ) : (
              'Confirm & Notify Buyer'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export default MarkAsShippedFormModal;