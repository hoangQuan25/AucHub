// src/components/seller/tabs/listings/AuctionFilters.jsx
import React from 'react';

const AuctionFilters = ({
  auctionStatusFilter,
  setAuctionStatusFilter,
  auctionTimeFilter,
  setAuctionTimeFilter,
  statusTabs, // Expecting the STATUS_TABS array
  timeFilters, // Expecting the TIME_FILTERS array
  onFilterChange, // Callback to execute when a filter changes (e.g., to reset pagination)
}) => {
  const handleStatusFilterChange = (newStatus) => {
    setAuctionStatusFilter(newStatus);
    if (onFilterChange) onFilterChange();
  };

  const handleTimeFilterChange = (newTime) => {
    setAuctionTimeFilter(newTime);
    if (onFilterChange) onFilterChange();
  };

  return (
    <div className="flex flex-col sm:flex-row gap-4 mb-4 items-center">
      {/* Status Tabs */}
      <div className="flex flex-wrap gap-2 border border-gray-200 rounded-md p-1 bg-gray-50">
        {statusTabs.map((t) => (
          <button
            key={t.key}
            onClick={() => handleStatusFilterChange(t.key)}
            className={`px-3 py-1.5 text-xs sm:text-sm rounded-md transition-colors duration-150 ease-in-out focus:outline-none ${
              auctionStatusFilter === t.key
                ? "bg-indigo-600 text-white shadow"
                : "text-gray-600 hover:bg-indigo-100 hover:text-indigo-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {/* Time Filter Dropdown */}
      <div className="flex items-center gap-2 text-sm sm:ml-auto"> {/* Added sm:ml-auto */}
        <label htmlFor="auctionTimeFilter" className="text-gray-600">Time:</label>
        <select
          id="auctionTimeFilter"
          value={auctionTimeFilter}
          onChange={(e) => handleTimeFilterChange(e.target.value)}
          className="border rounded px-2 py-1.5 text-sm focus:ring-indigo-500 focus:border-indigo-500" // Adjusted padding
        >
          {timeFilters.map((f) => (
            <option key={f.key} value={f.key}>{f.label}</option>
          ))}
        </select>
      </div>
    </div>
  );
};

export default AuctionFilters;