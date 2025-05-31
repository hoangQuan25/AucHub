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
    // --- 2. UPDATE: Navigation logic ---
    let path = null;
    if (notification.relatedOrderId) {
      path = `/orders/${notification.relatedOrderId}`;
    } else if (notification.relatedAuctionId && notification.relatedAuctionType) {
      // <-- CHECK auctionType
      if (notification.relatedAuctionType.toUpperCase() === "LIVE") {
        path = `/live-auctions/${notification.relatedAuctionId}`;
      } else if (notification.relatedAuctionType.toUpperCase() === "TIMED") {
        path = `/timed-auctions/${notification.relatedAuctionId}`;
      }
    }

    if (path) {
      onClose(); // Close the modal before navigating
      navigate(path);
      // 1. Mark as read
      if (!notification.isRead && notification.id) {
        markAsRead(notification.id);
      }
    }
  };

  const handleMarkAllReadInModal = () => {
    markAllAsRead(); // Call context function
    // Optionally refetch after a short delay or rely on optimistic update
    // setTimeout(() => fetchNotifications(pagination.page), 200);
  };

  if (!isOpen) return null;
  const hasUnreadOnPage = notifications.some((n) => !n.isRead);

  // ...existing imports...

return (
  <div
    className="fixed inset-0 z-50 flex justify-center items-center p-4"
    style={{ background: "none" }} // No overlay/gradient background
    onClick={onClose}
  >
    <div
      className="w-full max-w-2xl max-h-[80vh] flex flex-col border border-slate-700 rounded-2xl shadow-2xl bg-slate-900"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Header */}
      <div className="flex justify-between items-center p-5 border-b border-slate-700 bg-slate-800 rounded-t-2xl">
        <h2 className="text-xl font-bold text-indigo-300 tracking-wide">
          All Notifications
        </h2>
        <div className="flex items-center space-x-4">
          {hasUnreadOnPage && (
            <button
              onClick={handleMarkAllReadInModal}
              className="text-xs text-indigo-400 hover:text-indigo-200 font-semibold transition"
            >
              Mark all as read
            </button>
          )}
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white text-2xl font-bold transition"
            aria-label="Close notifications"
          >
            &times;
          </button>
        </div>
      </div>

      {/* Modal Body */}
      <div className="p-5 flex-grow overflow-y-auto bg-slate-900">
        {isLoading && (
          <div className="text-center p-6 text-indigo-200">
            Loading notifications...
          </div>
        )}
        {error && (
          <div className="text-center p-6 text-red-400">{error}</div>
        )}
        {!isLoading && !error && notifications.length === 0 && (
          <div className="text-center p-6 text-indigo-400">
            You have no notifications.
          </div>
        )}
        {!isLoading && !error && notifications.length > 0 && (
          <ul className="divide-y divide-slate-700">
            {notifications.map((notif) => (
              <li
                key={notif.id || JSON.stringify(notif)}
                onClick={() => handleNotificationClick(notif)}
                className={`py-4 px-3 rounded-lg transition-colors duration-150 cursor-pointer group ${
                  !notif.isRead
                    ? "bg-slate-800 border-l-4 border-indigo-500 shadow"
                    : "hover:bg-slate-800/80"
                }`}
              >
                <p
                  className={`text-base leading-relaxed ${
                    !notif.isRead
                      ? "text-indigo-100 font-semibold"
                      : "text-slate-300"
                  } group-hover:text-white transition`}
                >
                  {notif.message}
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  {new Date(notif.timestamp).toLocaleString()}
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Footer (Pagination) */}
      {pagination.totalPages > 1 && (
        <div className="flex justify-center items-center gap-4 p-4 border-t border-slate-700 bg-slate-800 rounded-b-2xl">
          <button
            className="px-4 py-1.5 bg-slate-900 border border-slate-700 rounded text-sm text-indigo-200 hover:bg-slate-800 transition disabled:opacity-50"
            disabled={pagination.page === 0 || isLoading}
            onClick={() => handlePageChange(pagination.page - 1)}
          >
            Previous
          </button>
          <span className="text-sm text-indigo-300">
            Page {pagination.page + 1} of {pagination.totalPages}
          </span>
          <button
            className="px-4 py-1.5 bg-slate-900 border border-slate-700 rounded text-sm text-indigo-200 hover:bg-slate-800 transition disabled:opacity-50"
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
