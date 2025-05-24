// src/components/EditProfileModal.jsx
import React, { useState, useEffect } from 'react';

function EditProfileModal({ isOpen, onClose, onSave, initialData, error, success }) {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    streetAddress: '',
    city: '',
    stateProvince: '',
    postalCode: '',
    country: '',
    sellerDescription: '', // New state field
  });
  const [isSaving, setIsSaving] = useState(false);

  // When initialData changes (or modal opens), update the form state
  useEffect(() => {
    if (initialData) {
      setFormData({
        firstName: initialData.firstName || '',
        lastName: initialData.lastName || '',
        phoneNumber: initialData.phoneNumber || '',
        streetAddress: initialData.streetAddress || '',
        city: initialData.city || '',
        stateProvince: initialData.stateProvince || '',
        postalCode: initialData.postalCode || '',
        country: initialData.country || '',
        // Populate sellerDescription if user is a seller
        sellerDescription: initialData.isSeller || initialData.seller ? (initialData.sellerDescription || '') : '',
      });
    }
  }, [initialData, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
     e.preventDefault();
     setIsSaving(true);
     try {
        // Pass the whole formData. The onSave in UserInfoPage will construct the payload.
        await onSave(formData);
     } catch (err) {
         console.error("Save failed in modal:", err);
     } finally {
         setIsSaving(false);
     }
  };

  if (!isOpen) return null;

  // Determine if the current user is a seller from initialData
  const isUserSeller = initialData && (initialData.isSeller || initialData.seller);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4 border-b pb-2">
           <h3 className="text-xl font-bold">Edit Profile</h3>
           <button onClick={onClose} className="text-gray-500 hover:text-gray-800 text-2xl font-bold">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
            {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
            {success && <p className="text-green-500 text-sm mb-3">{success}</p>}

            <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Personal Info</legend>
                {/* ... firstName, lastName, phoneNumber inputs ... */}
                 <div className="mb-3">
                    <label htmlFor="firstName" className="block mb-1 font-medium text-sm text-gray-700">First Name:</label>
                    <input id="firstName" name="firstName" type="text" value={formData.firstName} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                <div className="mb-3">
                   <label htmlFor="lastName" className="block mb-1 font-medium text-sm text-gray-700">Last Name:</label>
                   <input id="lastName" name="lastName" type="text" value={formData.lastName} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                 <div className="mb-3">
                   <label htmlFor="phoneNumber" className="block mb-1 font-medium text-sm text-gray-700">Phone:</label>
                   <input id="phoneNumber" name="phoneNumber" type="tel" value={formData.phoneNumber} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
            </fieldset>

            <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Address</legend>
                {/* ... address inputs ... */}
                <div className="mb-3">
                   <label htmlFor="streetAddress" className="block mb-1 font-medium text-sm text-gray-700">Street:</label>
                   <input id="streetAddress" name="streetAddress" type="text" value={formData.streetAddress} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                     <div>
                        <label htmlFor="city" className="block mb-1 font-medium text-sm text-gray-700">City:</label>
                        <input id="city" name="city" type="text" value={formData.city} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                     <div>
                        <label htmlFor="stateProvince" className="block mb-1 font-medium text-sm text-gray-700">State/Province:</label>
                        <input id="stateProvince" name="stateProvince" type="text" value={formData.stateProvince} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                 </div>
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                     <div>
                        <label htmlFor="postalCode" className="block mb-1 font-medium text-sm text-gray-700">Postal Code:</label>
                        <input id="postalCode" name="postalCode" type="text" value={formData.postalCode} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                     <div>
                        <label htmlFor="country" className="block mb-1 font-medium text-sm text-gray-700">Country:</label>
                        <input id="country" name="country" type="text" value={formData.country} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                 </div>
            </fieldset>

            {/* Conditionally render Seller Description fieldset */}
            {isUserSeller && (
              <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Seller Information</legend>
                <div className="mb-3">
                  <label htmlFor="sellerDescription" className="block mb-1 font-medium text-sm text-gray-700">
                    Seller Description (tell buyers about yourself or your shop):
                  </label>
                  <textarea
                    id="sellerDescription"
                    name="sellerDescription"
                    rows={4}
                    value={formData.sellerDescription}
                    onChange={handleChange}
                    className="w-full p-2 border rounded border-gray-300"
                    maxLength={1000} // Match backend length
                  />
                  <p className="text-xs text-gray-500 mt-1">Max 1000 characters.</p>
                </div>
              </fieldset>
            )}

           <div className="flex justify-end space-x-3 pt-4 border-t mt-6">
             <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-300 hover:bg-gray-400 rounded text-gray-800"> Cancel </button>
             <button type="submit" disabled={isSaving} className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded text-white disabled:opacity-50"> {isSaving ? 'Saving...' : 'Save Changes'} </button>
           </div>
        </form>
      </div>
    </div>
  );
}

export default EditProfileModal;