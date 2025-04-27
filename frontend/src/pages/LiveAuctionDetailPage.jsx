// src/pages/LiveAuctionDetailPage.jsx (Real-time Implementation)
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';
import apiClient from '../api/apiClient'; // Adjust path
// Assuming you have a DTO definition shared or copied from backend
// import { LiveAuctionStateDto } from '../dto/LiveAuctionStateDto';

// --- Simple Countdown Timer Component ---
const CountdownTimer = ({ endTimeMillis, onEnd }) => {
    const calculateTimeLeft = useCallback(() => {
        const now = Date.now();
        const difference = endTimeMillis - now;
        let timeLeft = {};

        if (difference > 0) {
            timeLeft = {
                // total: difference, // Optional: total milliseconds
                minutes: Math.floor((difference / 1000 / 60) % 60),
                seconds: Math.floor((difference / 1000) % 60)
            };
        } else {
            timeLeft = { minutes: 0, seconds: 0 };
        }
        return timeLeft;
    }, [endTimeMillis]);

    const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());
    const [isEnded, setIsEnded] = useState(false);

    useEffect(() => {
        // Exit early if already ended
        if (isEnded) return;
        // Reset ended state if end time changes
        setIsEnded(Date.now() >= endTimeMillis);

        const timer = setTimeout(() => {
            const newTimeLeft = calculateTimeLeft();
            setTimeLeft(newTimeLeft);
            if (newTimeLeft.minutes === 0 && newTimeLeft.seconds === 0) {
                 console.log("Countdown Timer Ended.");
                 setIsEnded(true);
                 if(onEnd) onEnd(); // Call callback if provided
            }
        }, 1000); // Update every second

        // Cleanup timeout on unmount or when endTimeMillis changes
        return () => clearTimeout(timer);
    }, [timeLeft, endTimeMillis, calculateTimeLeft, isEnded, onEnd]); // Rerun effect when timeLeft or endTimeMillis changes

    const displayMinutes = String(timeLeft.minutes || 0).padStart(2, '0');
    const displaySeconds = String(timeLeft.seconds || 0).padStart(2, '0');
    const critical = (timeLeft.minutes === 0 && (timeLeft.seconds || 0) <= 20); // Example: critical under 20s

    return (
         <span className={`font-bold text-xl ${critical ? 'text-red-600 animate-pulse' : 'text-gray-800'}`}>
            {displayMinutes}:{displaySeconds}
         </span>
    );
};
// --- End Countdown Timer Component ---


function LiveAuctionDetailPage() {
  const { auctionId } = useParams(); // Get auction ID from route parameter
  const { keycloak, initialized } = useKeycloak(); // For user info
  const navigate = useNavigate();

  const [auctionDetails, setAuctionDetails] = useState(null); // Holds combined details
  const [bidHistory, setBidHistory] = useState([]); // Separate state for bid history updates
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [isBidding, setIsBidding] = useState(false); // Loading state for placing a bid
  const [bidError, setBidError] = useState(''); // Error specific to placing a bid
  const [wsStatus, setWsStatus] = useState('Connecting...'); // WebSocket status

  const ws = useRef(null); // Ref to hold the WebSocket instance

  const loggedInUserId = initialized ? keycloak.subject : null; // Get logged-in user's ID (sub claim)


  // --- Fetch Initial Auction Details ---
  useEffect(() => {
    if (!auctionId || !initialized) return; // Wait for ID and Keycloak init

    setIsLoading(true);
    setError('');
    console.log(`Workspaceing initial details for auction ${auctionId}`);

    apiClient.get(`/liveauctions/${auctionId}/details`)
      .then(response => {
        console.log("Initial details received:", response.data);
        setAuctionDetails(response.data); // Set initial full state
        setBidHistory(response.data.recentBids || []); // Set initial bid history
      })
      .catch(err => {
        console.error("Failed to fetch auction details:", err);
        setError(err.response?.data?.message || `Could not load auction ${auctionId}.`);
      })
      .finally(() => setIsLoading(false));

  }, [auctionId, initialized]); // Fetch when ID or init status changes


  // --- WebSocket Connection ---
  useEffect(() => {
    // Only connect if we have an auction ID, user is authenticated, and WebSocket isn't already connecting/open
    if (!auctionId || !initialized || !keycloak.authenticated || ws.current) {
        // If WebSocket exists but conditions are no longer met, close it
        if (ws.current && (!initialized || !keycloak.authenticated)) {
             console.log("Closing WebSocket due to auth/init change.");
             ws.current.close();
             ws.current = null;
             setWsStatus('Disconnected');
        }
        return;
    }

    // Construct WebSocket URL (use wss:// for production with HTTPS)
    const wsUrl = `ws://localhost:8072/ws/liveauctions/${auctionId}`; // Use Gateway port
    console.log(`Attempting WebSocket connection to: ${wsUrl}`);
    setWsStatus('Connecting...');

    // Create WebSocket instance
    ws.current = new WebSocket(wsUrl); // NOTE: Authentication usually happens via cookies or query params if needed, standard WebSocket API doesn't easily send Bearer tokens. Ensure Gateway allows authenticated handshake.

    ws.current.onopen = () => {
      console.log(`WebSocket connected for auction ${auctionId}`);
      setWsStatus('Connected');
    };

    ws.current.onmessage = (event) => {
      try {
        const stateUpdate = JSON.parse(event.data); // Expecting LiveAuctionStateDto JSON
        console.log('WebSocket message received:', stateUpdate);

        // Update relevant parts of the auction details state
        setAuctionDetails(prevDetails => {
            if (!prevDetails || prevDetails.id !== stateUpdate.auctionId) return prevDetails; // Check if update is for the correct auction
            return {
                ...prevDetails,
                currentBid: stateUpdate.currentBid,
                highestBidderUsername: stateUpdate.highestBidderUsername,
                timeLeftMs: stateUpdate.timeLeftMs,
                reserveMet: stateUpdate.reserveMet,
                nextBidAmount: stateUpdate.nextBidAmount, // Update next bid amount
                status: stateUpdate.status,
                // Optionally update recent bids if sent via WebSocket
                // recentBids: stateUpdate.recentBids || prevDetails.recentBids
            };
        });

        // --- TODO: More robust bid history update ---
        // If stateUpdate contains the *newest* bid, prepend it.
        // If stateUpdate contains the *full* recent history, replace it.
        // Example assuming stateUpdate doesn't directly contain bids,
        // but maybe a NewBidEvent could be sent separately?
        // For now, we only update history when WE place a bid in handlePlaceBid mock.
        // ---

      } catch (e) {
        console.error("Failed to parse WebSocket message:", event.data, e);
      }
    };

    ws.current.onerror = (error) => {
      console.error(`WebSocket error for auction ${auctionId}:`, error);
      setWsStatus('Error');
      // TODO: Implement reconnection logic?
    };

    ws.current.onclose = (event) => {
      console.log(`WebSocket closed for auction ${auctionId}. Code: ${event.code}, Reason: ${event.reason}`);
      setWsStatus(`Closed (${event.code})`);
      ws.current = null; // Clear the ref
      // TODO: Implement reconnection logic?
    };

    // Cleanup function: Close WebSocket when component unmounts or auctionId changes
    return () => {
      if (ws.current) {
        console.log(`Closing WebSocket for auction ${auctionId} on component unmount/cleanup.`);
        ws.current.close();
        ws.current = null;
      }
    };

  }, [auctionId, initialized, keycloak.authenticated]); // Dependencies for WebSocket effect


  // --- Place Bid Handler ---
  const handlePlaceBid = async () => {
      if (!auctionDetails || isBidding || auctionDetails.status !== 'ACTIVE') return;
      // Check if user is currently the highest bidder
      if (auctionDetails.highestBidderUsername === keycloak.tokenParsed?.preferred_username) {
           setBidError("You are already the highest bidder.");
           setTimeout(() => setBidError(''), 3000); // Clear error after 3s
           return;
      }

      setIsBidding(true);
      setBidError('');
      const bidAmount = auctionDetails.nextBidAmount; // Bid the suggested next amount

      console.log(`Attempting to place bid: ${bidAmount} for auction ${auctionId}`);
      try {
          const payload = { amount: bidAmount };
          // Call the backend REST endpoint
          await apiClient.post(`/liveauctions/${auctionId}/bids`, payload);
          console.log(`Bid placement request for ${bidAmount} sent successfully.`);
          // DO NOT update state here. Wait for the WebSocket message broadcast.
          // You could optionally show a temporary "Bid Placed!" message.

      } catch (err) {
          console.error("Failed to place bid:", err);
          setBidError(err.response?.data?.message || err.message || 'Failed to place bid.');
           // Clear error after a few seconds
          setTimeout(() => setBidError(''), 5000);
      } finally {
          setIsBidding(false);
      }
  };

  // --- Render Logic ---

  if (isLoading) return <div className="text-center p-10">Loading Auction Details...</div>;
  if (error) return <div className="text-center p-10 text-red-600">{error}</div>;
  if (!auctionDetails) return <div className="text-center p-10">Auction data not available.</div>; // Should be caught by error typically

  const isUserHighestBidder = initialized && keycloak.authenticated && auctionDetails.highestBidderUsername === keycloak.tokenParsed?.preferred_username;
  const canBid = initialized && keycloak.authenticated && auctionDetails.status === 'ACTIVE' && !isUserHighestBidder;

  return (
    <div className="flex flex-col md:flex-row gap-6 p-4 max-w-7xl mx-auto">

        {/* Left Side: Product Info */}
        <div className="w-full md:w-1/2 lg:w-2/5">
             <h2 className="text-2xl font-bold mb-3">{auctionDetails.product?.title || 'Product Title Missing'}</h2>
             <div className="bg-gray-200 h-80 rounded mb-4 flex items-center justify-center">
                 <img src={auctionDetails.product?.imageUrls?.[0] || '/placeholder.png'} alt={auctionDetails.product?.title} className="max-h-full max-w-full object-contain"/>
             </div>
             <div className="bg-white p-4 rounded shadow-sm border text-sm">
                 <h4 className="font-semibold mb-2 border-b pb-1">Details</h4>
                 <p className="mb-2 whitespace-pre-wrap">{auctionDetails.product?.description || 'No description.'}</p>
                 <p className="mb-2"><strong>Condition:</strong> {auctionDetails.product?.condition?.replace('_',' ') || 'N/A'}</p>
                 <p className="mb-2"><strong>Categories:</strong> {auctionDetails.product?.categories?.map(c => c.name).join(', ') || 'N/A'}</p>
                 <p className="text-xs text-gray-600">Seller: {auctionDetails.sellerId || 'N/A'}</p> {/* TODO: Get seller username? */}
             </div>
        </div>

         {/* Right Side: Bidding Info & Actions */}
         <div className="w-full md:w-1/2 lg:w-3/5">
             {/* WebSocket Status Indicator */}
             <div className="text-xs text-right mb-1 text-gray-500">WebSocket: {wsStatus}</div>

             <div className="bg-white p-4 rounded shadow-sm border mb-4">
                  {/* Status Bar */}
                 <div className="flex justify-between items-center border-b pb-2 mb-3">
                     <span className={`text-sm font-medium ${auctionDetails.reserveMet ? 'text-green-600' : 'text-orange-600'}`}>
                         {auctionDetails.reserveMet ? '✔ Reserve Met' : (auctionDetails.reservePrice ? 'Reserve Not Met' : 'No Reserve')}
                     </span>
                     <div className="text-right">
                         <div className="text-xs text-gray-500">
                             {auctionDetails.status !== 'ACTIVE' ? 'Auction Ended' : 'Time Remaining'}
                          </div>
                         {auctionDetails.status === 'ACTIVE' ? (
                             <CountdownTimer endTimeMillis={Date.now() + (auctionDetails.timeLeftMs || 0)} onEnd={() => console.log("Timer visually ended - waiting for server state")} />
                         ) : (
                              <span className="font-bold text-xl text-gray-500">--:--</span>
                         )}
                     </div>
                 </div>

                 {/* Bidding Area */}
                 <div className="text-center my-4">
                     <p className="text-sm text-gray-600 mb-1">Current Bid</p>
                     <p className="text-4xl font-bold text-indigo-700">
                         {auctionDetails.currentBid?.toLocaleString('vi-VN') || auctionDetails.startPrice?.toLocaleString('vi-VN') || '0'} VNĐ
                     </p>
                     <p className="text-xs text-gray-500 mt-1">
                         Leading: {
                             !auctionDetails.highestBidderUsername ? 'No bids yet' :
                             isUserHighestBidder ? <span className="text-green-600 font-semibold">You!</span> :
                             auctionDetails.highestBidderUsername
                         }
                     </p>
                 </div>

                 {/* Bidding Button */}
                 <button
                    onClick={handlePlaceBid}
                    disabled={!canBid || isBidding} // Disable based on state
                    className="w-full py-3 px-4 bg-green-600 hover:bg-green-700 text-white font-bold rounded text-lg disabled:opacity-60 disabled:cursor-not-allowed transition duration-150"
                 >
                     {isBidding ? 'Placing Bid...' : `Bid ${auctionDetails.nextBidAmount?.toLocaleString('vi-VN') || 'N/A'} VNĐ`}
                 </button>
                 {bidError && <p className="text-center text-xs text-red-500 mt-2">{bidError}</p>}
                 <p className="text-center text-xs text-gray-500 mt-2">
                     (Next required bid: {auctionDetails.nextBidAmount?.toLocaleString('vi-VN') || 'N/A'} VNĐ)
                 </p>
             </div>

             {/* Bid History */}
             <div className="bg-white p-4 rounded shadow-sm border max-h-60 overflow-y-auto">
                 <h4 className="font-semibold mb-2 text-sm border-b pb-1">Bid History</h4>
                 {bidHistory.length === 0 ? (
                     <p className="text-xs text-gray-500">No bids placed yet.</p>
                 ) : (
                    <ul className="space-y-1 text-xs">
                        {bidHistory.map((bid, index) => (
                            <li key={bid.id || index} className="flex justify-between border-b border-dashed border-gray-200 py-0.5">
                                <span>{bid.bidderUsername === keycloak.tokenParsed?.preferred_username ? <span className="font-semibold text-blue-600">You</span> : bid.bidderUsername}</span>
                                <span className="font-medium">{bid.amount.toLocaleString('vi-VN')} VNĐ</span>
                                <span className="text-gray-500">{new Date(bid.bidTime).toLocaleTimeString()}</span> {/* Format time */}
                            </li>
                        ))}
                    </ul>
                 )}
             </div>
         </div>
    </div>
  );
}

export default LiveAuctionDetailPage;