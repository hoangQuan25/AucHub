import React from 'react';

function UserProfileInfoSection({ profileData, onAvatarUpload, isAvatarUploading }) { // Added isAvatarUploading if you want to disable button
  if (!profileData) return null;

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (file && onAvatarUpload) {
      onAvatarUpload(file);
      event.target.value = null; // Reset file input
    }
  };

  return (
    // This section already has a modern style, keeping it as the standout "hero" for user info.
    // bg-gradient-to-br from-indigo-50 via-white to-purple-50 is a good choice for a prominent section.
    <div className="p-6 md:p-8 rounded-2xl bg-gradient-to-br from-indigo-100 via-white to-purple-100 shadow-xl border border-indigo-200">
      <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6 md:gap-8">
        {/* Avatar Display and Upload */}
        <div className="flex flex-col items-center flex-shrink-0">
          {profileData.avatarUrl ? (
            <img
              src={profileData.avatarUrl}
              alt="User Avatar"
              className="h-32 w-32 rounded-full object-cover border-4 border-white shadow-lg transition-transform duration-300 hover:scale-105"
            />
          ) : (
            <div className="h-32 w-32 rounded-full bg-slate-200 flex items-center justify-center text-slate-400 text-3xl border-4 border-white shadow-lg">
              {/* You can use initials here if available */}
              <span>{profileData.username ? profileData.username.charAt(0).toUpperCase() : '?'}</span>
            </div>
          )}
          <label
            htmlFor="avatarUpload"
            className={`mt-4 px-5 py-2 ${isAvatarUploading ? 'bg-slate-400' : 'bg-indigo-600 hover:bg-indigo-700'} text-white rounded-lg text-sm font-medium shadow-md hover:shadow-lg cursor-pointer transition-all duration-150 ease-in-out`}
          >
            {isAvatarUploading ? 'Uploading...' : 'Change Avatar'}
            <input
              type="file"
              id="avatarUpload"
              name="avatarUpload"
              className="sr-only"
              accept="image/png, image/jpeg, image/gif"
              onChange={handleFileChange}
              disabled={isAvatarUploading}
            />
          </label>
        </div>

        {/* User Information Details */}
        <div className="flex-grow w-full mt-4 sm:mt-0">
          <h3 className="text-2xl font-bold mb-5 text-indigo-800 border-b border-indigo-200 pb-3">
            Your Information
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-3 text-sm md:text-base">
            <div>
              <p className="mb-2">
                <span className="font-semibold text-slate-700">Username:</span>{" "}
                <span className="text-slate-900">{profileData.username}</span>
              </p>
              <p className="mb-2">
                <span className="font-semibold text-slate-700">Email:</span>{" "}
                <span className="text-slate-900">{profileData.email}</span>
              </p>
              <p className="mb-2">
                <span className="font-semibold text-slate-700">First Name:</span>{" "}
                <span className="text-slate-900">{profileData.firstName || <span className="italic text-slate-500">(Not set)</span>}</span>
              </p>
            </div>
            <div>
              <p className="mb-2">
                <span className="font-semibold text-slate-700">Last Name:</span>{" "}
                <span className="text-slate-900">{profileData.lastName || <span className="italic text-slate-500">(Not set)</span>}</span>
              </p>
              <p className="mb-2">
                <span className="font-semibold text-slate-700">Phone:</span>{" "}
                <span className="text-slate-900">{profileData.phoneNumber || <span className="italic text-slate-500">(Not set)</span>}</span>
              </p>
              {/* Example of adding more info if available */}
              {/* <p className="mb-2">
                <span className="font-semibold text-slate-700">Member Since:</span>{" "}
                <span className="text-slate-900">{new Date(profileData.createdAt).toLocaleDateString() || <span className="italic text-slate-500">(Not set)</span>}</span>
              </p> */}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default UserProfileInfoSection;