// src/components/Header.jsx
import React, { useState, useEffect, useRef } from "react"; // Added useState, useEffect, useRef
import { Link, useNavigate } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { FaBell, FaHeart, FaShoppingBag, FaSearch } from "react-icons/fa"; // Import bell icon
import NotificationPanel from "./NotificationPanel"; // Import the panel
import AllNotificationsModal from "./AllNotificationsModal";
import { useNotifications } from "../context/NotificationContext"; // Import the context

const AUCTION_TYPE_SEARCH_OPTIONS = [
  { key: "ALL", label: "All Auctions" },
  { key: "LIVE", label: "Live Auctions" },
  { key: "TIMED", label: "Timed Auctions" },
];

function Header() {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const { unreadCount } = useNotifications();

  // State to control notification panel visibility
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const notificationIconRef = useRef(null); // Ref for the bell icon/button area
  const [isAllNotificationsModalOpen, setIsAllNotificationsModalOpen] =
    useState(false);

  const [searchTerm, setSearchTerm] = useState("");
  const [searchAuctionType, setSearchAuctionType] = useState(
    AUCTION_TYPE_SEARCH_OPTIONS[0].key
  );

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

  const handleSearchSubmit = (e) => {
    e.preventDefault(); // If using a form wrapper
    if (searchTerm.trim() || searchAuctionType !== "ALL") {
      const queryParams = new URLSearchParams();
      if (searchTerm.trim()) {
        queryParams.set("query", searchTerm.trim());
      }
      if (searchAuctionType !== "ALL") {
        // Only add type if not "ALL" to keep URL cleaner, or always add
        queryParams.set("type", searchAuctionType);
      }
      navigate(`/search?${queryParams.toString()}`);
    } else {
      // Optional: Navigate to a general auction Browse page if search is empty
      // or just don't do anything. For now, let's require some input or non-default type.
      navigate("/search"); // Or navigate to a default search/browse page
    }
  };

  return (
  <header className="bg-gray-900 text-white shadow-md">
  <div className="container mx-auto px-4 py-3 flex items-center justify-between">
    {/* ← Logo bên trái */}
    <Link
      to="/"
      className="text-2xl font-bold hover:text-indigo-400 transition cursor-pointer"
    >
      AucHub
    </Link>

    {/* — Search ở giữa — */}
    <form
      onSubmit={handleSearchSubmit}
      className="flex items-center flex-1 max-w-lg mx-4 bg-gray-800 border border-gray-700 rounded-md overflow-hidden"
    >
      <select
        value={searchAuctionType}
        onChange={e => setSearchAuctionType(e.target.value)}
        className="cursor-pointer bg-gray-800 text-sm text-white px-3 py-2 focus:outline-none"
      >
        {AUCTION_TYPE_SEARCH_OPTIONS.map(opt => (
          <option key={opt.key} value={opt.key}>
            {opt.label}
          </option>
        ))}
      </select>

      <input
        type="search"
        value={searchTerm}
        onChange={e => setSearchTerm(e.target.value)}
        placeholder="Search auctions..."
        className="flex-1 bg-gray-800 text-white text-sm px-3 py-2 placeholder-gray-400 focus:outline-none"
      />

      <button
        type="submit"
        className="cursor-pointer bg-indigo-600 hover:bg-indigo-700 px-4 py-2 transition"
        aria-label="Search"
      >
        <FaSearch size={18} />
      </button>
    </form>

    {/* → Icons & Profile bên phải */}
    <div className="flex items-center space-x-4">
      {/* Notification */}
      <div className="relative" ref={notificationIconRef}>
        <button
          onClick={togglePanel}
          className="relative p-2 rounded-full hover:bg-gray-800 text-gray-300 hover:text-white transition cursor-pointer"
          aria-label="Notifications"
        >
          <FaBell size={18} />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 h-2 w-2 bg-red-500 rounded-full ring-2 ring-gray-900" />
          )}
        </button>
        <NotificationPanel
          isOpen={isPanelOpen}
          onClose={closePanel}
          onOpenAllNotifications={openAllNotificationsModal}
        />
      </div>

      {/* Following */}
      <Link
        to="/following"
        className="p-2 rounded-full hover:bg-gray-800 text-gray-300 hover:text-white transition cursor-pointer"
      >
        <FaHeart size={18} />
      </Link>

      {/* Orders */}
      <Link
        to="/my-orders"
        className="p-2 rounded-full hover:bg-gray-800 text-gray-300 hover:text-white transition cursor-pointer"
      >
        <FaShoppingBag size={18} />
      </Link>

      {/* Profile */}
      <div
        onClick={goToProfile}
        className="flex items-center gap-2 px-2 py-1 rounded-md hover:bg-gray-800 cursor-pointer transition"
      >
        <div className="w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center font-semibold">
          {keycloak.tokenParsed?.preferred_username?.[0]?.toUpperCase() || "U"}
        </div>
        <span className="text-sm font-medium whitespace-nowrap">
          {keycloak.tokenParsed?.preferred_username}
        </span>
      </div>

      {/* Logout */}
      <button
        onClick={handleLogout}
        className="cursor-pointer bg-red-600 hover:bg-red-700 text-white font-semibold text-sm px-4 py-2 rounded-md transition"
      >
        Logout
      </button>
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
