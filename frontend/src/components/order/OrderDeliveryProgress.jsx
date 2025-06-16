// src/components/order/OrderDeliveryProgress.jsx
import React from 'react';
import { FaTruckLoading, FaCheckCircle, FaExclamationTriangle, FaBoxOpen } from 'react-icons/fa'; // Example icons

// You might want a mapping for delivery statuses to user-friendly text, similar to orderStatusMap
// src/constants/deliveryConstants.js (or orderConstants.js)
export const deliveryStatusMap = {
  PENDING_PREPARATION: 'Pending Preparation',
  READY_FOR_SHIPMENT: 'Ready for Shipment',
  SHIPPED_IN_TRANSIT: 'Shipped - In Transit',
  DELIVERED: 'Delivered (Awaiting Confirmation)', // Or keep as 'Delivered'
  AWAITING_BUYER_CONFIRMATION: 'Delivered - Awaiting Your Confirmation',
  RECEIPT_CONFIRMED_BY_BUYER: 'Completed - Receipt Confirmed',
  COMPLETED_AUTO: 'Completed (Auto-confirmed)',
  RETURN_REQUESTED_BY_BUYER: 'Return Requested',
  ISSUE_REPORTED: 'Issue Reported',
  CANCELLED: 'Delivery Cancelled',
};

// Helper to get an icon based on status
const getStatusIcon = (status) => {
  switch (status) {
    case 'PENDING_PREPARATION':
    case 'READY_FOR_SHIPMENT':
      return <FaBoxOpen className="text-orange-500 text-xl" />;
    case 'SHIPPED_IN_TRANSIT':
      return <FaTruckLoading className="text-blue-500 text-xl" />;
    case 'DELIVERED':
      return <FaCheckCircle className="text-green-500 text-xl" />;
    case 'ISSUE_REPORTED':
    case 'CANCELLED':
      return <FaExclamationTriangle className="text-red-500 text-xl" />;
    default:
      return <FaBoxOpen className="text-gray-500 text-xl" />;
  }
};

function OrderDeliveryProgress({ deliveryDetails }) {
  if (!deliveryDetails || !deliveryDetails.deliveryStatus) {
    return null; 
  }

  const {
    deliveryStatus,
    courierName,
    trackingNumber,
    shippedAt,
    deliveredAt,
    notes // Notes from the delivery record itself
  } = deliveryDetails;

  const friendlyStatus = deliveryStatusMap[deliveryStatus] || deliveryStatus;
  const statusIcon = getStatusIcon(deliveryStatus);

  const trackingLink = trackingNumber && courierName ? 
    `https://www.google.com/search?q=${encodeURIComponent(courierName + ' tracking ' + trackingNumber)}` : null;

  return (
    <div className="my-6 p-6 bg-white rounded-lg shadow border border-gray-200">
      <h2 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
        {statusIcon} Delivery Progress
      </h2>
      <div className="space-y-3 text-sm">
        <div className="flex justify-between">
          <span className="text-gray-600 font-medium">Status:</span>
          <span className="font-semibold text-gray-800">{friendlyStatus}</span>
        </div>

        {courierName && (
          <div className="flex justify-between">
            <span className="text-gray-600 font-medium">Courier:</span>
            <span className="text-gray-800">{courierName}</span>
          </div>
        )}

        {trackingNumber && (
          <div className="flex justify-between items-center">
            <span className="text-gray-600 font-medium">Tracking #:</span>
            {trackingLink ? (
              <a 
                href={trackingLink} 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-blue-600 hover:text-blue-800 hover:underline font-mono"
              >
                {trackingNumber}
              </a>
            ) : (
              <span className="text-gray-800 font-mono">{trackingNumber}</span>
            )}
          </div>
        )}

        {shippedAt && (
          <div className="flex justify-between">
            <span className="text-gray-600 font-medium">Shipped At:</span>
            <span className="text-gray-800">{new Date(shippedAt).toLocaleString('vi-VN')}</span>
          </div>
        )}

        {deliveredAt && (
          <div className="flex justify-between">
            <span className="text-gray-600 font-medium">Delivered At:</span>
            <span className="text-gray-800">{new Date(deliveredAt).toLocaleString('vi-VN')}</span>
          </div>
        )}
        
        {notes && (
          <div className="pt-2 mt-2 border-t border-gray-200">
            <span className="text-gray-600 font-medium block mb-1">Delivery Notes:</span>
            <p className="text-gray-700 whitespace-pre-wrap bg-gray-50 p-2 rounded">{notes}</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default OrderDeliveryProgress;