import React from 'react';

function UserAddressSection({ profileData }) {
  if (!profileData) return null;

  return (
    <div className="bg-white p-6 rounded-xl shadow-lg hover:shadow-xl transition-shadow duration-300 border border-gray-200 h-full">
      <h3 className="text-xl font-semibold text-slate-800 mb-4 pb-3 border-b border-gray-200">
        Shipping Address
      </h3>
      {profileData.streetAddress || profileData.city || profileData.country ? ( // Check if any part of address exists
        <div className="space-y-2 text-sm text-slate-700">
          <p>
            <span className="font-medium text-slate-600">Street:</span> {profileData.streetAddress || <span className="italic text-slate-500">(Not set)</span>}
          </p>
          <p>
            <span className="font-medium text-slate-600">City:</span> {profileData.city || <span className="italic text-slate-500">(Not set)</span>}
          </p>
          <p>
            <span className="font-medium text-slate-600">State/Province:</span> {profileData.stateProvince || <span className="italic text-slate-500">(Not set)</span>}
          </p>
          <p>
            <span className="font-medium text-slate-600">Postal Code:</span> {profileData.postalCode || <span className="italic text-slate-500">(Not set)</span>}
          </p>
          <p>
            <span className="font-medium text-slate-600">Country:</span> {profileData.country || <span className="italic text-slate-500">(Not set)</span>}
          </p>
        </div>
      ) : (
        <p className="text-slate-500 text-sm italic">
          No address information provided. Please edit your profile to add an address.
        </p>
      )}
    </div>
  );
}

export default UserAddressSection;