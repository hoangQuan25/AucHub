// src/context/NotificationContext.jsx
import React, { createContext, useState, useEffect, useContext, useRef, useCallback } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import SockJS from 'sockjs-client/dist/sockjs'; // Import SockJS
import { Client } from '@stomp/stompjs'; // Import STOMP client
import apiClient from '../api/apiClient'; // If needed for mark as read API calls
import { toast } from 'react-toastify';

// --- Helper: Create Context ---
const NotificationContext = createContext({
  notifications: [],
  unreadCount: 0,
  isConnected: false,
  followedAuctionIds: new Set(),
  markAsRead: (notificationId) => {}, 
  markAllAsRead: () => {}, 
  followAuction: (auctionId, auctionType) => {},
  unfollowAuction: (auctionId) => {},
});

// --- Custom Hook for easy consumption ---
export const useNotifications = () => useContext(NotificationContext);

// --- Provider Component ---
export const NotificationProvider = ({ children }) => {
  const { keycloak, initialized } = useKeycloak();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isConnected, setIsConnected] = useState(false);
  const [followedAuctionIds, setFollowedAuctionIds] = useState(new Set());

  
  const stompClientRef = useRef(null);
  const notificationSubscriptionRef = useRef(null);
  // --- NEW: Ref for unread count subscription ---
  const countSubscriptionRef = useRef(null);
  const currentUserId = useRef(null);

  // Function to handle incoming notification messages
  const handleIncomingNotification = useCallback((message) => {
    try {
      const notificationDto = JSON.parse(message.body); // Assuming body is NotificationDto JSON
      console.log("Received Notification:", notificationDto);

      // Add to the beginning of the list (newest first)
      // Limit the number of stored notifications if desired (e.g., keep last 50)
      setNotifications(prev => [notificationDto, ...prev.slice(0, 49)]);

      // Increment unread count if it's marked as unread (backend should set isRead=false)
      if (!notificationDto.isRead) {
        setUnreadCount(prev => prev + 1);
      }
      
      if (notificationDto.message) {
      let toastType = 'info'; // Default toast type
      const notificationTypeUpper = notificationDto.type?.toUpperCase();

      // Example logic to determine toast type based on notification type
      // You can customize these keywords and logic as needed.
      if (notificationTypeUpper?.includes('ERROR') || 
          notificationTypeUpper?.includes('CANCELLED') || 
          notificationTypeUpper?.includes('FAILED') ||
          notificationTypeUpper?.includes('DEFAULTED')) {
        toastType = 'error';
      } else if (notificationTypeUpper?.includes('SUCCESS') || 
                 notificationTypeUpper?.includes('COMPLETED') || 
                 notificationTypeUpper?.includes('CONFIRMED') || 
                 notificationTypeUpper?.includes('READY_FOR_SHIPPING') ||
                 notificationTypeUpper?.includes('CREATED') ) { // e.g. Order Created
        toastType = 'success';
      } else if (notificationTypeUpper?.includes('WARNING') || 
                 notificationTypeUpper?.includes('OUTBID') || 
                 notificationTypeUpper?.includes('DECISION_REQUIRED') ||
                 notificationTypeUpper?.includes('PAYMENT_DUE')) {
        toastType = 'warning';
      }

      toast(notificationDto.message, {
        type: toastType, // Uses the determined type: 'info', 'success', 'warning', 'error'
        // You can add more options here from react-toastify documentation
        // e.g., onClick, toastId, etc.
      });
    }

    } catch (e) {
      console.error("Failed to parse notification message:", message.body, e);
    }
  }, []);

  // --- NEW: Function to handle incoming unread count updates ---
  const handleUnreadCountUpdate = useCallback((message) => {
    try {
       const payload = JSON.parse(message.body);
       if (payload && typeof payload.unreadCount === 'number') {
           console.log("Received unread count update:", payload.unreadCount);
           setUnreadCount(payload.unreadCount);
       } else {
            console.warn("Received invalid unread count update payload:", payload);
       }
    } catch(e) {
        console.error("Failed to parse unread count update:", message.body, e);
    }
 }, [initialized, keycloak.authenticated]);

 const fetchUnreadCount = useCallback(async () => {
  if (!(initialized && keycloak.authenticated)) return;
  console.log("Fetching initial unread count...");
  try {
      const response = await apiClient.get('/notifications/unread-count');
      setUnreadCount(response.data?.unreadCount ?? 0);
  } catch (err) {
      console.error("Failed to fetch unread count:", err);
      // Optionally set an error state or just default to 0
      setUnreadCount(0);
  }
}, [initialized, keycloak.authenticated]);

const fetchFollowedIds = useCallback(async () => {
  if (!(initialized && keycloak.authenticated)) return;
  console.log("Fetching followed auction IDs...");
  try {
    const response = await apiClient.get('/notifications/following/ids');
    // Assuming response.data is an array/list of UUID strings
    if (Array.isArray(response.data)) {
      setFollowedAuctionIds(new Set(response.data));
      console.log(`User follows ${response.data.length} auctions.`);
    } else {
       console.warn("Received non-array response for followed IDs:", response.data);
       setFollowedAuctionIds(new Set());
    }
  } catch (err) {
    console.error("Failed to fetch followed auction IDs:", err);
    setFollowedAuctionIds(new Set()); // Reset on error
  }
}, [initialized, keycloak.authenticated]);

  // Function to connect WebSocket/STOMP
  const connectNotifications = useCallback(() => {
    // Prevent connection if not authenticated, not initialized, or already connected/connecting
    if (!initialized || !keycloak.authenticated || stompClientRef.current?.active) {
         console.log("Notification connect prerequisites not met or already active.");
         setIsConnected(stompClientRef.current?.active || false); // Update connected state
         return;
    }

    const userId = keycloak.subject;
    currentUserId.current = userId; // Store current user ID
    console.log(`Attempting STOMP connection for user ${userId}...`);
    setIsConnected(false); // Set connecting state

    const client = new Client({
      webSocketFactory: () => {
        const gatewayHost = "localhost:8072"; // Your API Gateway
        const sockjsUrl = `${window.location.protocol}//${gatewayHost}/ws/notifications`; // Use the correct notification endpoint path
        console.log(`Creating SockJS connection to: ${sockjsUrl}`);
        return new SockJS(sockjsUrl);
        // Note: Authentication (Keycloak token) needs to be handled.
        // Usually done via cookies set by the Gateway, or potentially query params if needed & secure.
        // Sending token in connectHeaders often DOES NOT WORK with SockJS/WebSockets due to browser limitations.
      },
      reconnectDelay: 10000, // Reconnect every 10 seconds
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log("STOMP NOTIF DEBUG:", str), // Enable debug logging
    });

    client.onConnect = (frame) => {
      console.log(`NOTIFICATIONS STOMP Connected for user ${userId}: ${frame}`);
      setIsConnected(true);

      // Subscribe to user-specific notifications
      const notificationDest = `/user/${userId}/queue/notifications`;
      console.log(`Subscribing to STOMP destination: ${notificationDest}`);
      notificationSubscriptionRef.current = client.subscribe(notificationDest, handleIncomingNotification);

      // --- NEW: Subscribe to unread count updates (Optional) ---
      const countDest = `/user/${userId}/queue/unread-count`;
      console.log(`Subscribing to STOMP destination: ${countDest}`);
      countSubscriptionRef.current = client.subscribe(countDest, handleUnreadCountUpdate);
      // --- End Subscribe to count ---

      console.log("Notification subscriptions active.");

      // Fetch initial unread count AFTER connecting/subscribing
      fetchUnreadCount();
      fetchFollowedIds();
    };

    client.onStompError = (frame) => {
      console.error("NOTIFICATIONS STOMP Error:", frame.headers['message'], frame.body);
      setIsConnected(false);
    };
    client.onWebSocketError = (error) => {
      console.error("NOTIFICATIONS WebSocket Error:", error);
      setIsConnected(false);
    };

     client.onWebSocketClose = (event) => {
       console.log(`NOTIFICATIONS WebSocket Closed: Code=${event?.code}`);
       setIsConnected(false);
       notificationSubscriptionRef.current = null;
       countSubscriptionRef.current = null; // Clear count subscription too
     };

    client.activate();
    stompClientRef.current = client;

  }, [initialized, keycloak.authenticated, handleIncomingNotification, handleUnreadCountUpdate, fetchUnreadCount]); // Dependencies for connect function


  // Function to disconnect WebSocket/STOMP
  const disconnectNotifications = useCallback(() => {
    console.log("Attempting to disconnect notifications STOMP client...");
    // Unsubscribe explicitly before deactivating
    if (notificationSubscriptionRef.current) {
        try { notificationSubscriptionRef.current.unsubscribe(); } catch (e) { console.error("Error unsubscribing notifications:", e); }
        notificationSubscriptionRef.current = null;
    }
    if (countSubscriptionRef.current) { // Unsubscribe from count too
        try { countSubscriptionRef.current.unsubscribe(); } catch (e) { console.error("Error unsubscribing count:", e); }
        countSubscriptionRef.current = null;
    }
     if (stompClientRef.current?.active) {
        try { stompClientRef.current.deactivate(); } catch (e) { console.error("Error deactivating client:", e); }
        console.log("Notifications STOMP client deactivated.");
     }
     stompClientRef.current = null;
     setIsConnected(false);
     setNotifications([]);
     setUnreadCount(0);
     currentUserId.current = null;
  }, []);

  
  // Effect to connect/disconnect based on auth state
  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      connectNotifications();
    } else {
      disconnectNotifications();
    }
    return () => disconnectNotifications();
  }, [initialized, keycloak.authenticated]);

  const markAsRead = useCallback(async (notificationIdInput) => {
    // Allow single ID or array
    const notificationIds = Array.isArray(notificationIdInput) ? notificationIdInput : [notificationIdInput];
    if (notificationIds.length === 0) return;

    const userId = currentUserId.current;
    if (!userId) return; // Need user context

    console.log(`Attempting to mark notifications ${notificationIds} as read for user ${userId}`);

    // Optimistic UI update
    let actuallyMarkedCount = 0;
    setNotifications(prev => prev.map(n => {
        if (notificationIds.includes(n.id) && !n.isRead) {
            actuallyMarkedCount++; // Count how many were actually unread
            return { ...n, isRead: true };
        }
        return n;
    }));
     // Decrement count optimistically ONLY by the number that were actually unread
    if (actuallyMarkedCount > 0) {
        setUnreadCount(prev => Math.max(0, prev - actuallyMarkedCount));
    }

    try {
      await apiClient.post('/notifications/mark-read', notificationIds); // Send array of IDs
      // Optional: If backend doesn't push count updates, fetch count again here
      // if (!backend_pushes_count_updates) fetchUnreadCount();
    } catch (err) {
      console.error("Failed to mark notification(s) as read:", err);
      // TODO: Revert optimistic update on failure? More complex state needed.
       // For now, log the error. Might fetch count again to resync.
       fetchUnreadCount();
    }
  }, [fetchUnreadCount]);


  const markAllAsRead = useCallback(async () => {
    const userId = currentUserId.current;
    if (!userId) return;

    console.log(`Attempting to mark all notifications as read for user ${userId}`);

    // Optimistic UI update
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    setUnreadCount(0); // Reset count optimistically

   try {
     await apiClient.post('/notifications/mark-all-read'); // No body needed
      // Optional: If backend doesn't push count updates, fetch count again (should be 0)
      // if (!backend_pushes_count_updates) fetchUnreadCount(); // Should ideally result in 0
   } catch (err) {
     console.error("Failed to mark all notifications as read:", err);
     // TODO: Revert optimistic update? Fetch real count.
     fetchUnreadCount();
   }
 }, [fetchUnreadCount]);

 const followAuction = useCallback(async (auctionId, auctionType) => {
  const userId = currentUserId.current;
  if (!userId || !auctionId || !auctionType) return;
  console.log(`Requesting follow: User ${userId}, Auction ${auctionId}, Type ${auctionType}`);

  // Optimistic UI Update
  setFollowedAuctionIds(prev => new Set(prev).add(auctionId));

  try {
      // Ensure type is uppercase for consistency if needed by backend path param matching
      await apiClient.post(`/notifications/follow/${auctionType.toUpperCase()}/${auctionId}`);
      console.log("Follow request successful");
  } catch(err) {
       console.error(`Failed to follow auction ${auctionId}:`, err);
       // Revert optimistic update on failure
       setFollowedAuctionIds(prev => {
           const newSet = new Set(prev);
           newSet.delete(auctionId);
           return newSet;
       });
       // TODO: Show error message to user?
  }

}, []); 

 const unfollowAuction = useCallback(async (auctionId) => {
  const userId = currentUserId.current;
  if (!userId || !auctionId) return;
  console.log(`Requesting unfollow: User ${userId}, Auction ${auctionId}`);

   // Optimistic UI Update
   setFollowedAuctionIds(prev => {
       const newSet = new Set(prev);
       newSet.delete(auctionId);
       return newSet;
   });

  try {
      // DELETE request, only needs auctionId
      await apiClient.delete(`/notifications/follow/${auctionId}`);
      console.log("Unfollow request successful");
  } catch(err) {
       console.error(`Failed to unfollow auction ${auctionId}:`, err);
        // Revert optimistic update on failure
        setFollowedAuctionIds(prev => new Set(prev).add(auctionId)); // Add it back
        // TODO: Show error message to user?
  }
}, []); 

  // --- Provide state and functions through context ---
  const value = {
    notifications,
    unreadCount,
    isConnected,
    markAsRead,
    markAllAsRead,
    followedAuctionIds,
    followAuction,
    unfollowAuction,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};