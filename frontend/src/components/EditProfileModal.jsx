// src/components/EditProfileModal.jsx
import React, { useState, useEffect } from 'react';

function EditProfileModal({ isOpen, onClose, onSave, initialData, error, success }) {
  // Initialize state with initialData passed via props
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    streetAddress: '',
    city: '',
    stateProvince: '',
    postalCode: '',
    country: '',
    paymentCardType: '',
    paymentLast4Digits: '',
    paymentExpiryMonth: '',
    paymentExpiryYear: '',
    // Add other fields from UpdateUserDto as needed
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
        paymentCardType: initialData.paymentCardType || '',
        paymentLast4Digits: initialData.paymentLast4Digits || '',
        paymentExpiryMonth: initialData.paymentExpiryMonth || '',
        paymentExpiryYear: initialData.paymentExpiryYear || '',
      });
    }
  }, [initialData, isOpen]); // Update form when initialData is available or modal opens

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
     e.preventDefault();
     setIsSaving(true);
     try {
        await onSave(formData); // Call the onSave prop passed from UserInfoPage
        // If onSave is successful, the parent component closes the modal
     } catch (err) {
         // Error state is set in the parent component (UserInfoPage) via onSave's catch block
         console.error("Save failed in modal:", err);
     } finally {
         setIsSaving(false);
     }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-60 flex justify-center items-center z-50 p-4">
      <div className="bg-white p-6 rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-4 border-b pb-2">
           <h3 className="text-xl font-bold">Edit Profile</h3>
           <button onClick={onClose} className="text-gray-500 hover:text-gray-800 text-2xl font-bold">&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
            {/* Display error/success messages passed from parent */}
            {error && <p className="text-red-500 text-sm mb-3">{error}</p>}
            {success && <p className="text-green-500 text-sm mb-3">{success}</p>}

            {/* --- Form Sections --- */}
            <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Personal Info</legend>
                {/* First Name */}
                <div className="mb-3">
                    <label htmlFor="firstName" className="block mb-1 font-medium text-sm text-gray-700">First Name:</label>
                    <input id="firstName" name="firstName" type="text" value={formData.firstName} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                {/* Last Name */}
                <div className="mb-3">
                   <label htmlFor="lastName" className="block mb-1 font-medium text-sm text-gray-700">Last Name:</label>
                   <input id="lastName" name="lastName" type="text" value={formData.lastName} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                 {/* Phone Number */}
                 <div className="mb-3">
                   <label htmlFor="phoneNumber" className="block mb-1 font-medium text-sm text-gray-700">Phone:</label>
                   <input id="phoneNumber" name="phoneNumber" type="tel" value={formData.phoneNumber} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
            </fieldset>

            <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Address</legend>
                 {/* Street Address */}
                 <div className="mb-3">
                   <label htmlFor="streetAddress" className="block mb-1 font-medium text-sm text-gray-700">Street:</label>
                   <input id="streetAddress" name="streetAddress" type="text" value={formData.streetAddress} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                 {/* City, State, Postal, Country inputs... */}
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

             <fieldset className="border p-4 rounded">
                <legend className="text-lg font-semibold px-2">Payment Method (Mock)</legend>
                 {/* Card Type */}
                 <div className="mb-3">
                   <label htmlFor="paymentCardType" className="block mb-1 font-medium text-sm text-gray-700">Card Type:</label>
                   <input id="paymentCardType" name="paymentCardType" type="text" placeholder="e.g., Visa" value={formData.paymentCardType} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                </div>
                 {/* Last 4, Expiry */}
                 <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                     <div>
                        <label htmlFor="paymentLast4Digits" className="block mb-1 font-medium text-sm text-gray-700">Last 4 Digits:</label>
                        <input id="paymentLast4Digits" name="paymentLast4Digits" type="text" maxLength={4} value={formData.paymentLast4Digits} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                     <div>
                        <label htmlFor="paymentExpiryMonth" className="block mb-1 font-medium text-sm text-gray-700">Expiry Month (MM):</label>
                        <input id="paymentExpiryMonth" name="paymentExpiryMonth" type="text" maxLength={2} placeholder="MM" value={formData.paymentExpiryMonth} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                     <div>
                        <label htmlFor="paymentExpiryYear" className="block mb-1 font-medium text-sm text-gray-700">Expiry Year (YYYY):</label>
                        <input id="paymentExpiryYear" name="paymentExpiryYear" type="text" maxLength={4} placeholder="YYYY" value={formData.paymentExpiryYear} onChange={handleChange} className="w-full p-2 border rounded border-gray-300" />
                    </div>
                 </div>
            </fieldset>

           {/* Buttons */}
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