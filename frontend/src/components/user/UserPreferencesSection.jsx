import React from "react";

const ToggleSwitch = ({ isEnabled, onToggle, isDisabled }) => (
  <button
    type="button"
    onClick={onToggle}
    disabled={isDisabled}
    className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 ${
      isEnabled ? "bg-indigo-600" : "bg-gray-200"
    } ${isDisabled ? "cursor-not-allowed opacity-50" : ""}`}
    aria-pressed={isEnabled}
  >
    <span
      aria-hidden="true"
      className={`inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
        isEnabled ? "translate-x-5" : "translate-x-0"
      }`}
    />
  </button>
);


const UserPreferencesSection = ({
  emailNotificationsEnabled,
  onToggleEmailNotifications,
  isSaving,
  error,
  success,
}) => {
  return (
    <div className="bg-white p-6 rounded-lg shadow-md">
      <h3 className="text-lg font-semibold text-slate-800 mb-4 border-b pb-3">
        Notification Preferences
      </h3>

      <div className="flex items-center justify-between">
        <div>
          <p className="font-medium text-slate-700">Email Notifications</p>
          <p className="text-sm text-slate-500">
            Receive emails about auction status, outbids, and wins.
          </p>
        </div>
        <div className="flex items-center">
            {isSaving && <span className="text-sm text-slate-500 mr-3">Saving...</span>}
            <ToggleSwitch 
                isEnabled={emailNotificationsEnabled}
                onToggle={onToggleEmailNotifications}
                isDisabled={isSaving}
            />
        </div>
      </div>
       {error && <p className="text-sm text-red-600 mt-2">{error}</p>}
       {success && <p className="text-sm text-green-600 mt-2">{success}</p>}
    </div>
  );
};

export default UserPreferencesSection;