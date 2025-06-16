import React, { useState, useEffect, useCallback } from "react";
import { useKeycloak } from "@react-keycloak/web";
import apiClient from "../api/apiClient";
import { loadStripe } from "@stripe/stripe-js";

import ConfirmationModal from "../components/ConfirmationModal";
import EditProfileModal from "../components/EditProfileModal";
import StripeWrappedSetupFormModal from "../components/StripeSetupFormModal";

import UserProfileInfoSection from "../components/user/UserProfileInfoSection";
import UserAddressSection from "../components/user/UserAddressSection";
import UserPaymentMethodSection from "../components/user/UserPaymentMethodSection";
import UserSellerSection from "../components/user/UserSellerSection";
import UserBanStatusSection from "../components/user/UserBanStatusSection";

const STRIPE_PUBLISHABLE_KEY =
  "pk_test_51RN788QoAglQPjjvhupJXkisXj7R7wt7epc8hYTUbDBTCxumwAownPBKNMM8NfNVza13yVVf6SrfAnmAxoiJtfRw00cIVf2LIl";
const stripePromise = loadStripe(STRIPE_PUBLISHABLE_KEY);

function UserInfoPage() {
  const { keycloak, initialized } = useKeycloak();

  // State for profile data
  const [profileData, setProfileData] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState("");

  // State for seller activation
  const [isSellerActivating, setIsSellerActivating] = useState(false);
  const [sellerActivationError, setSellerActivationError] = useState("");
  const [sellerActivationSuccess, setSellerActivationSuccess] = useState("");
  const [isConfirmationModalOpen, setIsConfirmationModalOpen] = useState(false);

  // State for Edit Profile Modal
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editError, setEditError] = useState("");
  const [editSuccess, setEditSuccess] = useState("");

  // State for Payment Method
  const [isAddingPaymentMethod, setIsAddingPaymentMethod] = useState(false);
  const [paymentMethodError, setPaymentMethodError] = useState("");
  const [paymentMethodSuccess, setPaymentMethodSuccess] = useState("");
  const [setupIntentClientSecret, setSetupIntentClientSecret] = useState(null);
  const [isStripeSetupModalOpen, setIsStripeSetupModalOpen] = useState(false);

  const [isAvatarUploading, setIsAvatarUploading] = useState(false);
  const [avatarUploadError, setAvatarUploadError] = useState("");

  const CLOUDINARY_CLOUD_NAME = "dkw4hauo9"; // Your Cloudinary cloud name
  const CLOUDINARY_UPLOAD_PRESET = "auction_preset"; // Your Upload Preset NAME
  const CLOUDINARY_UPLOAD_URL = `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`;

  const fetchProfile = useCallback(async () => {
    if (initialized && keycloak.authenticated) {
      setProfileError("");
      try {
        await keycloak.updateToken(5);
        setProfileLoading(true);
        const response = await apiClient.get("/users/me");
        setProfileData(response.data);
        setPaymentMethodError("");
        setPaymentMethodSuccess("");
        setEditError(""); // Clear edit errors on profile refresh
        setEditSuccess("");
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
      setProfileLoading(true); // Still initializing
    }
  }, [initialized, keycloak]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleAvatarUpload = async (file) => {
    if (!file) return;

    setIsAvatarUploading(true);
    setAvatarUploadError("");

    const formData = new FormData();
    formData.append("file", file);
    formData.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);

    try {
      // Step 1: Upload to Cloudinary
      console.log("Uploading avatar to Cloudinary...");
      const cloudinaryResponse = await fetch(CLOUDINARY_UPLOAD_URL, {
        method: "POST",
        body: formData,
      });

      if (!cloudinaryResponse.ok) {
        const errorData = await cloudinaryResponse.json();
        throw new Error(errorData.error?.message || "Cloudinary upload failed");
      }
      const cloudinaryData = await cloudinaryResponse.json();
      const newAvatarUrl = cloudinaryData.secure_url;
      console.log("Avatar uploaded to Cloudinary:", newAvatarUrl);

      // Step 2: Update backend with the new Cloudinary URL
      await keycloak.updateToken(5); // Ensure token is fresh
      const backendResponse = await apiClient.put("/users/me/avatar", {
        avatarUrl: newAvatarUrl,
      });

      setProfileData(backendResponse.data); // Assuming backend returns the updated UserDto

      setEditSuccess("Avatar updated successfully!"); // Reuse editSuccess or add avatarSuccess
      setTimeout(() => setEditSuccess(""), 3000);
    } catch (err) {
      console.error("Avatar update process failed:", err);
      const errorMessage =
        err?.response?.data?.message ||
        err.message ||
        "Failed to update avatar.";
      setAvatarUploadError(errorMessage);
      setTimeout(() => setAvatarUploadError(""), 5000);
    } finally {
      setIsAvatarUploading(false);
    }
  };

  // --- Seller Activation Handlers ---
  const promptBecomeSeller = () => {
    setSellerActivationError(""); // Clear previous errors
    setSellerActivationSuccess("");

    // Prerequisite checks
    if (!profileData) {
      setSellerActivationError(
        "Profile data is not loaded yet. Please wait or refresh."
      );
      return;
    }

    const missingFields = [];
    if (!profileData.firstName) missingFields.push("First Name");
    if (!profileData.lastName) missingFields.push("Last Name");
    if (!profileData.phoneNumber) missingFields.push("Phone Number");
    if (
      !profileData.streetAddress ||
      !profileData.city ||
      !profileData.postalCode ||
      !profileData.country
    ) {
      missingFields.push(
        "a complete address (Street, City, Postal Code, Country)"
      );
    }

    const hasPaymentMethod =
      profileData.hasDefaultPaymentMethod ||
      (profileData.stripeDefaultPaymentMethodId &&
        profileData.stripeDefaultPaymentMethodId.trim() !== "");

    if (!hasPaymentMethod) {
      missingFields.push(
        "a saved payment method (please add one via the 'Payment Method' section)"
      );
    }

    if (missingFields.length > 0) {
      setSellerActivationError(
        `Please complete your profile before becoming a seller. You are missing: ${missingFields.join(
          ", "
        )}. You can update these details using the 'Edit Profile' button.`
      );
      return; // Prevent modal from opening
    }

    // If all checks pass, open the confirmation modal
    setIsConfirmationModalOpen(true);
  };

  const handleConfirmBecomeSeller = async () => {
    setIsConfirmationModalOpen(false);
    setIsSellerActivating(true);
    setSellerActivationError("");
    setSellerActivationSuccess("");
    try {
      await apiClient.post("/users/me/activate-seller");
      setSellerActivationSuccess(
        "Account successfully upgraded! Reloading page..."
      );
      setProfileData((prevData) => ({
        ...prevData,
        isSeller: true,
        seller: true,
      })); // Ensure 'seller' field is also updated if used
      await keycloak.updateToken(-1); // Force refresh of Keycloak token that might contain roles
      setTimeout(() => {
        window.location.reload(); // Reload to ensure all state (especially Keycloak roles) is fresh
      }, 1500);
    } catch (err) {
      console.error("Seller activation failed:", err);
      setSellerActivationError(
        err.response?.data?.message || "Failed to upgrade account."
      );
      setIsSellerActivating(false);
    }
  };

  // --- Edit Profile Handlers ---
  const handleOpenEditModal = () => {
    setEditError("");
    setEditSuccess("");
    setIsEditModalOpen(true);
  };

  const handleSaveProfile = async (updatedData) => {
    setEditError("");
    setEditSuccess("");
    try {
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

      if (profileData && (profileData.isSeller || profileData.seller)) {
        // Check if sellerDescription was part of formDataFromModal (it will be if the field was rendered)
        if (typeof updatedData.sellerDescription === "string") {
          payload.sellerDescription = updatedData.sellerDescription;
        }
      }
      
      const response = await apiClient.put("/users/me", payload);
      setProfileData(response.data);
      setEditSuccess("Profile updated successfully!");
      setIsEditModalOpen(false);
    } catch (err) {
      console.error("Failed to update profile:", err);
      setEditError(err.response?.data?.message || "Failed to update profile.");
      return Promise.reject(err);
    }
  };

  // --- Payment Method Handlers ---
  const handleAddOrUpdatePaymentMethod = async () => {
    setIsAddingPaymentMethod(true);
    setPaymentMethodError("");
    setPaymentMethodSuccess("");
    setSetupIntentClientSecret(null);
    try {
      const response = await apiClient.post(
        "/users/me/payment-method/setup-intent-secret"
      );
      if (response.data && response.data.clientSecret) {
        setSetupIntentClientSecret(response.data.clientSecret);
        setIsStripeSetupModalOpen(true);
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
    setIsAddingPaymentMethod(true);
    setPaymentMethodError("");
    setPaymentMethodSuccess("");
    try {
      const response = await apiClient.post(
        "/users/me/payment-method/confirm-setup",
        { stripePaymentMethodId }
      );
      setPaymentMethodSuccess(
        response.data?.message || "Payment method saved successfully!"
      );
      fetchProfile(); // Refresh profile to show new payment method and potentially updated stripeCustomerId
    } catch (err) {
      console.error("Failed to confirm payment method with backend:", err);
      setPaymentMethodError(
        err.response?.data?.message || "Could not save payment method."
      );
    } finally {
      setIsAddingPaymentMethod(false);
      setIsStripeSetupModalOpen(false);
      setSetupIntentClientSecret(null);
    }
  };

  const handleStripeSetupError = (errorMessage) => {
    setPaymentMethodError(
      errorMessage || "Failed to set up payment method with Stripe."
    );
    setIsAddingPaymentMethod(false);
  };

  if (!initialized || profileLoading) {
    return <div className="text-center p-10">Loading...</div>;
  }

  if (!keycloak.authenticated) {
    return (
      <div className="text-center p-10 text-red-600">
        Please log in to view your profile.
      </div>
    );
  }


  return (
    <div className="bg-slate-50 min-h-screen"> {/* Added a light background to the whole page */}
      <div className="container mx-auto p-4 md:p-8">
        {profileData &&  profileData.banEndsAt && (
          <UserBanStatusSection banEndsAt={profileData.banEndsAt} />
        )}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8">
          <h1 className="text-3xl font-bold text-slate-800 mb-4 md:mb-0">
            User Profile & Settings
          </h1>
          {profileData && !profileError && (
            <button
              onClick={handleOpenEditModal}
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 px-5 rounded-lg text-sm shadow-md hover:shadow-lg transition-all duration-150 ease-in-out"
            >
              Edit Profile
            </button>
          )}
        </div>

        {isAvatarUploading && (
          <p className="text-center text-indigo-600 my-3 p-3 bg-indigo-100 rounded-lg">
            Uploading avatar...
          </p>
        )}
        {avatarUploadError && (
          <p className="text-center text-red-600 my-3 bg-red-100 p-3 rounded-lg">
            {avatarUploadError}
          </p>
        )}
        {editSuccess && ( // Show general edit success message here for broader visibility
          <p className="text-center text-green-600 my-3 bg-green-100 p-3 rounded-lg">
            {editSuccess}
          </p>
        )}

        {profileError && !profileData && (
          <p className="text-red-600 mb-6 text-center bg-red-100 p-4 rounded-lg shadow">
            Error loading profile details: {profileError}
          </p>
        )}

        {profileData ? (
          <div className="space-y-8"> {/* Consistent spacing for all main sections */}
            <UserProfileInfoSection
              profileData={profileData}
              onAvatarUpload={handleAvatarUpload}
            />

            {/* Grid layout for Address, Payment, and Seller sections */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              <div className="lg:col-span-1"> {/* Or lg:col-span-2 for wider address */}
                <UserAddressSection profileData={profileData} />
              </div>

              {/* Payment and Seller Sections - stacked in the remaining column(s) */}
              <div className="lg:col-span-2 space-y-8"> 
                <UserPaymentMethodSection
                  profileData={profileData}
                  onAddOrUpdatePaymentMethod={handleAddOrUpdatePaymentMethod}
                  isAddingPaymentMethod={isAddingPaymentMethod}
                  paymentMethodError={paymentMethodError}
                  paymentMethodSuccess={paymentMethodSuccess}
                  stripePromise={stripePromise}
                />
                <UserSellerSection
                  isSeller={profileData.seller || profileData.isSeller}
                  onPromptBecomeSeller={promptBecomeSeller}
                  isSellerActivating={isSellerActivating}
                  sellerActivationError={sellerActivationError}
                  sellerActivationSuccess={sellerActivationSuccess}
                />
              </div>
            </div>
          </div>
        ) : (
          !profileLoading &&
          !profileError && (
            <p className="text-center text-slate-600 p-10">
              Could not load profile information.
            </p>
          )
        )}

        {/* Modals */}
        <ConfirmationModal
          isOpen={isConfirmationModalOpen}
          onClose={() => setIsConfirmationModalOpen(false)}
          onConfirm={handleConfirmBecomeSeller}
          title="Become a Seller?"
          message="Do you want to upgrade your account to gain seller privileges?"
          isLoading={isSellerActivating}
          error={sellerActivationError} // Pass error to be displayed in modal
          confirmText="Yes, Upgrade"
          confirmButtonClass="bg-purple-600 hover:bg-purple-700" // Keep purple for this distinct action
        />
        {profileData && isEditModalOpen && (
          <EditProfileModal
            isOpen={isEditModalOpen}
            onClose={() => setIsEditModalOpen(false)}
            onSave={handleSaveProfile}
            initialData={profileData}
            error={editError} // Pass error to be displayed in modal
          />
        )}
        {isStripeSetupModalOpen && setupIntentClientSecret && stripePromise && (
          <StripeWrappedSetupFormModal
            isOpen={isStripeSetupModalOpen}
            onClose={() => {
              setIsStripeSetupModalOpen(false);
              setSetupIntentClientSecret(null);
              setPaymentMethodError(""); 
            }}
            clientSecret={setupIntentClientSecret}
            onSuccess={handleStripeSetupSuccess}
            onError={handleStripeSetupError}
            stripePromise={stripePromise}
          />
        )}
      </div>
    </div>
  );
}

export default UserInfoPage;
