// src/components/AddProductModal.jsx (Real Cloudinary Upload + Debug Logging)
import React, { useState, useEffect } from "react";
import CategorySelector from "./CategorySelector"; // Adjust path as needed
import { FaTimesCircle } from 'react-icons/fa';
import apiClient from "../api/apiClient";

const productConditions = [
  { value: "NEW_WITH_TAGS", label: "New with Tags" },
  { value: "LIKE_NEW", label: "Like New (No Tags)" },
  { value: "VERY_GOOD", label: "Very Good" },
  { value: "GOOD", label: "Good" },
  { value: "FAIR", label: "Fair" },
  { value: "POOR", label: "Poor" },
];

function AddProductModal({ isOpen, onClose, onSuccess, editingProduct }) {
  const isEditMode = !!editingProduct;
  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [condition, setCondition] = useState('');
  const [selectedCategoryIds, setSelectedCategoryIds] = useState(new Set());

  // Image States
  const [existingImageUrls, setExistingImageUrls] = useState([]); // URLs from product being edited
  const [newImageFiles, setNewImageFiles] = useState([]); // NEW File objects selected by user
  const [imagePreviewUrls, setImagePreviewUrls] = useState([]); // For previewing NEW files
  const [imagesToRemove, setImagesToRemove] = useState(new Set());

  // Category Fetching State
  const [allCategories, setAllCategories] = useState([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [categoryError, setCategoryError] = useState("");

  // General modal state
  const [error, setError] = useState("");
  const [isUploading, setIsUploading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const CLOUDINARY_CLOUD_NAME = "dkw4hauo9"; // Your Cloudinary cloud name
  const CLOUDINARY_UPLOAD_PRESET = "auction_preset"; // Your Upload Preset NAME
  const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;

  useEffect(() => {
    if (isOpen) {
      // Reset general state
      setError(''); setCategoryError(''); setIsUploading(false); setIsSaving(false);
      // Reset image states
      setNewImageFiles([]); setImagesToRemove(new Set()); setImagePreviewUrls([]);

      // Fetch categories
      setCategoryLoading(true);
      apiClient.get('/products/categories')
        .then(response => setAllCategories(response.data || []))
        .catch(err => setCategoryError("Could not load categories."))
        .finally(() => setCategoryLoading(false));

      // Pre-fill form if in Edit Mode
      if (isEditMode && editingProduct) {
        setTitle(editingProduct.title || '');
        setDescription(editingProduct.description || '');
        setCondition(editingProduct.condition || '');
        setSelectedCategoryIds(new Set(editingProduct.categories?.map(cat => cat.id) || []));
        setExistingImageUrls(editingProduct.imageUrls || []);
      } else {
        // Reset form for Add Mode
        setTitle(''); setDescription(''); setCondition('');
        setSelectedCategoryIds(new Set()); setExistingImageUrls([]);
      }
    } else {
        // Cleanup object URLs when modal closes
        imagePreviewUrls.forEach(url => URL.revokeObjectURL(url));
        setImagePreviewUrls([]);
    }
  }, [isOpen, isEditMode, editingProduct]); // Re-run if these change

  // --- Handle NEW image selection ---
  const handleNewImageChange = (event) => {
    setError('');
    if (event.target.files) {
      const filesArray = Array.from(event.target.files);
      const currentImageCount = existingImageUrls.length - imagesToRemove.size + newImageFiles.length;
      const availableSlots = 10 - currentImageCount;

      if (filesArray.length > availableSlots) {
           setError(`You can only add ${availableSlots > 0 ? availableSlots : 0} more image(s) (max 10 total).`);
           event.target.value = null; // Reset file input
      } else if (filesArray.length > 0){
          // Append new files to existing new files state
          setNewImageFiles(prev => [...prev, ...filesArray]);
          console.log("New files selected:", filesArray);
          // Generate preview URLs for NEW files only
          const newPreviews = filesArray.map(file => URL.createObjectURL(file));
          setImagePreviewUrls(prev => [...prev, ...newPreviews]);
          event.target.value = null; // Reset file input to allow selecting same file again if removed
      }
    }
  };

  // --- Remove an EXISTING image ---
  const handleRemoveExistingImage = (urlToRemove) => {
      setImagesToRemove(prev => new Set(prev).add(urlToRemove));
      console.log("Marked existing image for removal:", urlToRemove);
  };

  // --- Remove a NEWLY ADDED image (before upload) ---
   const handleRemoveNewImage = (indexToRemove) => {
       // Revoke the specific object URL first to prevent memory leaks
       URL.revokeObjectURL(imagePreviewUrls[indexToRemove]);
       // Remove the file and its preview URL
       setNewImageFiles(prev => prev.filter((_, index) => index !== indexToRemove));
       setImagePreviewUrls(prev => prev.filter((_, index) => index !== indexToRemove));
       console.log("Removed new image at index:", indexToRemove);
   };

   // --- Submit Handler ---
   const handleSubmit = async (event) => {
    event.preventDefault();
    setError(''); setCategoryError('');

    // Calculate final image count for validation
    const finalImageCount = existingImageUrls.length - imagesToRemove.size + newImageFiles.length;

    // --- Validation ---
    if (!title.trim() || !description.trim() || !condition) { setError('Title, Description, and Condition are required.'); return; }
    if (selectedCategoryIds.size === 0) { setError('Please select at least one category.'); return; }
    if (finalImageCount === 0) { setError('Please add or keep at least one image.'); return; }
    if (finalImageCount > 10) { setError('Cannot exceed 10 images total.'); return; } // Should be caught by input, but double check
    // ... Cloudinary config checks ...
    // --- End Validation ---

    setIsUploading(true); // Covers both Cloudinary and Saving steps
    setIsSaving(true);

    let finalImageUrls = []; // This will hold the list sent to backend

    try {
      // --- Step 1: Upload NEW images (if any) ---
      let newlyUploadedUrls = [];
      if (newImageFiles.length > 0) {
        console.log(`Uploading ${newImageFiles.length} new image(s) to Cloudinary...`);
        const uploadPromises = newImageFiles.map(async (file) => {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
            const response = await fetch(CLOUDINARY_UPLOAD_URL, { method: 'POST', body: formData });
            if (!response.ok) throw new Error(`Upload failed for ${file.name}`);
            const data = await response.json();
            return data.secure_url;
        });
        newlyUploadedUrls = await Promise.all(uploadPromises);
        console.log("New images uploaded successfully. URLs:", newlyUploadedUrls);
      }
      setIsUploading(false); // Finished potential uploads

      // --- Step 2: Construct Final Image URL List ---
      // Start with existing URLs, filter out those marked for removal, add new ones
      finalImageUrls = existingImageUrls
          .filter(url => !imagesToRemove.has(url)) // Keep existing if not marked for removal
          .concat(newlyUploadedUrls); // Add the newly uploaded URLs

      console.log("Final image URLs to save:", finalImageUrls);

      // --- Step 3: Prepare Backend Payload ---
      const productPayload = {
        title: title.trim(),
        description: description.trim(),
        condition: condition,
        imageUrls: finalImageUrls, // Send the final list
        categoryIds: Array.from(selectedCategoryIds)
      };

      // --- Step 4: Call Backend API (POST for Add, PUT for Edit) ---
      console.log("Saving product metadata to backend:", productPayload);
      setIsSaving(true); // Set saving true specifically for backend call
      let savedProductData;
      if (isEditMode) {
        console.log(`Sending PUT request to /products/${editingProduct.id}`);
        const response = await apiClient.put(`/products/${editingProduct.id}`, productPayload);
        savedProductData = response.data;
        console.log("Product updated successfully:", savedProductData);
      } else {
        console.log("Sending POST request to /products/new-product");
        const response = await apiClient.post('/products/new-product', productPayload);
        savedProductData = response.data;
        console.log("Product created successfully:", savedProductData);
      }
      setIsSaving(false); // Finished saving

      onSuccess(savedProductData); // Call parent's success handler
      handleClose(); // Close modal

    } catch (err) {
      console.error("Failed during product creation/update:", err);
      setError(err?.response?.data?.message || err?.message || 'Failed to save product.');
      setIsUploading(false);
      setIsSaving(false);
    }
  };
  // --- End Submit Handler ---

  const handleClose = () => { if (isUploading || isSaving) return; onClose(); };

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

          {/* --- Condition Dropdown --- */}
          <div>
            <label
              htmlFor="productCondition"
              className="block mb-1 font-medium text-sm text-gray-700"
            >
              Condition:
            </label>
            <select
              id="productCondition"
              name="condition"
              value={condition}
              onChange={(e) => setCondition(e.target.value)}
              required
              className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="" disabled>
                -- Select Condition --
              </option>
              {productConditions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* --- Category Selector --- */}
          <div>
            <label className="block mb-1 font-medium text-sm text-gray-700">
              Categories:
            </label>
            <CategorySelector
              categories={allCategories}
              selectedIds={selectedCategoryIds}
              onSelectionChange={setSelectedCategoryIds} // Pass the state setter directly
              isLoading={categoryLoading}
              error={categoryError}
            />
          </div>
          
          <div>
                <label className="block mb-1 font-medium text-sm text-gray-700">Images (up to 10 total):</label>

                {/* Display Existing Images (in Edit Mode) with Delete buttons */}
                {isEditMode && existingImageUrls.length > 0 && (
                    <div className="mt-2 mb-2">
                        <p className="text-xs font-semibold text-gray-600 mb-1">Current Images (Click X to remove):</p>
                        <div className="flex flex-wrap gap-2">
                            {existingImageUrls.map((url, index) => (
                                // Only display if not marked for removal
                                !imagesToRemove.has(url) && (
                                    <div key={`existing-${index}`} className="relative group">
                                        <img src={url} alt={`Current ${index+1}`} className="h-20 w-20 object-cover border rounded shadow-sm"/>
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveExistingImage(url)}
                                            className="absolute top-0 right-0 bg-red-500 text-white rounded-full p-0.5 leading-none text-xs opacity-75 group-hover:opacity-100"
                                            title="Remove this image"
                                        >
                                            <FaTimesCircle />
                                        </button>
                                    </div>
                                )
                            ))}
                         </div>
                    </div>
                )}

                {/* Display New Image Previews with Remove buttons */}
                {newImageFiles.length > 0 && (
                     <div className="mt-2 mb-2">
                        <p className="text-xs font-semibold text-gray-600 mb-1">New Images to Upload:</p>
                        <div className="flex flex-wrap gap-2">
                            {imagePreviewUrls.map((previewUrl, index) => (
                                <div key={`new-${index}`} className="relative group">
                                    <img src={previewUrl} alt={`New preview ${index+1}`} className="h-20 w-20 object-cover border rounded shadow-sm"/>
                                    <button
                                        type="button"
                                        onClick={() => handleRemoveNewImage(index)}
                                        className="absolute top-0 right-0 bg-red-500 text-white rounded-full p-0.5 leading-none text-xs opacity-75 group-hover:opacity-100"
                                        title="Remove this new image"
                                    >
                                         <FaTimesCircle />
                                    </button>
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                {/* File Input - remove HTML5 'required' if allowing edit without new images */}
                <label htmlFor="productImages" className="block mb-1 font-medium text-sm text-gray-700 sr-only">Select Images:</label> {/* Hide label visually if needed */}
                <input
                    id="productImages"
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleNewImageChange}
                    // Remove 'required' here - validation is handled in handleSubmit
                    className="w-full p-2 border rounded border-gray-300 file:mr-4 file:py-2 file:px-4 file:rounded file:border-0 file:text-sm file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100 disabled:opacity-50"
                    disabled={isUploading || isSaving || (existingImageUrls.length - imagesToRemove.size + newImageFiles.length >= 10)} // Disable if max reached
                />
                 <p className="text-xs text-gray-600 mt-1">
                     Total images: {existingImageUrls.length - imagesToRemove.size + newImageFiles.length} / 10
                 </p>
            </div>
             {/* --- END IMAGE SECTION --- */}

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
                ? "Uploading..."
                : isSaving
                ? "Saving..."
                : isEditMode
                ? "Save Changes"
                : "Add Product"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddProductModal;
