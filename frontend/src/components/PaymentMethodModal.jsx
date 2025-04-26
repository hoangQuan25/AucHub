// src/components/PaymentMethodModal.jsx
import React, { useState, useEffect } from 'react';

function PaymentMethodModal({ isOpen, onClose, onSave }) {
  // State for form fields
  const [formData, setFormData] = useState({
    cardType: 'Visa', // Default selection
    last4Digits: '',
    expiryMonth: '',
    expiryYear: '',
    isDefault: false,
  });
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setFormData({
        cardType: 'Visa',
        last4Digits: '',
        expiryMonth: '',
        expiryYear: '',
        isDefault: false,
      });
      setError('');
      setIsSaving(false);
    }
  }, [isOpen]);

  // Handle input changes
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    // Basic validation for last4Digits
    if (name === 'last4Digits' && (!/^\d*$/.test(value) || value.length > 4)) {
        return; // Only allow up to 4 digits
    }
     // Basic validation for expiry month/year (simple length check)
    if (name === 'expiryMonth' && (!/^\d*$/.test(value) || value.length > 2)) {
        return;
    }
    if (name === 'expiryYear' && (!/^\d*$/.test(value) || value.length > 4)) {
        return;
    }

    setFormData(prevData => ({
      ...prevData,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsSaving(true);

    // Basic frontend validation
    if (formData.last4Digits.length !== 4) {
        setError('Last 4 digits must be exactly 4 numbers.');
        setIsSaving(false);
        return;
    }
    if (!formData.expiryMonth || formData.expiryMonth.length < 1 || formData.expiryMonth.length > 2) {
        setError('Expiry month is required (1-2 digits).');
        setIsSaving(false);
        return;
    }
     if (!formData.expiryYear || formData.expiryYear.length !== 4) {
        setError('Expiry year is required (4 digits).');
        setIsSaving(false);
        return;
    }
    // Add more robust date validation if needed (e.g., check if expiry is in the past)


    try {
      // Call the onSave prop function passed from the parent
      await onSave(formData); // Only pass formData for adding
      // onSave should handle closing the modal on success
    } catch (saveError) {
      console.error("Error saving payment method:", saveError);
      setError(saveError.message || 'Failed to save payment method. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  // Don't render the modal if it's not open
  if (!isOpen) {
    return null;
  }

  // --- Generate Year Options ---
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 15 }, (_, i) => currentYear + i); // Next 15 years

  // --- Generate Month Options ---
  const months = Array.from({ length: 12 }, (_, i) => (i + 1).toString().padStart(2, '0')); // 01-12


  return (
    // Basic Modal Structure
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50 flex justify-center items-center p-4">
      <div className="relative bg-white w-full max-w-lg mx-auto p-6 rounded-lg shadow-xl">
        <h3 className="text-lg font-medium leading-6 text-gray-900 mb-4">
          Add New Payment Method
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Card Type */}
          <div>
            <label htmlFor="cardType" className="block text-sm font-medium text-gray-700">Card Type</label>
            <select
              id="cardType"
              name="cardType"
              value={formData.cardType}
              onChange={handleChange}
              required
              className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
            >
              <option>Visa</option>
              <option>Mastercard</option>
              {/* Add other card types as needed */}
              <option>American Express</option>
              <option>Discover</option>
            </select>
          </div>

          {/* Last 4 Digits */}
          <div>
            <label htmlFor="last4Digits" className="block text-sm font-medium text-gray-700">Last 4 Digits</label>
            <input
              type="text" // Use text to allow leading zeros if needed, pattern handles digits
              name="last4Digits"
              id="last4Digits"
              value={formData.last4Digits}
              onChange={handleChange}
              required
              maxLength="4"
              pattern="\d{4}" // HTML5 pattern validation
              title="Please enter exactly 4 digits"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>

          {/* Expiry Date */}
          <div className="flex space-x-3">
            <div className="flex-1">
              <label htmlFor="expiryMonth" className="block text-sm font-medium text-gray-700">Expiry Month</label>
              <select
                id="expiryMonth"
                name="expiryMonth"
                value={formData.expiryMonth}
                onChange={handleChange}
                required
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                <option value="" disabled>MM</option>
                {months.map(month => (
                  <option key={month} value={month}>{month}</option>
                ))}
              </select>
            </div>
            <div className="flex-1">
              <label htmlFor="expiryYear" className="block text-sm font-medium text-gray-700">Expiry Year</label>
               <select
                id="expiryYear"
                name="expiryYear"
                value={formData.expiryYear}
                onChange={handleChange}
                required
                className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
              >
                 <option value="" disabled>YYYY</option>
                 {years.map(year => (
                   <option key={year} value={year}>{year}</option>
                 ))}
               </select>
            </div>
          </div>

          {/* Default Checkbox */}
          <div className="flex items-center">
            <input
              id="isDefaultPM" // Use different ID from address modal
              name="isDefault"
              type="checkbox"
              checked={formData.isDefault}
              onChange={handleChange}
              className="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
            />
            <label htmlFor="isDefaultPM" className="ml-2 block text-sm text-gray-900">
              Set as default payment method
            </label>
          </div>

          {/* Error Message */}
          {error && <p className="text-sm text-red-600">{error}</p>}

          {/* Action Buttons */}
          <div className="mt-6 flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSaving}
              className="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSaving}
              className="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : 'Save Payment Method'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default PaymentMethodModal;
