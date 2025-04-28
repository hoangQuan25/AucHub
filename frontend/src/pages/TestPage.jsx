// src/pages/TestPage.jsx (New File)
import React, { useState } from 'react';
import apiClient from '../api/apiClient'; // Your configured axios instance
import { useKeycloak } from '@react-keycloak/web'; // To check authentication status

function TestPage() {
  const { keycloak, initialized } = useKeycloak();
  const [pingResult, setPingResult] = useState('');
  const [pingError, setPingError] = useState('');
  const [pingLoading, setPingLoading] = useState(false);

  const [echoResult, setEchoResult] = useState(null);
  const [echoError, setEchoError] = useState('');
  const [echoLoading, setEchoLoading] = useState(false);
  const [echoInput, setEchoInput] = useState('{\n  "testKey": "testValue",\n  "number": 123\n}'); // Default JSON input

  // Handler for the PING (GET) request
  const handlePing = async () => {
    setPingLoading(true);
    setPingResult('');
    setPingError('');
    console.log("Sending PING request...");
    try {
      const response = await apiClient.get('/liveauctions/test/ping');
      console.log("PING Response:", response);
      setPingResult(response.data); // Expects a string like "Pong..."
    } catch (error) {
      console.error("PING Error:", error);
      setPingError(`Error: ${error.message} - ${error.response?.data?.message || error.response?.statusText || ''}`);
    } finally {
      setPingLoading(false);
    }
  };

  // Handler for the ECHO (POST) request
  const handleEcho = async () => {
    if (!keycloak.authenticated) {
      setEchoError("Error: You must be logged in to send the ECHO POST request.");
      return;
    }

    setEchoLoading(true);
    setEchoResult(null);
    setEchoError('');
    console.log("Sending ECHO request...");

    let requestBody = null;
    try {
      requestBody = JSON.parse(echoInput); // Try parsing input as JSON
    } catch (parseError) {
      setEchoError("Error: Invalid JSON in input field.");
      setEchoLoading(false);
      return;
    }

    try {
      // apiClient should automatically add the Authorization header
      const response = await apiClient.post('/liveauctions/test/echo', requestBody);
      console.log("ECHO Response:", response);
      setEchoResult(response.data); // Expects the JSON map echoed back
    } catch (error) {
      console.error("ECHO Error:", error);
      // Log the detailed error structure
      console.error("Full ECHO error object:", error.toJSON ? error.toJSON() : error);
      // Display error details
      let errorDetails = `Error: ${error.message}`;
      if (error.response) {
        errorDetails += ` | Status: ${error.response.status}`;
        errorDetails += ` | Data: ${JSON.stringify(error.response.data || error.response.statusText)}`;
      } else if (error.request) {
         errorDetails += ` | No response received from server.`;
      }
      setEchoError(errorDetails);

      // *** SPECIAL CHECK FOR REDIRECT TO LOGIN ***
      // Check if the error looks like the redirect issue
      if (error.message === 'Network Error' && error.request?.responseURL?.includes('/login')) {
         setEchoError("CRITICAL ERROR: Request was likely redirected to login page by Gateway, possibly due to Auth/CORS/Filter issue on Gateway. Check Gateway logs!");
      }

    } finally {
      setEchoLoading(false);
    }
  };

  if (!initialized) {
    return <div className="p-4">Initializing authentication...</div>;
  }

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold">LiveAuctions Service Test Page</h1>

      {/* --- Ping Test --- */}
      <div className="p-4 border rounded shadow">
        <h2 className="text-lg font-semibold mb-2">Test GET /api/liveauctions/test/ping (Public)</h2>
        <button
          onClick={handlePing}
          disabled={pingLoading}
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
        >
          {pingLoading ? 'Pinging...' : 'Send Ping (GET)'}
        </button>
        {pingResult && (
          <div className="mt-3 p-3 bg-green-100 border border-green-300 rounded text-green-800">
            <strong>Success:</strong> <pre className="inline">{pingResult}</pre>
          </div>
        )}
        {pingError && (
          <div className="mt-3 p-3 bg-red-100 border border-red-300 rounded text-red-800">
            <strong>Error:</strong> {pingError}
          </div>
        )}
      </div>

      {/* --- Echo Test --- */}
      <div className="p-4 border rounded shadow">
        <h2 className="text-lg font-semibold mb-2">Test POST /api/liveauctions/test/echo (Requires SELLER Role)</h2>
        {!keycloak.authenticated && (
          <p className="text-orange-600 mb-2">Please log in to test the authenticated ECHO endpoint.</p>
        )}
        <div className="mb-3">
          <label htmlFor="echoInput" className="block text-sm font-medium text-gray-700 mb-1">
            JSON Request Body:
          </label>
          <textarea
            id="echoInput"
            rows="4"
            value={echoInput}
            onChange={(e) => setEchoInput(e.target.value)}
            className="w-full p-2 border rounded border-gray-300 font-mono text-sm"
            placeholder='Enter valid JSON, e.g., {"key": "value"}'
          />
        </div>
        <button
          onClick={handleEcho}
          disabled={echoLoading || !keycloak.authenticated}
          className="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600 disabled:opacity-50"
        >
          {echoLoading ? 'Echoing...' : 'Send Echo (POST)'}
        </button>
        {echoResult && (
          <div className="mt-3 p-3 bg-green-100 border border-green-300 rounded text-green-800">
            <strong>Success:</strong>
            <pre className="mt-1 text-sm bg-gray-50 p-2 rounded overflow-auto">
              {JSON.stringify(echoResult, null, 2)}
            </pre>
          </div>
        )}
        {echoError && (
          <div className="mt-3 p-3 bg-red-100 border border-red-300 rounded text-red-800">
            <strong>Error:</strong> {echoError}
          </div>
        )}
      </div>
    </div>
  );
}

export default TestPage;
