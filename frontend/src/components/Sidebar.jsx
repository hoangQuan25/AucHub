// src/components/Sidebar.jsx
import React from "react";
import { NavLink } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";
import { FaHome, FaUserCog, FaStore, FaBookOpen, FaPlusCircle, FaThList } from "react-icons/fa"; // Example icons

function Sidebar() {
  const { keycloak } = useKeycloak();
  const isSeller = keycloak.hasRealmRole("ROLE_SELLER");
  const username = keycloak.tokenParsed?.preferred_username;

  const baseLinkClasses = "flex items-center space-x-3 py-2.5 px-4 rounded-lg transition-all duration-200 ease-in-out group";
  const activeLinkClasses = "bg-indigo-500 text-white font-semibold shadow-md hover:bg-indigo-400";
  const inactiveLinkClasses = "text-slate-300 hover:bg-slate-700 hover:text-slate-100";
  
  // A helper for NavLink className prop
  const getNavLinkClass = ({ isActive }) => `${baseLinkClasses} ${isActive ? activeLinkClasses : inactiveLinkClasses}`;

  return (
    <aside className="w-64 bg-slate-800 text-slate-200 p-5 flex flex-col space-y-6 shadow-lg">
      
      <nav className="flex-grow">
        <ul className="space-y-2">
          <li>
            <NavLink to="/" end className={getNavLinkClass}>
              <FaHome className="text-slate-400 group-hover:text-slate-200 transition-colors group-[.bg-indigo-500]:text-indigo-100" size={18} />
              <span>Homepage</span>
            </NavLink>
          </li>
          <li>
            <NavLink to="/auction-rules-guide" className={getNavLinkClass}>
              <FaBookOpen className="text-slate-400 group-hover:text-slate-200 transition-colors group-[.bg-indigo-500]:text-indigo-100" size={18} />
              <span>Auction Rules</span>
            </NavLink>
          </li>

          {isSeller && (
            <>
              {username && (
                <li>
                  <NavLink to={`/seller/${username}`} className={getNavLinkClass}>
                    <FaStore className="text-slate-400 group-hover:text-slate-200 transition-colors group-[.bg-indigo-500]:text-indigo-100" size={18} />
                    <span>My Shop</span>
                  </NavLink>
                </li>
              )}
               
            </>
          )}

          <li>
            <NavLink to="/profile" className={getNavLinkClass}>
              <FaUserCog className="text-slate-400 group-hover:text-slate-200 transition-colors group-[.bg-indigo-500]:text-indigo-100" size={18} />
              <span>Account Settings</span>
            </NavLink>
          </li>
        </ul>
      </nav>
    </aside>
  );
}

export default Sidebar;