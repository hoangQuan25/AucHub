// src/components/Header.jsx
import React, { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { FaBell, FaHeart, FaShoppingBag, FaSearch, FaUserCircle } from "react-icons/fa"; // Added FaUserCircle for fallback
import NotificationPanel from "./NotificationPanel";
import AllNotificationsModal from "./AllNotificationsModal";
import { useNotifications } from "../context/NotificationContext";

const AUCTION_TYPE_SEARCH_OPTIONS = [
  { key: "ALL", label: "All Auctions" },
  { key: "LIVE", label: "Live Auctions" },
  { key: "TIMED", label: "Timed Auctions" },
];

function Header() {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const { unreadCount } = useNotifications();

  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const notificationIconRef = useRef(null);
  const [isAllNotificationsModalOpen, setIsAllNotificationsModalOpen] =
    useState(false);

  const [searchTerm, setSearchTerm] = useState("");
  const [searchAuctionType, setSearchAuctionType] = useState(
    AUCTION_TYPE_SEARCH_OPTIONS[0].key
  );

  const userInitial = keycloak.tokenParsed?.preferred_username?.[0]?.toUpperCase();

  const handleLogout = () => {
    keycloak.logout({ redirectUri: window.location.origin });
  };

  const goToProfile = () => {
    navigate("/profile");
  };

  const togglePanel = (e) => {
    e.stopPropagation();
    setIsPanelOpen((prev) => !prev);
  };

  const closePanel = () => {
    setIsPanelOpen(false);
  };

  const openAllNotificationsModal = () => {
    setIsPanelOpen(false);
    setIsAllNotificationsModalOpen(true);
  };
  const closeAllNotificationsModal = () => {
    setIsAllNotificationsModalOpen(false);
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        isPanelOpen &&
        notificationIconRef.current &&
        !notificationIconRef.current.contains(event.target) &&
        // Ensure the click is not on the panel itself if it's rendered outside the ref
        !event.target.closest('.notification-panel-class') // Add a class to your NotificationPanel main div
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

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const queryParams = new URLSearchParams();
    if (searchTerm.trim()) {
      queryParams.set("query", searchTerm.trim());
    }
    if (searchAuctionType !== "ALL") {
      queryParams.set("type", searchAuctionType);
    }
    // Navigate even if params are empty, to a general search/browse page
    navigate(`/search?${queryParams.toString()}`);
  };

  return (
    <header className="bg-slate-900 text-slate-100 shadow-lg sticky top-0 z-50 border-b border-slate-700/50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        {/* Logo */}
        <Link
          to="/"
          className="text-2xl font-bold text-indigo-400 hover:text-indigo-300 transition-colors duration-200 ease-in-out"
        >
          AucHub
        </Link>

        {/* Search Bar */}
        <form
          onSubmit={handleSearchSubmit}
          className="hidden md:flex items-center flex-1 max-w-xl mx-4 bg-slate-800 border border-slate-700 rounded-lg overflow-hidden shadow-sm focus-within:ring-2 focus-within:ring-indigo-500 transition-all duration-200"
        >
          <select
            value={searchAuctionType}
            onChange={(e) => setSearchAuctionType(e.target.value)}
            className="cursor-pointer bg-slate-800 text-xs text-slate-200 pl-3 pr-2 py-2.5 focus:outline-none appearance-none"
            aria-label="Select auction type"
          >
            {AUCTION_TYPE_SEARCH_OPTIONS.map((opt) => (
              <option key={opt.key} value={opt.key}>
                {opt.label}
              </option>
            ))}
          </select>
          <span className="h-full border-l border-slate-700 mx-1"></span>
          <input
            type="search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search auctions..."
            className="flex-1 bg-slate-800 text-slate-100 text-sm px-3 py-2.5 placeholder-slate-500 focus:outline-none"
          />
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2.5 transition-colors duration-200 ease-in-out"
            aria-label="Search"
          >
            <FaSearch size={16} />
          </button>
        </form>

        {/* Icons & Profile */}
        <div className="flex items-center space-x-2 sm:space-x-3">
          {/* Notification */}
          <div className="relative" ref={notificationIconRef}>
            <button
              onClick={togglePanel}
              className="relative p-2 rounded-full text-slate-400 hover:bg-slate-700/80 hover:text-slate-100 transition-colors duration-200 ease-in-out"
              aria-label="Notifications"
            >
              <FaBell size={18} />
              {unreadCount > 0 && (
                <span className="absolute top-0 right-0 h-2.5 w-2.5 bg-red-500 rounded-full ring-2 ring-slate-900 animate-pulse" />
              )}
            </button>
            {/* Ensure NotificationPanel has a class like 'notification-panel-class' if it's not part of the ref */}
            <NotificationPanel
              isOpen={isPanelOpen}
              onClose={closePanel}
              onOpenAllNotifications={openAllNotificationsModal}
            />
          </div>

          <Link
            to="/following"
            className="p-2 rounded-full text-slate-400 hover:bg-slate-700/80 hover:text-slate-100 transition-colors duration-200 ease-in-out"
            aria-label="Following"
          >
            <FaHeart size={18} />
          </Link>

          <Link
            to="/my-orders"
            className="p-2 rounded-full text-slate-400 hover:bg-slate-700/80 hover:text-slate-100 transition-colors duration-200 ease-in-out"
            aria-label="My Orders"
          >
            <FaShoppingBag size={18} />
          </Link>
          
          <span className="h-6 w-px bg-slate-700 hidden sm:block"></span> {/* Divider */}

          {/* Profile Dropdown / Link */}
          {keycloak.authenticated ? (
            <div className="relative group">
              <button
                onClick={goToProfile}
                className="flex items-center gap-2 px-2 py-1.5 rounded-md hover:bg-slate-700/80 transition-colors duration-200 ease-in-out"
                aria-label="User profile"
              >
                <div className="w-7 h-7 rounded-full bg-indigo-500 flex items-center justify-center text-sm font-semibold text-white ring-1 ring-slate-700">
                  {userInitial || <FaUserCircle size={16}/>}
                </div>
                <span className="hidden lg:block text-sm font-medium text-slate-200 group-hover:text-slate-50 whitespace-nowrap">
                  {keycloak.tokenParsed?.preferred_username}
                </span>
              </button>
              {/* Basic Dropdown Example (can be expanded) */}
              {/* <div className="absolute right-0 mt-1 w-48 bg-slate-800 border border-slate-700 rounded-md shadow-lg py-1 hidden group-hover:block">
                <Link to="/profile" className="block px-4 py-2 text-sm text-slate-200 hover:bg-slate-700">Profile</Link>
                <button onClick={handleLogout} className="w-full text-left block px-4 py-2 text-sm text-rose-400 hover:bg-slate-700">Logout</button>
              </div> */}
            </div>
          ) : (
            <button 
              onClick={() => keycloak.login()}
              className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm px-4 py-2 rounded-md transition-colors duration-200 ease-in-out"
            >
              Login
            </button>
          )}
           {keycloak.authenticated && (
             <button
                onClick={handleLogout}
                className="hidden sm:block bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs px-3 py-1.5 rounded-md transition-colors duration-200 ease-in-out"
              >
                Logout
              </button>
           )}
        </div>
      </div>

      <AllNotificationsModal
        isOpen={isAllNotificationsModalOpen}
        onClose={closeAllNotificationsModal}
      />
    </header>
  );
}

export default Header;