// src/components/AddProductModal.jsx (Real Cloudinary Upload + Debug Logging)
import React, { useState, useEffect } from "react";
import CategorySelector from "./CategorySelector"; // Adjust path as needed
// Make sure you have axios installed and apiClient configured if you switch fetch to axios
import apiClient from '../api/apiClient';

const productConditions = [
  { value: 'NEW_WITH_TAGS', label: 'New with Tags' },
  { value: 'LIKE_NEW', label: 'Like New (No Tags)' },
  { value: 'VERY_GOOD', label: 'Very Good' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
  { value: 'POOR', label: 'Poor' },
];

function AddProductModal({ isOpen, onClose, onSuccess }) {
  // Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [condition, setCondition] = useState(''); // State for selected condition
  const [images, setImages] = useState([]);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState(new Set()); // Use a Set for unique IDs

  // State for fetching categories
  const [allCategories, setAllCategories] = useState([]);
  const [categoryLoading, setCategoryLoading] = useState(false);
  const [categoryError, setCategoryError] = useState('');

  // General modal state
  const [error, setError] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const CLOUDINARY_CLOUD_NAME = "dkw4hauo9"; // Your Cloudinary cloud name
  const CLOUDINARY_UPLOAD_PRESET = "auction_preset"; // Your Upload Preset NAME
  const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;

  // --- Fetch Categories when Modal Opens ---
  useEffect(() => {
    if (isOpen) {
      // Reset form when opening
      setTitle('');
      setDescription('');
      setCondition('');
      setImages([]);
      setSelectedCategoryIds(new Set());
      setError('');
      setCategoryError('');
      setIsUploading(false);
      setIsSaving(false);

      // Fetch categories
      setCategoryLoading(true);
      console.log("Fetching categories...");
      apiClient.get('/products/categories') // Assuming public endpoint routed by Gateway
        .then(response => {
          console.log("Categories received:", response.data);
          setAllCategories(response.data || []);
        })
        .catch(err => {
          console.error("Failed to fetch categories:", err);
          setCategoryError("Could not load categories. Please try again.");
          setAllCategories([]);
        })
        .finally(() => {
          setCategoryLoading(false);
        });
    }
  }, [isOpen]); // Dependency: only run when modal opens

  // --- Image Handler (Allows up to 10 now) ---
  const handleImageChange = (event) => {
     setError('');
     if (event.target.files) {
       if (event.target.files.length > 10) { // Increased limit
         setError('You can upload a maximum of 10 images.');
         setImages([]);
         event.target.value = null;
       } else if (event.target.files.length > 0){
         setImages(Array.from(event.target.files));
         console.log("Selected files:", event.target.files);
       } else {
         setImages([]);
       }
     }
   };

  // --- Category Selection Handler ---
  const handleCategoryChange = (categoryId) => {
      setSelectedCategoryIds(prevIds => {
          const newIds = new Set(prevIds); // Create mutable copy
          if (newIds.has(categoryId)) {
              newIds.delete(categoryId); // Toggle off
          } else {
              newIds.add(categoryId); // Toggle on
          }
          console.log("Selected Category IDs:", newIds);
          return newIds;
      });
  };

  // --- Submit Handler ---
  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setCategoryError(''); // Clear category error on submit attempt

    // --- Validation ---
    if (!title.trim() || !description.trim() || !condition) {
      setError('Title, Description, and Condition are required.');
      return;
    }
     if (selectedCategoryIds.size === 0) {
       setError('Please select at least one category.');
       return; // Added category validation
     }
    if (images.length === 0) {
      setError('Please upload at least one image.');
      return;
    }
    // ... Cloudinary config checks ...
    // --- End Validation ---

    setIsUploading(true);
    let uploadedImageUrls = [];

    // --- Cloudinary Upload ---
    console.log('Starting image uploads to Cloudinary...');
    const uploadPromises = images.map(/* ... same Cloudinary upload logic as before ... */
        async (file) => {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
            const response = await fetch(CLOUDINARY_UPLOAD_URL, { method: 'POST', body: formData });
            if (!response.ok) throw new Error(`Upload failed for ${file.name}`);
            const data = await response.json();
            return data.secure_url;
        }
    );

    try {
      uploadedImageUrls = await Promise.all(uploadPromises);
      console.log("Cloudinary Upload Success. URLs:", uploadedImageUrls);
      setIsUploading(false);

      // --- Save Metadata to Backend ---
      setIsSaving(true);
      const productPayload = {
        title: title.trim(),
        description: description.trim(),
        condition: condition, // Selected condition value
        imageUrls: uploadedImageUrls,
        categoryIds: Array.from(selectedCategoryIds) // Convert Set to Array
      };
      console.log("Saving product metadata to backend:", productPayload);

      // Replace with actual API call
      await apiClient.post('/products/new-product', productPayload);

      console.log("Product metadata saved successfully!");
      setIsSaving(false);
      onSuccess(productPayload); // Call parent's success handler
      handleClose(); // Close modal

    } catch (err) {
      console.error("Failed during product creation:", err);
      setError(err?.response?.data?.message || err?.message || 'Failed to create product.');
      setIsUploading(false);
      setIsSaving(false);
    }
  };
  // --- End Submit Handler ---

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

          {/* --- Condition Dropdown --- */}
          <div>
                <label htmlFor="productCondition" className="block mb-1 font-medium text-sm text-gray-700">Condition:</label>
                <select
                    id="productCondition"
                    name="condition"
                    value={condition}
                    onChange={(e) => setCondition(e.target.value)}
                    required
                    className="w-full p-2 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                    <option value="" disabled>-- Select Condition --</option>
                    {productConditions.map(opt => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
            </div>

             {/* --- Category Selector --- */}
             <div>
                <label className="block mb-1 font-medium text-sm text-gray-700">Categories:</label>
                <CategorySelector
                    categories={allCategories}
                    selectedIds={selectedCategoryIds}
                    onSelectionChange={setSelectedCategoryIds} // Pass the state setter directly
                    isLoading={categoryLoading}
                    error={categoryError}
                />
                {/* Display error related to categories specifically if needed */}
             </div>
          {/* --- End Category Selector --- */}

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
