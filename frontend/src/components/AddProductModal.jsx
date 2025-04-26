// src/components/AddProductModal.jsx
import React, { useState, useEffect } from 'react';

function AddProductModal({ isOpen, onClose, onSuccess }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [images, setImages] = useState([]); // Holds File objects selected by user
  const [error, setError] = useState('');
  const [isUploading, setIsUploading] = useState(false); // Uploading state for images
  const [isSaving, setIsSaving] = useState(false); // Saving state for product metadata

  // --- IMPORTANT: Configure these ---
  const CLOUDINARY_CLOUD_NAME = "dkw4hauo9"; // <--- Replace with YOUR Cloudinary Cloud Name
  const CLOUDINARY_UPLOAD_PRESET = "auction_preset"; // <--- Replace with the Upload Preset NAME you created
  const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;

  const handleImageChange = (event) => {
    setError(''); // Clear previous errors
    if (event.target.files) {
      if (event.target.files.length > 5) {
        setError('You can upload a maximum of 5 images.');
        setImages([]); // Clear selection
        event.target.value = null; // Reset file input
      } else {
        setImages(Array.from(event.target.files)); // Store File objects
        console.log("Selected files:", event.target.files);
      }
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault(); // Prevent default form submission
    setError('');

    // Basic validation
    if (!title.trim() || !description.trim()) {
      setError('Title and Description are required.');
      return;
    }
    if (images.length === 0) {
      setError('Please upload at least one image.');
      return;
    }

    // --- Mock Submission ---
    console.log('Submitting Product (Mock):', { title, description, images });
    // In a real app, you would:
    // 1. Upload images to a server/storage (this is complex) -> get back URLs
    // 2. Call apiClient.post('/api/products', { title, description, imageUrls: [...] })
    // 3. Handle API response
    // For now, just call the onSuccess callback and close
    onSuccess({ title, description, imageCount: images.length }); // Pass mock data back
    // --- End Mock Submission ---

     // Reset form after mock success
     setTitle('');
     setDescription('');
     setImages([]);
     // Consider keeping modal open on error or closing via onClose prop passed in onSuccess
  };

  const handleClose = () => {
    // Reset form state when closing
    setTitle('');
    setDescription('');
    setImages([]);
    setError('');
    onClose(); // Call the onClose function passed from parent
  };


  if (!isOpen) return null;

  return (
    // Basic Modal Structure (similar to ConfirmationModal but larger)
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4 border-b pb-2">
           <h3 className="text-xl font-bold">Add New Product</h3>
           <button onClick={handleClose} className="text-gray-500 hover:text-gray-800 text-2xl font-bold">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
          <div>
            <label htmlFor="productTitle" className="block mb-1 font-medium text-sm text-gray-700">Title:</label>
            <input
              id="productTitle"
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              maxLength={100} // Example limit
            />
          </div>

          <div>
            <label htmlFor="productDescription" className="block mb-1 font-medium text-sm text-gray-700">Description:</label>
            <textarea
              id="productDescription"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              rows={5} // Make it larger
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label htmlFor="productImages" className="block mb-1 font-medium text-sm text-gray-700">Images (1-5 files):</label>
            <input
              id="productImages"
              type="file"
              multiple // Allow multiple file selection
              accept="image/*" // Accept only image types
              onChange={handleImageChange}
              required
              className="w-full p-2 border rounded border-gray-300 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
            />
             {images.length > 0 && (
                <p className="text-xs text-gray-600 mt-1">{images.length} file(s) selected.</p>
                // Basic preview could be added here by reading file objects
             )}
          </div>

          {/* Buttons */}
          <div className="flex justify-end space-x-3 pt-4 border-t mt-6">
            <button
              type="button" // Important: type="button" to prevent submitting form
              onClick={handleClose}
              className="px-4 py-2 bg-gray-300 hover:bg-gray-400 rounded text-gray-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded text-white"
            >
              Add Product (Mock)
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddProductModal;