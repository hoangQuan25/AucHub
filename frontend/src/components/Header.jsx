// src/components/Header.jsx
import React, { useState, useEffect, useRef } from "react"; // Added useState, useEffect, useRef
import { Link, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { FaBell, FaHeart, FaShoppingBag } from "react-icons/fa"; // Import bell icon
import NotificationPanel from "./NotificationPanel"; // Import the panel
import AllNotificationsModal from "./AllNotificationsModal";
import { useNotifications } from "../context/NotificationContext"; // Import the context

function Header() {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const { unreadCount } = useNotifications();

  // State to control notification panel visibility
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const notificationIconRef = useRef(null); // Ref for the bell icon/button area
  const [isAllNotificationsModalOpen, setIsAllNotificationsModalOpen] =
    useState(false);

  const handleLogout = () => {
    keycloak.logout({ redirectUri: window.location.origin });
  };

  const goToProfile = () => {
    navigate("/profile");
  };

  const togglePanel = (e) => {
    e.stopPropagation(); // Prevent immediate closing by click outside handler
    setIsPanelOpen((prev) => !prev);
  };

  const closePanel = () => {
    setIsPanelOpen(false);
  };

  // --- NEW Handler to open the 'All Notifications' modal ---
  const openAllNotificationsModal = () => {
    setIsPanelOpen(false); // Close the dropdown panel first
    setIsAllNotificationsModalOpen(true);
  };
  const closeAllNotificationsModal = () => {
    setIsAllNotificationsModalOpen(false);
  };

  // Effect to handle clicks outside the panel to close it
  useEffect(() => {
    const handleClickOutside = (event) => {
      /* ... existing logic to close panel ... */
      if (
        isPanelOpen &&
        notificationIconRef.current &&
        !notificationIconRef.current.contains(event.target)
      ) {
        closePanel();
      }
    };
    if (isPanelOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    } else {
      document.removeEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isPanelOpen]);

  return (
    <header className="p-4 bg-gray-800 text-white flex justify-between items-center shadow-md relative">
      {" "}
      {/* Add relative positioning */}
      <Link to="/" className="text-2xl font-bold hover:text-gray-300">
        AucHub
      </Link>
      <div>
        {keycloak.authenticated ? (
          <div className="flex items-center space-x-4">
            {/* --- Notification Bell --- */}
            <div ref={notificationIconRef} className="relative">
              {" "}
              {/* Wrapper for positioning panel */}
              <button
                onClick={togglePanel}
                className="p-2 rounded-full hover:bg-gray-700 text-gray-300 hover:text-white relative"
                aria-label="Notifications"
              >
                <FaBell size="1.25em" />
                {/* Unread indicator badge */}
                {unreadCount > 0 && (
                  <span className="absolute top-0 right-0 block h-2.5 w-2.5 transform -translate-y-1/2 translate-x-1/2 rounded-full bg-red-500 ring-2 ring-gray-800"></span>
                  // Or display a count:
                  // <span className="absolute -top-1 -right-2 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-red-100 bg-red-600 rounded-full">{unreadCount}</span>
                )}
              </button>
              {/* Render Notification Panel */}
              {/* Panel positioning depends on the wrapper being relative */}
              <NotificationPanel
                isOpen={isPanelOpen}
                onClose={closePanel}
                onOpenAllNotifications={openAllNotificationsModal}
              />
            </div>
            {/* --- End Notification Bell --- */}

            {/* --- Following (heart) link --- */}
            <Link
              to="/following"
              className="p-2 rounded-full hover:bg-gray-700 text-gray-300 hover:text-white"
            >
              <FaHeart size="1.25em" />
            </Link>

            {/* My Orders (bag) link */}
            <Link
              to="/my-orders"
              className="p-2 rounded-full hover:bg-gray-700 text-gray-300 hover:text-white"
              title="My Orders"
            >
              <FaShoppingBag size="1.25em" />
            </Link>

            {/* User Menu/Avatar Section */}
            <div
              onClick={goToProfile}
              className="flex items-center cursor-pointer hover:bg-gray-700 p-2 rounded"
            >
              <div className="w-8 h-8 rounded-full bg-gray-500 mr-2 flex items-center justify-center">
                <span className="text-sm font-semibold">
                  {keycloak.tokenParsed?.preferred_username
                    ?.charAt(0)
                    .toUpperCase() || "U"}
                </span>
              </div>
              <span className="font-medium">
                {keycloak.tokenParsed?.preferred_username}
              </span>
            </div>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
            >
              Logout
            </button>
          </div>
        ) : (
          // Login/Register buttons
          <div className="flex items-center space-x-2">
            <button
              onClick={() => keycloak.login()}
              className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
            >
              Login
            </button>
            <button
              onClick={() => keycloak.register()}
              className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
            >
              Register
            </button>
          </div>
        )}
      </div>
      <AllNotificationsModal
        isOpen={isAllNotificationsModalOpen}
        onClose={closeAllNotificationsModal}
      />
    </header>
  );
}

export default Header;
