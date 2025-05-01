import React, { useState } from "react";
import { FaChevronDown } from "react-icons/fa";

const CollapsibleSection = ({ title, defaultOpen = false, children }) => {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className="rounded-xl border bg-white shadow-sm overflow-hidden mb-4">
      <button
        onClick={() => setOpen((prev) => !prev)}
        className="w-full flex items-center justify-between px-4 py-3 text-sm font-semibold text-gray-800 hover:bg-gray-50 transition"
      >
        <span>{title}</span>
        <FaChevronDown
          className={`transform transition-transform duration-300 ${
            open ? "rotate-180" : ""
          }`}
          size={14}
        />
      </button>

      <div
        className={`transition-all duration-300 ease-in-out ${
          open ? "max-h-[1000px] py-3 px-5" : "max-h-0 overflow-hidden px-5"
        } text-sm text-gray-700`}
      >
        {children}
      </div>
    </div>
  );
};

export default CollapsibleSection;
