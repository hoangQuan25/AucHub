// src/components/NotificationPanel.jsx
import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useNotifications } from "../context/NotificationContext"; // Import REAL hook

// Remove onOpenAllNotifications if modal is opened directly from Header
function NotificationPanel({ isOpen, onClose, onOpenAllNotifications }) {
  // Use the REAL hook now
  const { notifications, unreadCount, markAllAsRead, markAsRead } =
    useNotifications();
  const navigate = useNavigate();

  if (!isOpen) return null;

  const handleViewAllClick = () => {
    if (onOpenAllNotifications) onOpenAllNotifications();
    onClose();
  };

  const handleMarkOneRead = (id, isRead) => {
    // Only call markAsRead if it's currently unread
    if (!isRead) {
      markAsRead(id);
    }
    // Optional: Navigate to related item? For now, just marks read.
  };

  const handleNotificationClick = (notification) => {
    console.log("Notification clicked:", notification);

    // Navigation logic (Order > Auction)
    let path = null;
    if (notification.relatedOrderId) {
      path = `/orders/${notification.relatedOrderId}`;
    } else if (notification.relatedAuctionId && notification.relatedAuctionType) { // <-- CHECK auctionType
      if (notification.relatedAuctionType.toUpperCase() === 'LIVE') {
        path = `/live-auctions/${notification.relatedAuctionId}`;
      } else if (notification.relatedAuctionType.toUpperCase() === 'TIMED') {
        path = `/timed-auctions/${notification.relatedAuctionId}`;
      }
    }

    if (path) {
      onClose(); // Close the panel before navigating
      navigate(path);
      if (!notification.isRead) {
      markAsRead(notification.id);
    }
    }
  };

  // Check if there are any unread notifications to decide if button should show
  const hasUnread = unreadCount > 0 || notifications.some((n) => !n.isRead);

  return (
    <div
      className="absolute right-4 top-full mt-2 w-80 max-h-[70vh] bg-white rounded-md shadow-lg border border-gray-200 overflow-hidden flex flex-col z-50"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Header */}
      <div className="p-3 flex justify-between items-center border-b">
        <h4 className="font-semibold text-sm text-gray-800">Notifications</h4>
        {/* Show "Mark all as read" only if there are unread items */}
        {hasUnread && (
          <button
            onClick={markAllAsRead} // Call context function
            className="text-xs text-indigo-600 hover:underline"
          >
            Mark all as read
          </button>
        )}
      </div>
      {/* Body */}
      <div className="overflow-y-auto flex-grow">
        {notifications.length === 0 ? (
          <p className="text-center text-gray-500 text-sm p-6">
            No recent notifications.
          </p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {/* Only show maybe the latest 10-15 in the dropdown? Or all received? */}
            {notifications.slice(0, 15).map(
              (
                notif // Limit displayed items
              ) => (
                <li
                  key={notif.id || JSON.stringify(notif)} // Ensure unique key
                  // Call handler to mark read on click
                  onClick={() => {
                    handleMarkOneRead(notif.id, notif.isRead);
                    handleNotificationClick(notif);
                  }}
                  // Apply styles and cursor pointer
                  className={`p-3 text-sm hover:bg-gray-100 transition-colors duration-100 ${
                    !notif.isRead
                      ? "bg-blue-50 font-medium cursor-pointer"
                      : "text-gray-600 cursor-default"
                  }`}
                >
                  <p
                    className={`leading-relaxed ${
                      !notif.isRead ? "text-gray-900" : "text-gray-700"
                    }`}
                  >
                    {notif.message}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {new Date(notif.timestamp).toLocaleString()}{" "}
                    {/* Use timestamp field */}
                  </p>
                </li>
              )
            )}
          </ul>
        )}
      </div>
      {/* Footer */}
      <div className="p-2 text-center border-t bg-gray-50">
        <button
          onClick={handleViewAllClick}
          className="text-sm text-indigo-600 hover:underline font-medium"
        >
          View all notifications
        </button>
      </div>
    </div>
  );
}

export default NotificationPanel;
