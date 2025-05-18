import React, { useState, useEffect, useCallback } from "react";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient"; // Adjust path if needed
import ConfirmationModal from "../components/ConfirmationModal"; // For seller activation
import EditProfileModal from "../components/EditProfileModal"; // You will create this modal
import { loadStripe } from "@stripe/stripe-js";
import StripeWrappedSetupFormModal from "../components/StripeSetupFormModal";

const STRIPE_PUBLISHABLE_KEY =
  "pk_test_51RN788QoAglQPjjvhupJXkisXj7R7wt7epc8hYTUbDBTCxumwAownPBKNMM8NfNVza13yVVf6SrfAnmAxoiJtfRw00cIVf2LIl";
const stripePromise = loadStripe(STRIPE_PUBLISHABLE_KEY);

function UserInfoPage() {
  const { keycloak, initialized } = useKeycloak();

  // State for profile data (now includes address/payment directly)
  const [profileData, setProfileData] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState("");

  // State for seller activation
  const [isSellerActivating, setIsSellerActivating] = useState(false);
  const [sellerActivationError, setSellerActivationError] = useState("");
  const [sellerActivationSuccess, setSellerActivationSuccess] = useState("");
  const [isConfirmationModalOpen, setIsConfirmationModalOpen] = useState(false);

  // State for the new Edit Profile Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editError, setEditError] = useState("");
  const [editSuccess, setEditSuccess] = useState("");

  const [isAddingPaymentMethod, setIsAddingPaymentMethod] = useState(false); // To show a loader or modal
  const [paymentMethodError, setPaymentMethodError] = useState("");
  const [paymentMethodSuccess, setPaymentMethodSuccess] = useState("");
  const [setupIntentClientSecret, setSetupIntentClientSecret] = useState(null);
  const [isStripeSetupModalOpen, setIsStripeSetupModalOpen] = useState(false);

  const fetchProfile = useCallback(async () => {
    if (initialized && keycloak.authenticated) {
      setProfileError("");
      try {
        await keycloak.updateToken(5);
        setProfileLoading(true);
        const response = await apiClient.get("/users/me");
        setProfileData(response.data);
        setPaymentMethodError(""); // Clear payment method errors on profile refresh
        setPaymentMethodSuccess("");
      } catch (err) {
        console.error("Failed during token update or profile fetch:", err);
        setProfileError(
          err?.response?.data?.message ||
            err?.message ||
            "Failed to load profile data."
        );
      } finally {
        setProfileLoading(false);
      }
    } else if (initialized) {
      setProfileLoading(false);
      setProfileError("User is not authenticated. Please log in.");
    } else {
      setProfileLoading(true);
    }
  }, [initialized, keycloak]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  // --- Seller Activation Handlers --- (Keep as before, using correct state setters)
  const promptBecomeSeller = () => {
    setSellerActivationError("");
    setSellerActivationSuccess("");
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
      setSellerActivationSuccess(
        "Account successfully upgraded! Reloading page..."
      );
      // Update local state - though reload makes this temporary
      setProfileData((prevData) => ({ ...prevData, isSeller: true }));
      await keycloak.updateToken(-1);
      console.log("Token refresh requested after seller activation.");
      setTimeout(() => {
        window.location.reload();
      }, 1000);
    } catch (err) {
      console.error("Seller activation failed:", err);
      setSellerActivationError(
        err.response?.data?.message || "Failed to upgrade account."
      );
      setIsSellerActivating(false); // Only stop loading on error if not reloading
    }
    // No finally needed if reloading on success
  };

  // --- Edit Profile Handlers ---
  const handleOpenEditModal = () => {
    setEditError("");
    setEditSuccess("");
    setIsEditModalOpen(true);
  };

  const handleSaveProfile = async (updatedData) => {
    console.log("Attempting to save profile:", updatedData);
    setEditError("");
    setEditSuccess("");
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
      };
      const response = await apiClient.put("/users/me", payload);
      setProfileData(response.data); // Update profile data with response from backend
      setEditSuccess("Profile updated successfully!");
      setIsEditModalOpen(false); // Close modal on success
    } catch (err) {
      console.error("Failed to update profile:", err);
      setEditError(err.response?.data?.message || "Failed to update profile.");
      // Keep modal open on error
      return Promise.reject(err); // Signal error to modal if needed
    }
  };

  const handleAddOrUpdatePaymentMethod = async () => {
    setIsAddingPaymentMethod(true);
    setPaymentMethodError("");
    setPaymentMethodSuccess("");
    setSetupIntentClientSecret(null); // Reset previous secret

    try {
      console.log("Requesting SetupIntent secret from backend...");
      // Backend endpoint to get client_secret for SetupIntent
      const response = await apiClient.post(
        "/users/me/payment-method/setup-intent-secret"
      );

      if (response.data && response.data.clientSecret) {
        console.log("Received client_secret:", response.data.clientSecret);
        setSetupIntentClientSecret(response.data.clientSecret);
        setIsStripeSetupModalOpen(true); // Open the Stripe modal
      } else {
        throw new Error("Failed to get SetupIntent client secret from server.");
      }
    } catch (err) {
      console.error("Failed to initiate payment method setup:", err);
      setPaymentMethodError(
        err.response?.data?.message ||
          err.message ||
          "Could not start payment method setup."
      );
    } finally {
      setIsAddingPaymentMethod(false);
    }
  };

  const handleStripeSetupSuccess = async (stripePaymentMethodId) => {
    console.log(
      "Stripe Setup Succeeded. PaymentMethod ID:",
      stripePaymentMethodId
    );
    setIsAddingPaymentMethod(true); // Indicate processing for backend confirmation
    setPaymentMethodError("");
    setPaymentMethodSuccess("");

    try {
      // Call backend to confirm setup and save PaymentMethod ID
      const response = await apiClient.post(
        "/users/me/payment-method/confirm-setup",
        { stripePaymentMethodId }
      );
      setPaymentMethodSuccess(
        response.data?.message || "Payment method saved successfully!"
      );
      fetchProfile(); // Refresh profile to show new payment method
    } catch (err) {
      console.error("Failed to confirm payment method with backend:", err);
      setPaymentMethodError(
        err.response?.data?.message || "Could not save payment method."
      );
    } finally {
      setIsAddingPaymentMethod(false);
      setIsStripeSetupModalOpen(false); // Close modal regardless of backend confirmation outcome here
      setSetupIntentClientSecret(null); // Clear the secret
    }
  };

  const handleStripeSetupError = (errorMessage) => {
    setPaymentMethodError(
      errorMessage || "Failed to set up payment method with Stripe."
    );
    setIsAddingPaymentMethod(false);
    // Keep the Stripe modal open if desired, or close it:
    // setIsStripeSetupModalOpen(false);
    // setSetupIntentClientSecret(null);
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
    return (
      <div className="text-center p-10 text-red-600">
        {" "}
        Error loading profile: {profileError}{" "}
      </div>
    );
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
      {profileError && (
        <p className="text-red-500 mb-4 text-center">
          Error loading profile details: {profileError}
        </p>
      )}

      {/* Display User Info Section (only if profileData exists) */}
      {profileData ? (
        <>
          <div className="mb-6 p-4 border rounded bg-white shadow-sm">
            <h3 className="text-lg font-semibold mb-3 border-b pb-2">
              {" "}
              Your Information{" "}
            </h3>
            <p>
              <strong>Username:</strong> {profileData.username}
            </p>
            <p>
              <strong>Email:</strong> {profileData.email}
            </p>
            <p>
              <strong>First Name:</strong>{" "}
              {profileData.firstName || "(Not set)"}
            </p>
            <p>
              <strong>Last Name:</strong> {profileData.lastName || "(Not set)"}
            </p>
            <p>
              <strong>Phone:</strong> {profileData.phoneNumber || "(Not set)"}
            </p>
          </div>

          {/* Display Address */}
          <div className="mb-6 p-4 border rounded bg-white shadow-sm">
            <h3 className="text-lg font-semibold mb-3 border-b pb-2">
              {" "}
              Address{" "}
            </h3>
            {profileData.streetAddress ? (
              <>
                <p>
                  <strong>Street:</strong> {profileData.streetAddress}
                </p>
                <p>
                  <strong>City:</strong> {profileData.city}
                </p>
                <p>
                  <strong>State/Province:</strong>{" "}
                  {profileData.stateProvince || "(Not set)"}
                </p>
                <p>
                  <strong>Postal Code:</strong> {profileData.postalCode}
                </p>
                <p>
                  <strong>Country:</strong> {profileData.country}
                </p>
              </>
            ) : (
              <p className="text-gray-600 text-sm">No address saved yet.</p>
            )}
          </div>

          {/* Display Payment Method */}
          <div className="mb-6 p-4 border rounded bg-white shadow-sm">
            <div className="flex justify-between items-center mb-3 border-b pb-2">
              <h3 className="text-lg font-semibold">Payment Method</h3>
              <button
                onClick={handleAddOrUpdatePaymentMethod}
                disabled={isAddingPaymentMethod || !stripePromise} // Disable if Stripe.js not loaded
                className="bg-blue-500 hover:bg-blue-600 text-white text-xs font-semibold py-1 px-3 rounded disabled:opacity-50"
              >
                {isAddingPaymentMethod
                  ? "Processing..."
                  : profileData.hasDefaultPaymentMethod
                  ? "Update Method"
                  : "Add Payment Method"}
              </button>
            </div>
            {paymentMethodError && (
              <p className="text-red-500 text-sm mb-2">{paymentMethodError}</p>
            )}
            {paymentMethodSuccess && (
              <p className="text-green-500 text-sm mb-2">
                {paymentMethodSuccess}
              </p>
            )}

            {profileData.hasDefaultPaymentMethod &&
            profileData.defaultCardLast4 ? (
              <p>
                <strong>Default Card:</strong>{" "}
                {profileData.defaultCardBrand || "N/A"} ending in ****{" "}
                {profileData.defaultCardLast4}
                {profileData.defaultCardExpiryMonth &&
                  profileData.defaultCardExpiryYear && (
                    <span className="text-gray-600 text-sm">
                      {" "}
                      (Exp: {profileData.defaultCardExpiryMonth}/
                      {profileData.defaultCardExpiryYear})
                    </span>
                  )}
              </p>
            ) : (
              <p className="text-gray-600 text-sm">
                No default payment method saved. Click "Add Payment Method" to
                set one up for faster checkouts and bidding.
              </p>
            )}
          </div>
        </>
      ) : (
        !profileLoading && <p>Could not load profile information.</p> // Show if profileData is null and not loading, and no major error shown above
      )}

      {/* Seller Status Section */}
      <div className="mt-6 p-4 border rounded bg-white shadow-sm">
        <h3 className="text-lg font-semibold mb-2 border-b pb-1">
          {" "}
          Seller Status{" "}
        </h3>
        {sellerActivationError && (
          <p className="text-red-500 mb-2 text-sm">{sellerActivationError}</p>
        )}
        {sellerActivationSuccess && (
          <p className="text-green-500 mb-2 text-sm">
            {sellerActivationSuccess}
          </p>
        )}

        {/* Use isSeller derived from profileData */}
        {isSeller ? (
          <p className="text-green-700 font-medium">
            {" "}
            ✔ You are registered as a Seller.{" "}
          </p>
        ) : (
          <div>
            <p className="mb-3 text-sm text-gray-700">
              {" "}
              Upgrade your account to list items and start selling on AucHub.{" "}
            </p>
            <button
              onClick={promptBecomeSeller}
              disabled={isSellerActivating}
              className="bg-purple-600 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50 transition duration-150 ease-in-out"
            >
              {isSellerActivating ? "Processing..." : "Become a Seller"}
            </button>
          </div>
        )}
      </div>

      <ConfirmationModal
        isOpen={isConfirmationModalOpen}
        onClose={() => setIsConfirmationModalOpen(false)}
        onConfirm={handleConfirmBecomeSeller}
        title="Become a Seller?"
        message="Do you want to upgrade your account to gain seller privileges?"
        isLoading={isSellerActivating} // Pass loading state
        error={sellerActivationError} // Pass error state
        confirmText="Yes, Upgrade" // Customize button text
        confirmButtonClass="bg-purple-600 hover:bg-purple-700" // Customize style
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
      {isStripeSetupModalOpen && setupIntentClientSecret && stripePromise && (
        <StripeWrappedSetupFormModal
          isOpen={isStripeSetupModalOpen}
          onClose={() => {
            setIsStripeSetupModalOpen(false);
            setSetupIntentClientSecret(null); // Clear secret on close
            setPaymentMethodError(""); // Clear errors
          }}
          clientSecret={setupIntentClientSecret}
          onSuccess={handleStripeSetupSuccess}
          onError={handleStripeSetupError}
          stripePromise={stripePromise} // Pass stripePromise
        />
      )}
    </div>
  );
}

export default UserInfoPage;
