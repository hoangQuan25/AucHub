// src/components/AllNotificationsModal.jsx
import React, { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import apiClient from "../api/apiClient"; // Adjust path if needed
import { useKeycloak } from "@react-keycloak/web"; // Needed if API requires auth
import { useNotifications } from "../context/NotificationContext"; // Import context hook

function AllNotificationsModal({ isOpen, onClose }) {
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();
  const { markAsRead, markAllAsRead, unreadCount } = useNotifications();

  const [notifications, setNotifications] = useState([]);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 15,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const fetchNotifications = useCallback(
    async (pageToFetch = 0) => {
      // Don't fetch if not open or not authenticated/initialized
      if (!isOpen || !(initialized && keycloak.authenticated)) {
        return;
      }

      setIsLoading(true);
      setError(""); // Clear previous error

      try {
        const response = await apiClient.get(
          "/notifications/my-notifications",
          {
            params: {
              page: pageToFetch,
              size: pagination.size,
              sort: "createdAt,desc", // Fetch newest first
            },
          }
        );

        const pageData = response.data;
        if (pageData && pageData.content) {
          setNotifications(pageData.content);
          setPagination((prev) => ({
            ...prev,
            page: pageData.number ?? pageToFetch,
            totalPages: pageData.totalPages || 0,
          }));
        } else {
          setNotifications([]);
          setPagination((prev) => ({
            ...prev,
            page: pageToFetch,
            totalPages: 0,
          }));
        }
      } catch (err) {
        console.error("Failed to fetch notifications:", err);
        setError(
          err.response?.data?.message || "Could not load notifications."
        );
        setNotifications([]);
      } finally {
        setIsLoading(false);
      }
    },
    [isOpen, initialized, keycloak.authenticated, pagination.size]
  );

  // Fetch data when modal opens or page changes
  useEffect(() => {
    if (isOpen) {
      fetchNotifications(pagination.page);
    }
  }, [isOpen, pagination.page, fetchNotifications]);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < pagination.totalPages) {
      setPagination((prev) => ({ ...prev, page: newPage }));
    }
  };

  const handleNotificationClick = (notification) => {
    // 1. Mark as read (if unread) using context function
    if (!notification.isRead && notification.id) {
      // Check if ID exists
      markAsRead(notification.id);
    }

    // 2. Navigate
    if (notification.relatedAuctionId) {
      // Assuming Timed Auctions for now
      const path = `/timed-auctions/${notification.relatedAuctionId}`;
      console.log(`Navigating to: ${path}`);
      onClose();
      navigate(path);
    } else {
      console.log(
        "Notification clicked, but no relatedAuctionId found:",
        notification
      );
    }
  };

  const handleMarkAllReadInModal = () => {
    markAllAsRead(); // Call context function
    // Optionally refetch after a short delay or rely on optimistic update
    // setTimeout(() => fetchNotifications(pagination.page), 200);
  };

  if (!isOpen) return null;
  const hasUnreadOnPage = notifications.some((n) => !n.isRead);

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header with Close & Mark All As Read */}
        <div className="flex justify-between items-center p-4 border-b">
          <h2 className="text-lg font-semibold text-gray-800">
            All Notifications
          </h2>
          <div className="flex items-center space-x-4">
            {/* Optional: Add Mark All Read button here */}
            {hasUnreadOnPage && (
              <button
                onClick={handleMarkAllReadInModal}
                className="text-xs text-indigo-600 hover:underline"
              >
                Mark all as read
              </button>
            )}
            <button
              onClick={onClose}
              className="text-gray-500 hover:text-gray-800 text-2xl font-bold"
              aria-label="Close notifications"
            >
              &times;
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-4 flex-grow overflow-y-auto">
          {isLoading && (
            <div className="text-center p-6 text-gray-500">
              Loading notifications...
            </div>
          )}
          {error && <div className="text-center p-6 text-red-600">{error}</div>}
          {!isLoading && !error && notifications.length === 0 && (
            <div className="text-center p-6 text-gray-500">
              You have no notifications.
            </div>
          )}
          {!isLoading && !error && notifications.length > 0 && (
            <ul className="divide-y divide-gray-200">
              {notifications.map((notif) => (
                // Make list item clickable
                <li
                  key={notif.id || JSON.stringify(notif)}
                  // Call common handler on click
                  onClick={() => handleNotificationClick(notif)}
                  className={`py-3 px-2 rounded transition-colors duration-150 ${
                    !notif.isRead ? 'bg-blue-50 hover:bg-blue-100 font-medium' : 'hover:bg-gray-100'
                  } ${notif.relatedAuctionId ? 'cursor-pointer' : 'cursor-default'}`} // Make clickable only if link exists
                >
                  <p className={`text-sm leading-relaxed ${!notif.isRead ? 'text-gray-900' : 'text-gray-700'}`}>
                    {notif.message}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {new Date(notif.timestamp).toLocaleString()}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Footer (Pagination) */}
        {pagination.totalPages > 1 && (
          <div className="flex justify-center items-center gap-4 p-3 border-t bg-gray-50 rounded-b-lg">
            <button
              className="px-4 py-1.5 bg-white border rounded text-sm disabled:opacity-50"
              disabled={pagination.page === 0 || isLoading}
              onClick={() => handlePageChange(pagination.page - 1)}
            >
              Previous
            </button>
            <span className="text-sm text-gray-600">
              Page {pagination.page + 1} of {pagination.totalPages}
            </span>
            <button
              className="px-4 py-1.5 bg-white border rounded text-sm disabled:opacity-50"
              disabled={
                pagination.page >= pagination.totalPages - 1 || isLoading
              }
              onClick={() => handlePageChange(pagination.page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default AllNotificationsModal;
