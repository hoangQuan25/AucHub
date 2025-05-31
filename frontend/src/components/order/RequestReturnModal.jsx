// src/components/RequestReturnModal.jsx
import React, { useState, useEffect } from 'react';
import { FaTimes, FaTimesCircle, FaFileUpload, FaSpinner } from 'react-icons/fa';

// Reusing your Cloudinary constants
const CLOUDINARY_CLOUD_NAME = "dkw4hauo9";
const CLOUDINARY_UPLOAD_PRESET = "auction_preset";
const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;

function RequestReturnModal({ isOpen, onClose, onSubmit, isLoading: isSubmitting, apiError, orderId }) {
  // Form field states
  const [reason, setReason] = useState('');
  const [comments, setComments] = useState('');
  const [returnCourier, setReturnCourier] = useState('');
  const [returnTrackingNumber, setReturnTrackingNumber] = useState('');

  // State for Image Uploading
  const [imageFiles, setImageFiles] = useState([]);
  const [imagePreviewUrls, setImagePreviewUrls] = useState([]);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState(''); // Internal modal error for validation/uploads

  // Reset state when modal opens or closes
  useEffect(() => {
    if (isOpen) {
      setReason('');
      setComments('');
      setReturnCourier('');
      setReturnTrackingNumber('');
      setError('');
      setImageFiles([]);
      imagePreviewUrls.forEach(url => URL.revokeObjectURL(url));
      setImagePreviewUrls([]);
    }
  }, [isOpen]);

  // Image handling logic (handleImageChange, handleRemoveImage) remains the same...
  const handleImageChange = (event) => {
    setError('');
    if (event.target.files) {
      const filesArray = Array.from(event.target.files);
      const availableSlots = 10 - imageFiles.length;
      if (filesArray.length > availableSlots) {
        setError(`You can only add ${availableSlots > 0 ? availableSlots : 0} more image(s) (max 10 total).`);
        event.target.value = null;
      } else if (filesArray.length > 0) {
        setImageFiles(prev => [...prev, ...filesArray]);
        const newPreviews = filesArray.map(file => URL.createObjectURL(file));
        setImagePreviewUrls(prev => [...prev, ...newPreviews]);
        event.target.value = null;
      }
    }
  };

  const handleRemoveImage = (indexToRemove) => {
    URL.revokeObjectURL(imagePreviewUrls[indexToRemove]);
    setImageFiles(prev => prev.filter((_, index) => index !== indexToRemove));
    setImagePreviewUrls(prev => prev.filter((_, index) => index !== indexToRemove));
  };


  // Submit Handler remains the same...
  const handleSubmit = async () => {
    setError('');
    if (!reason || !returnCourier || !returnTrackingNumber) {
      setError("Please fill in all required fields: Reason, Return Courier, and Tracking Number.");
      return;
    }
    setIsUploading(true);
    try {
      let uploadedImageUrls = [];
      if (imageFiles.length > 0) {
        const uploadPromises = imageFiles.map(async (file) => {
          const formData = new FormData();
          formData.append('file', file);
          formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
          const response = await fetch(CLOUDINARY_UPLOAD_URL, { method: 'POST', body: formData });
          if (!response.ok) throw new Error(`Image upload failed.`);
          const data = await response.json();
          return data.secure_url;
        });
        uploadedImageUrls = await Promise.all(uploadPromises);
      }
      setIsUploading(false);
      const returnData = { reason, comments, returnCourier, returnTrackingNumber, imageUrls: uploadedImageUrls };
      await onSubmit(returnData);
    } catch (err) {
      console.error("Failed during image upload or submission:", err);
      setError(err?.message || 'An error occurred. Please try again.');
      setIsUploading(false);
    }
  };

  const totalIsLoading = isSubmitting || isUploading;

  // --- NEW: Render nothing if modal is not open ---
  if (!isOpen) return null;

  // --- NEW: Self-contained modal structure ---
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex justify-center items-center p-4" aria-modal="true" role="dialog">
      {/* Modal Panel */}
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl transform transition-all">
        {/* Modal Header */}
        <div className="flex items-center justify-between p-4 border-b">
          <h2 className="text-xl font-semibold text-gray-800">Request Return & Refund</h2>
          <button onClick={onClose} disabled={totalIsLoading} className="text-gray-400 hover:text-gray-600">
            <FaTimes size={20} />
          </button>
        </div>

        {/* Modal Body (Scrollable) */}
        <div className="p-6 space-y-4 max-h-[70vh] overflow-y-auto">
          {/* --- Form Fields (Same as before) --- */}
          <div>
            <label className="block text-sm font-medium text-gray-700">Reason for Return*</label>
            <input type="text" value={reason} onChange={(e) => setReason(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="e.g., Item not as described"/>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Return Courier*</label>
            <input type="text" value={returnCourier} onChange={(e) => setReturnCourier(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="e.g., Vietnam Post, Viettel Post" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Return Tracking Number*</label>
            <input type="text" value={returnTrackingNumber} onChange={(e) => setReturnTrackingNumber(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" placeholder="Enter the tracking number" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Additional Comments</label>
            <textarea rows={3} value={comments} onChange={(e) => setComments(e.target.value)} className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"/>
          </div>

          {/* Image Upload Section */}
          <div>
            <label className="block text-sm font-medium text-gray-700">Add Images (max 10)</label>
            <div className="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md">
                <div className="space-y-1 text-center">
                    <FaFileUpload className="mx-auto h-12 w-12 text-gray-400" />
                    <div className="flex text-sm text-gray-600">
                        <label htmlFor="file-upload" className="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500">
                            <span>Upload files</span>
                            <input id="file-upload" name="file-upload" type="file" className="sr-only" multiple accept="image/*" onChange={handleImageChange} disabled={totalIsLoading} />
                        </label>
                        <p className="pl-1">or drag and drop</p>
                    </div>
                    <p className="text-xs text-gray-500">PNG, JPG, GIF up to 10MB</p>
                </div>
            </div>
          </div>

          {/* Image Preview Section */}
          {imagePreviewUrls.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700">Image Previews:</p>
              <div className="mt-2 grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-2">
                {imagePreviewUrls.map((url, index) => (
                  <div key={index} className="relative group">
                    <img src={url} alt={`Preview ${index}`} className="h-24 w-24 object-cover rounded-md" />
                    <button onClick={() => handleRemoveImage(index)} disabled={totalIsLoading} className="absolute top-0 right-0 -mt-2 -mr-2 bg-white rounded-full p-0.5 text-red-500 hover:text-red-700 disabled:opacity-50" aria-label="Remove image">
                      <FaTimesCircle className="h-5 w-5" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-4 bg-gray-50 border-t rounded-b-lg">
          {(error || apiError) && (
            <div className="mb-3 p-3 bg-red-100 text-red-700 text-sm rounded-md">
              {error || apiError}
            </div>
          )}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={totalIsLoading}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSubmit}
              disabled={totalIsLoading}
              className="inline-flex justify-center px-4 py-2 text-sm font-medium text-white bg-orange-600 border border-transparent rounded-md shadow-sm hover:bg-orange-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-orange-500 disabled:opacity-50"
            >
              {totalIsLoading && <FaSpinner className="animate-spin -ml-1 mr-3 h-5 w-5" />}
              {isUploading ? 'Uploading...' : isSubmitting ? 'Submitting...' : 'Submit Request'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default RequestReturnModal;