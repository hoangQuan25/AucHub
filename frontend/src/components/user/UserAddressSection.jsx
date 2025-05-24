import React from 'react';

function UserAddressSection({ profileData }) {
  if (!profileData) return null;

  return (
    <div className="mb-6 p-4 border rounded bg-white shadow-sm">
      <h3 className="text-lg font-semibold mb-3 border-b pb-2">
        Address
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
            <strong>State/Province:</strong> {profileData.stateProvince || '(Not set)'}
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
  );
}

export default UserAddressSection;