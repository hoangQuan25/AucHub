// src/components/Sidebar.jsx – now includes the new “My Auctions” link for sellers
import React from 'react';
import { NavLink } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';

function Sidebar() {
  const { keycloak } = useKeycloak();
  const isSeller = keycloak.hasRealmRole('ROLE_SELLER');

  const linkStyle = "block py-2 px-4 rounded hover:bg-gray-700";
  const activeLinkStyle = "bg-gray-700";

  return (
    <aside className="w-64 bg-gray-900 text-white p-4 flex flex-col">
      <nav className="flex-grow">
        <ul>
          <li>
            <NavLink
              to="/"
              end
              className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ''}`}
            >
              Homepage
            </NavLink>
          </li>

          <li>
            <NavLink
              to="/live-auctions"
              className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ''}`}
            >
              Live Auctions
            </NavLink>
          </li>

          {isSeller && (
            <>
              <li>
                <NavLink
                  to="/my-auctions"
                  className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ''}`}
                >
                  Your Auctions
                </NavLink>
              </li>
              <li>
                <NavLink
                  to="/my-products"
                  className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ''}`}
                >
                  Your Products
                </NavLink>
              </li>
            </>
          )}

          <li>
            <NavLink
              to="/profile"
              className={({ isActive }) => `${linkStyle} ${isActive ? activeLinkStyle : ''}`}
            >
              Account Settings
            </NavLink>
          </li>
        </ul>
      </nav>

      {/* Optional footer */}
      <div className="mt-auto text-xs text-gray-500" />
    </aside>
  );
}

export default Sidebar;
