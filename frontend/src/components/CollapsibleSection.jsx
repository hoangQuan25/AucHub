import React, { useState } from "react";
import { FaChevronDown } from "react-icons/fa";

const CollapsibleSection = ({ title, defaultOpen = false, children }) => {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className="border rounded-lg mb-4 bg-white shadow-sm overflow-hidden">
      <button
        className="w-full flex justify-between items-center px-4 py-3 text-left font-semibold text-gray-800 hover:bg-gray-50 transition"
        onClick={() => setOpen((o) => !o)}
      >
        <span>{title}</span>
        <span
          className={`transform transition-transform duration-300 ${
            open ? "rotate-180" : ""
          }`}
        >
          <FaChevronDown size="0.9em" />
        </span>
      </button>

      <div
        className={`px-4 text-sm text-gray-700 transition-all duration-300 ${
          open ? "max-h-[1000px] py-3" : "max-h-0 overflow-hidden"
        }`}
      >
        {children}
      </div>
    </div>
  );
};

export default CollapsibleSection;
