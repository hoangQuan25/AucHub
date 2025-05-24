import React from 'react';

// You might want to create a dedicated Avatar component later if it gets more complex
function UserProfileInfoSection({ profileData, onAvatarUpload }) { // Added onAvatarUpload prop
  if (!profileData) return null;

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file && onAvatarUpload) {
      onAvatarUpload(file);
      event.target.value = null; // Reset file input
    }
  };

  return (
    <div className="mb-6 p-4 border rounded bg-white shadow-sm">
      <div className="flex items-start space-x-4">
        {/* Avatar Display and Upload */}
        <div className="flex-shrink-0">
          {profileData.avatarUrl ? (
            <img
              src={profileData.avatarUrl}
              alt="User Avatar"
              className="h-24 w-24 rounded-full object-cover border border-gray-300 shadow-sm"
            />
          ) : (
            <div className="h-24 w-24 rounded-full bg-gray-200 flex items-center justify-center text-gray-500 text-sm border">
              No Avatar
            </div>
          )}
          <label
            htmlFor="avatarUpload"
            className="mt-2 block text-sm text-center text-indigo-600 hover:text-indigo-500 cursor-pointer"
          >
            Change Avatar
            <input
              type="file"
              id="avatarUpload"
              name="avatarUpload"
              className="sr-only" // Hide default input, style the label
              accept="image/png, image/jpeg, image/gif"
              onChange={handleFileChange}
            />
          </label>
        </div>

        {/* User Information Details */}
        <div className="flex-grow">
          <h3 className="text-lg font-semibold mb-3 border-b pb-2">
            Your Information
          </h3>
          <p>
            <strong>Username:</strong> {profileData.username}
          </p>
          <p>
            <strong>Email:</strong> {profileData.email}
          </p>
          <p>
            <strong>First Name:</strong> {profileData.firstName || '(Not set)'}
          </p>
          <p>
            <strong>Last Name:</strong> {profileData.lastName || '(Not set)'}
          </p>
          <p>
            <strong>Phone:</strong> {profileData.phoneNumber || '(Not set)'}
          </p>
        </div>
      </div>
    </div>
  );
}

export default UserProfileInfoSection;