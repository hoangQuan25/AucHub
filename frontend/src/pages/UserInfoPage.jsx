import React, { useState, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import apiClient from '../api/apiClient'; // Adjust path if needed
import ConfirmationModal from '../components/ConfirmationModal'; // For seller activation
import EditProfileModal from '../components/EditProfileModal'; // You will create this modal

function UserInfoPage() {
  const { keycloak, initialized } = useKeycloak();

  // State for profile data (now includes address/payment directly)
  const [profileData, setProfileData] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');

  // State for seller activation
  const [isSellerActivating, setIsSellerActivating] = useState(false);
  const [sellerActivationError, setSellerActivationError] = useState('');
  const [sellerActivationSuccess, setSellerActivationSuccess] = useState('');
  const [isConfirmationModalOpen, setIsConfirmationModalOpen] = useState(false);

  // State for the new Edit Profile Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editError, setEditError] = useState('');
  const [editSuccess, setEditSuccess] = useState('');


  // --- Fetch User Profile on Load/Auth Change (with updateToken fix) ---
  useEffect(() => {
    const fetchProfile = async () => {
      if (initialized && keycloak.authenticated) {
        console.log("useEffect: Initialized & Authenticated.");
        // Don't set loading true here initially, wait for token check
        setProfileError('');
        try {
          // Ensure token is ready before API call, especially on reload
          console.log("useEffect: Ensuring token is up-to-date...");
          await keycloak.updateToken(5); // Check validity for 5s, refresh if needed
          console.log("useEffect: Token ready, fetching profile data...");
          setProfileLoading(true); // Set loading true just before fetch

          const response = await apiClient.get('/users/me'); // Use await with async func
          console.log("useEffect: Profile data received:", response.data);
          setProfileData(response.data);

        } catch (err) {
          console.error("useEffect: Failed during token update or profile fetch:", err);
          setProfileError(err?.response?.data?.message || err?.response?.data || err?.message || 'Failed to load profile data.');
        } finally {
           // Always set loading false after attempt completes
          setProfileLoading(false);
        }
      } else if (initialized) {
        console.log("useEffect: Initialized but not authenticated.");
        setProfileLoading(false); // Stop loading if not auth'd
        setProfileError("User is not authenticated. Please log in.");
      } else {
        console.log("useEffect: Keycloak not initialized yet.");
         setProfileLoading(true); // Keep loading true until initialized
      }
    };

    fetchProfile();
  }, [initialized, keycloak.authenticated]); // Re-run when these change

  // --- Seller Activation Handlers --- (Keep as before, using correct state setters)
  const promptBecomeSeller = () => {
    setSellerActivationError('');
    setSellerActivationSuccess('');
    setIsConfirmationModalOpen(true);
  };

  const handleConfirmBecomeSeller = async () => {
    // ... (same logic as before, including reload) ...
     setIsConfirmationModalOpen(false);
     setIsSellerActivating(true);
     setSellerActivationError("");
     setSellerActivationSuccess("");
     try {
       await apiClient.post("/users/me/activate-seller");
       setSellerActivationSuccess( "Account successfully upgraded! Reloading page...");
       // Update local state - though reload makes this temporary
       setProfileData((prevData) => ({ ...prevData, isSeller: true }));
       await keycloak.updateToken(-1);
       console.log("Token refresh requested after seller activation.");
       setTimeout(() => { window.location.reload(); }, 1000);
     } catch (err) {
       console.error("Seller activation failed:", err);
       setSellerActivationError( err.response?.data?.message || "Failed to upgrade account." );
       setIsSellerActivating(false); // Only stop loading on error if not reloading
     }
     // No finally needed if reloading on success
  };

  // --- Edit Profile Handlers ---
  const handleOpenEditModal = () => {
    setEditError('');
    setEditSuccess('');
    setIsEditModalOpen(true);
  };

  const handleSaveProfile = async (updatedData) => {
    console.log("Attempting to save profile:", updatedData);
    setEditError('');
    setEditSuccess('');
    try {
      // Send only the fields present in UpdateUserDto
      const payload = {
         firstName: updatedData.firstName,
         lastName: updatedData.lastName,
         phoneNumber: updatedData.phoneNumber,
         streetAddress: updatedData.streetAddress,
         city: updatedData.city,
         stateProvince: updatedData.stateProvince,
         postalCode: updatedData.postalCode,
         country: updatedData.country,
         paymentCardType: updatedData.paymentCardType,
         paymentLast4Digits: updatedData.paymentLast4Digits,
         paymentExpiryMonth: updatedData.paymentExpiryMonth,
         paymentExpiryYear: updatedData.paymentExpiryYear,
      };
      const response = await apiClient.put('/users/me', payload);
      setProfileData(response.data); // Update profile data with response from backend
      setEditSuccess('Profile updated successfully!');
      setIsEditModalOpen(false); // Close modal on success
    } catch (err) {
        console.error("Failed to update profile:", err);
        setEditError(err.response?.data?.message || 'Failed to update profile.');
        // Keep modal open on error
        return Promise.reject(err); // Signal error to modal if needed
    }
  };


  // --- Derived State ---
  // Determine seller status using fetched data if available
   const isSeller = profileData?.seller ?? false; // Use profileData.isSeller if profileData exists, otherwise false

  // --- Render Logic ---
  if (!initialized || profileLoading) {
    return <div className="text-center p-10">Loading...</div>;
  }

  // Separate error display for profile loading vs edits vs seller activation
  if (!profileData && profileError) {
     return <div className="text-center p-10 text-red-600"> Error loading profile: {profileError} </div>;
  }


  return (
    <div>
      <div className="flex justify-between items-center mb-6">
         <h2 className="text-2xl font-bold">User Profile & Settings</h2>
         {/* Add Edit button only if profile data loaded successfully */}
         {profileData && !profileError && (
              <button
                onClick={handleOpenEditModal}
                className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 px-4 rounded text-sm transition duration-150 ease-in-out"
              >
                Edit Profile
              </button>
         )}
      </div>

      {/* Display Specific Profile Load Error if it occurred */}
      {profileError && <p className="text-red-500 mb-4 text-center">Error loading profile details: {profileError}</p>}


      {/* Display User Info Section (only if profileData exists) */}
      {profileData && (
         <>
            <div className="mb-6 p-4 border rounded bg-white shadow-sm">
               <h3 className="text-lg font-semibold mb-3 border-b pb-2"> Your Information </h3>
               <p><strong>Username:</strong> {profileData.username}</p>
               <p><strong>Email:</strong> {profileData.email}</p>
               <p><strong>First Name:</strong> {profileData.firstName || '(Not set)'}</p>
               <p><strong>Last Name:</strong> {profileData.lastName || '(Not set)'}</p>
               <p><strong>Phone:</strong> {profileData.phoneNumber || '(Not set)'}</p>
            </div>

            {/* Display Address */}
            <div className="mb-6 p-4 border rounded bg-white shadow-sm">
               <h3 className="text-lg font-semibold mb-3 border-b pb-2"> Address </h3>
               {profileData.streetAddress ? (
                 <>
                   <p><strong>Street:</strong> {profileData.streetAddress}</p>
                   <p><strong>City:</strong> {profileData.city}</p>
                   <p><strong>State/Province:</strong> {profileData.stateProvince || '(Not set)'}</p>
                   <p><strong>Postal Code:</strong> {profileData.postalCode}</p>
                   <p><strong>Country:</strong> {profileData.country}</p>
                 </>
               ) : (
                 <p className="text-gray-600 text-sm">No address saved yet.</p>
               )}
            </div>

            {/* Display Payment Method */}
            <div className="mb-6 p-4 border rounded bg-white shadow-sm">
               <h3 className="text-lg font-semibold mb-3 border-b pb-2"> Payment Method </h3>
               {profileData.paymentLast4Digits ? (
                 <p>
                   <strong>Card:</strong> {profileData.paymentCardType || 'N/A'} ending in **** {profileData.paymentLast4Digits}
                   <span className="text-gray-600"> (Exp: {profileData.paymentExpiryMonth || 'MM'}/{profileData.paymentExpiryYear || 'YYYY'})</span>
                 </p>
               ) : (
                 <p className="text-gray-600 text-sm">No payment method saved yet.</p>
               )}
            </div>
         </>
      )}


      {/* Seller Status Section */}
      <div className="mt-6 p-4 border rounded bg-white shadow-sm">
        <h3 className="text-lg font-semibold mb-2 border-b pb-1"> Seller Status </h3>
        {sellerActivationError && <p className="text-red-500 mb-2 text-sm">{sellerActivationError}</p>}
        {sellerActivationSuccess && <p className="text-green-500 mb-2 text-sm">{sellerActivationSuccess}</p>}

        {/* Use isSeller derived from profileData */}
        {isSeller ? (
          <p className="text-green-700 font-medium"> ✔ You are registered as a Seller. </p>
        ) : (
          <div>
            <p className="mb-3 text-sm text-gray-700"> Upgrade your account to list items and start selling on AucHub. </p>
            <button
              onClick={promptBecomeSeller}
              disabled={isSellerActivating}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50 transition duration-150 ease-in-out"
            >
              {isSellerActivating ? 'Processing...' : 'Become a Seller'}
            </button>
          </div>
        )}
      </div>

      {/* Modals */}
      <ConfirmationModal
        isOpen={isConfirmationModalOpen}
        onClose={() => setIsConfirmationModalOpen(false)}
        onConfirm={handleConfirmBecomeSeller}
        title="Become a Seller?"
        message="Do you want to upgrade your account to gain seller privileges?"
      />

      {/* Edit Profile Modal */}
      {profileData && ( // Only render modal if profile data is available to edit
         <EditProfileModal
             isOpen={isEditModalOpen}
             onClose={() => setIsEditModalOpen(false)}
             onSave={handleSaveProfile} // Pass the save handler
             initialData={profileData} // Pass current profile data
             error={editError} // Pass error state to modal
             success={editSuccess} // Pass success state to modal
         />
      )}

    </div>
  );
}

export default UserInfoPage;