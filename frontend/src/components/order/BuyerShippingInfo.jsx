// src/components/order/BuyerShippingInfo.jsx
import React from 'react';
import { FaMapMarkerAlt, FaPhone, FaUserCircle } from 'react-icons/fa'; // Example icons

function BuyerShippingInfo({ deliveryDetails }) {
  if (!deliveryDetails) {
    return (
      <div className="mb-6 p-4 bg-blue-50 text-blue-700 rounded-lg border border-blue-200">
        Preparing shipping information... If this persists, the delivery record might not have been created yet.
      </div>
    );
  }

  // Fallback for missing fields
  const recipientName = deliveryDetails.shippingRecipientName || 'N/A';
  const phone = deliveryDetails.shippingPhoneNumber || 'N/A';
  const street = deliveryDetails.shippingStreetAddress || 'N/A';
  const city = deliveryDetails.shippingCity || 'N/A';
  const postalCode = deliveryDetails.shippingPostalCode || 'N/A';
  const country = deliveryDetails.shippingCountry || 'N/A';
  const addressLine1 = street;
  const addressLine2 = `${city}${postalCode ? `, ${postalCode}` : ''}${country ? `, ${country}` : ''}`;


  return (
    <div className="mb-6 p-6 bg-white rounded-lg shadow border border-gray-200">
      <h2 className="text-xl font-semibold text-gray-800 mb-4">Ship To:</h2>
      <div className="space-y-3 text-sm text-gray-700">
        <div className="flex items-center gap-3">
          <FaUserCircle className="text-lg text-gray-500 flex-shrink-0" />
          <div>
            <span className="font-medium text-gray-800">Recipient:</span> {recipientName}
            <br />
            (<span className="text-xs text-gray-500">Buyer ID: {deliveryDetails.buyerId ? deliveryDetails.buyerId.substring(0,8) : 'N/A'}...</span>)
          </div>
        </div>
        {deliveryDetails.shippingPhoneNumber && (
             <div className="flex items-center gap-3">
                <FaPhone className="text-lg text-gray-500 flex-shrink-0" />
                <div>
                <span className="font-medium text-gray-800">Phone:</span> {phone}
                </div>
            </div>
        )}
        <div className="flex items-start gap-3">
          <FaMapMarkerAlt className="text-lg text-gray-500 flex-shrink-0 mt-1" />
          <div>
            <span className="font-medium text-gray-800">Address:</span>
            <p>{addressLine1}</p>
            <p>{addressLine2}</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BuyerShippingInfo;