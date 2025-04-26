import React from 'react';
import { NavLink } from 'react-router-dom'; // Use NavLink for active styling
import { useKeycloak } from '@react-keycloak/web';

function Sidebar() {
  const { keycloak } = useKeycloak();
  const isSeller = keycloak.hasRealmRole('ROLE_SELLER');

  const linkStyle = "block py-2 px-4 rounded hover:bg-gray-700";
  const activeLinkStyle = "bg-gray-700"; // Tailwind class for active link

  return (
    <aside className="w-64 bg-gray-900 text-white p-4 flex flex-col">
      <nav className="flex-grow">
        <ul>
          <li>
            <NavLink
              to="/"
              className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ""}`}
              end // Ensure exact match for homepage
            >
              Homepage
            </NavLink>
          </li>
          <li>
            <NavLink
              to="/live-auctions" // Can point to '/' initially if needed
              className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ""}`}
            >
              Live Auctions
            </NavLink>
          </li>
          {/* Conditional Seller Link */}
          {isSeller && (
             <li>
               <NavLink
                 to="/my-products"
                 className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ""}`}
               >
                 Your Products
               </NavLink>
             </li>
          )}
           {/* Add more links like "My Bids", "Account Settings" here */}
           <li>
             <NavLink
               to="/profile" // Link to user info page
               className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ""}`}
             >
               Account Settings
             </NavLink>
           </li>
        </ul>
      </nav>
      <div className="mt-auto text-xs text-gray-500">
        {/* Footer info if needed */}
      </div>
    </aside>
  );
}

export default Sidebar;