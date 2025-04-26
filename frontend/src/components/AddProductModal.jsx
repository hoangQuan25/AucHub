// src/components/AddProductModal.jsx (Real Cloudinary Upload + Debug Logging)
import React, { useState, useEffect } from "react";
// Make sure you have axios installed and apiClient configured if you switch fetch to axios
// import apiClient from '../api/apiClient';

function AddProductModal({ isOpen, onClose, onSuccess }) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState([]); // Holds File objects selected by user
  const [error, setError] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // --- IMPORTANT: Configure these ---
  const CLOUDINARY_CLOUD_NAME = "dkw4hauo9"; // Your provided Cloudinary cloud name
  const CLOUDINARY_UPLOAD_PRESET = "auction_preset"; // Your provided Upload Preset NAME
  const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;
  // ---

  // Reset form when modal opens/closes
  useEffect(() => {
    if (!isOpen) {
      setTitle("");
      setDescription("");
      setImages([]);
      setError("");
      setIsUploading(false);
      setIsSaving(false);
    }
  }, [isOpen]);

  // --- MODIFIED handleImageChange with LOGGING ---
  const handleImageChange = (event) => {
    console.log("--- handleImageChange Triggered ---");
    setError(""); // Clear previous errors

    if (event.target.files) {
      console.log("event.target.files object:", event.target.files);
      const filesArray = Array.from(event.target.files);
      console.log("Number of files selected:", filesArray.length);

      if (filesArray.length > 5) {
        console.log("Validation Error: Too many files selected.");
        setError("You can upload a maximum of 5 images.");
        setImages([]); // Clear state
        event.target.value = null; // Reset the input visually
      } else if (filesArray.length > 0) {
        console.log("Attempting to set images state with:", filesArray);
        setImages(filesArray); // Update state
        // Check state right after setting (won't show update immediately due to async nature)
        // console.log("images state variable immediately after setImages:", images);
      } else {
        console.log("Validation: No files selected or selection cleared.");
        setImages([]); // Clear state if no files are selected
      }
    } else {
      console.log("handleImageChange: event.target.files is null or undefined");
    }
    console.log("--- handleImageChange Finished ---");
  };
  // --- END MODIFIED handleImageChange ---

  // --- MODIFIED handleSubmit with LOGGING and Real Upload ---
  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    // --- Add log HERE ---
    console.log("--- handleSubmit Triggered ---");
    console.log("handleSubmit: Current 'images' state:", images); // Check state value *at submit time*
    console.log("handleSubmit: Current 'images' state length:", images.length);
    // --- End log ---

    // Basic Validation
    if (!title.trim() || !description.trim()) {
      setError("Title and Description are required.");
      return;
    }
    if (images.length === 0) {
      // Check THIS value
      console.log("Validation Failed: images.length is 0");
      setError("Please upload at least one image.");
      return; // <-- If it fails here, the state wasn't updated correctly
    }
    if (!CLOUDINARY_CLOUD_NAME || CLOUDINARY_CLOUD_NAME === "YOUR_CLOUD_NAME") {
      /* ... */
    }
    if (
      !CLOUDINARY_UPLOAD_PRESET ||
      CLOUDINARY_UPLOAD_PRESET === "auction_preset"
    ) {
      // Check if preset name is default/placeholder
      console.warn(
        "Using default/placeholder Cloudinary Upload Preset name 'auction_preset'. Ensure this is correct."
      );
    }
    // --- End Validation ---

    setIsUploading(true);
    const uploadedImageUrls = [];

    // --- Upload files to Cloudinary concurrently ---
    console.log("Starting real image uploads to Cloudinary...");
    const uploadPromises = images.map(async (file) => {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);

      console.log(`Uploading ${file.name}...`);
      try {
        // Using fetch API
        const response = await fetch(CLOUDINARY_UPLOAD_URL, {
          method: "POST",
          body: formData,
        });
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(
            `Cloudinary upload failed: ${
              errorData?.error?.message || response.statusText
            }`
          );
        }
        const data = await response.json();
        console.log(`Uploaded ${file.name} successfully:`, data.secure_url);
        return data.secure_url; // Return the secure URL
      } catch (uploadError) {
        console.error(`Error uploading ${file.name}:`, uploadError);
        throw uploadError; // Re-throw to make Promise.all fail
      }
    });

    try {
      // Wait for all uploads
      const urls = await Promise.all(uploadPromises);
      uploadedImageUrls.push(...urls);
      console.log("All images uploaded successfully. URLs:", uploadedImageUrls);
      setIsUploading(false);

      // --- Save product metadata to backend ---
      setIsSaving(true);
      console.log("Saving product metadata to backend...");
      const productPayload = {
        title: title.trim(),
        description: description.trim(),
        imageUrls: uploadedImageUrls,
      };

      // !!! --- TODO: Replace console.log with actual API call --- !!!
      // await apiClient.post('/api/products', productPayload);
      console.log(
        "TODO: Replace this log with actual backend API call -> await apiClient.post('/api/products', productPayload);"
      );
      await new Promise((resolve) => setTimeout(resolve, 500)); // Simulate backend save delay
      // !!! --- END TODO --- !!!

      console.log("Product metadata save simulated/completed.");
      setIsSaving(false);
      onSuccess(productPayload); // Call parent's success handler
      handleClose(); // Close modal
    } catch (err) {
      console.error("Failed during product creation (upload or save):", err);
      setError(
        err?.response?.data?.message ||
          err?.message ||
          "Failed to create product."
      );
      setIsUploading(false);
      setIsSaving(false);
    }
  };
  // --- END MODIFIED handleSubmit ---

  const handleClose = () => {
    // Reset form state when closing
    setTitle("");
    setDescription("");
    setImages([]);
    setError("");
    onClose(); // Call the onClose function passed from parent
  };

  if (!isOpen) return null;

  // --- Render Logic (Keep the form structure as before) ---
  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        {/* ... Modal Header ... */}
        <div className="flex justify-between items-center mb-4 border-b pb-2">
          <h3 className="text-xl font-bold">Add New Product</h3>
          <button
            onClick={handleClose}
            disabled={isUploading || isSaving}
            className="text-gray-500 hover:text-gray-800 text-2xl font-bold disabled:opacity-50"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
          <div>
            <label
              htmlFor="productTitle"
              className="block mb-1 font-medium text-sm text-gray-700"
            >
              Title:
            </label>
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
            <label
              htmlFor="productDescription"
              className="block mb-1 font-medium text-sm text-gray-700"
            >
              Description:
            </label>
            <textarea
              id="productDescription"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              rows={5} // Make it larger
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          {/* Image Input */}
          <div>
            <label
              htmlFor="productImages"
              className="block mb-1 font-medium text-sm text-gray-700"
            >
              Images (1-5 files):
            </label>
            <input
              id="productImages"
              type="file"
              multiple
              accept="image/*"
              onChange={handleImageChange}
              required
              className="w-full p-2 border rounded border-gray-300 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100 disabled:opacity-50"
              disabled={isUploading || isSaving} // Disable while processing
            />
            {images.length > 0 && (
              <p className="text-xs text-gray-600 mt-1">
                {images.length} file(s) selected.
              </p>
            )}
          </div>

          {/* Buttons */}
          <div className="flex justify-end space-x-3 pt-4 border-t mt-6">
            <button
              type="button"
              onClick={handleClose}
              disabled={isUploading || isSaving}
              className="px-4 py-2 bg-gray-300 hover:bg-gray-400 rounded text-gray-800 disabled:opacity-50"
            >
              {" "}
              Cancel{" "}
            </button>
            <button
              type="submit"
              disabled={isUploading || isSaving}
              className="px-4 py-2 bg-green-600 hover:bg-green-700 rounded text-white disabled:opacity-50"
            >
              {isUploading
                ? "Uploading Images..."
                : isSaving
                ? "Saving Product..."
                : "Add Product"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddProductModal;
