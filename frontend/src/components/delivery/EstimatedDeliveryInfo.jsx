import React from 'react';
import { FaTruck } from 'react-icons/fa';
import CountdownTimer from '../CountdownTimer';

function EstimatedDeliveryInfo({ deliveryDetails }) {
  if (
    !deliveryDetails ||
    deliveryDetails.deliveryStatus !== 'SHIPPED_IN_TRANSIT' ||
    !deliveryDetails.estimatedDeliveryAt
  ) {
    return null;
  }

  const estimatedTime = new Date(deliveryDetails.estimatedDeliveryAt).getTime();
  const estimatedDateStr = new Date(estimatedTime).toLocaleString('en-GB', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div className="my-8 p-6 bg-gradient-to-br from-blue-50 via-white to-blue-100 rounded-xl border border-blue-200 shadow-md">
      <div className="flex items-center gap-4 mb-4">
        <div className="bg-blue-100 p-3 rounded-full shadow">
          <FaTruck className="text-2xl text-blue-600" />
        </div>
        <h3 className="text-xl font-bold text-blue-900 tracking-tight">
          Estimated Delivery Time
        </h3>
      </div>
      <div className="text-base text-blue-900 space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center gap-2 pt-1">
          <span className="font-medium">Estimated delivery at:</span>
          <span className="inline-block px-3 py-1 bg-blue-100 text-blue-900 font-semibold rounded-lg shadow-sm text-base tracking-wider">
            {estimatedDateStr}
          </span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center gap-2 pt-1">
          <span className="font-medium">Time remaining:</span>
          <span className="inline-block px-3 py-1 bg-blue-100 text-white font-bold rounded-lg shadow-sm text-lg tracking-wider">
            <CountdownTimer endTimeMillis={estimatedTime} />
          </span>
        </div>
        <div className="flex items-center gap-2 text-xs text-blue-700 bg-blue-50 rounded p-2 mt-2 border border-blue-100">
          <svg className="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M13 16h-1v-4h-1m1-4h.01M12 20.5C6.753 20.5 2 15.747 2 10.5S6.753.5 12 .5s10 4.753 10 10-4.753 10-10 10z" />
          </svg>
          This is the estimated timeframe from the carrier. The system will automatically update the order and begin the buyer confirmation process when this time is up.
        </div>
      </div>
    </div>
  );
}

export default EstimatedDeliveryInfo;