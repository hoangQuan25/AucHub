// src/pages/MockStripePage.jsx
import React from 'react';
import { Link } from 'react-router-dom';

function MockStripePage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
      <div className="bg-white p-8 rounded-lg shadow-lg text-center max-w-md w-full">
        <h1 className="text-2xl font-bold text-indigo-600 mb-4">Mock Payment Gateway</h1>
        <p className="text-gray-700 mb-6">
          This is where the Stripe payment form (Stripe Elements) would appear.
          For now, assume payment interaction happens here.
        </p>
        <div className="space-y-3">
           <p className="text-sm text-gray-500">Simulate an outcome:</p>
           {/* In a real test, these might trigger backend webhooks */}
           <button
              onClick={() => alert("Simulating Payment Success! (Backend webhook would confirm)")}
              className="w-full px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
            >
              Simulate Payment Success
           </button>
           <button
             onClick={() => alert("Simulating Payment Failure! (Backend webhook would confirm)")}
             className="w-full px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              Simulate Payment Failure
           </button>
           <Link to="/my-orders">
             <button className="w-full mt-4 px-4 py-2 border border-gray-300 text-gray-700 rounded hover:bg-gray-100">
               Go Back to My Orders
             </button>
           </Link>
        </div>
      </div>
    </div>
  );
}

export default MockStripePage;