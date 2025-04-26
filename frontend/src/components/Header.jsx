import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useKeycloak } from '@react-keycloak/web';

function Header() {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();

  const handleLogout = () => {
    keycloak.logout({ redirectUri: window.location.origin }); // Redirect home after logout
  };

  const goToProfile = () => {
    navigate('/profile'); // Navigate to user info page
  };

  return (
    <header className="p-4 bg-gray-800 text-white flex justify-between items-center shadow-md">
      <Link to="/" className="text-2xl font-bold hover:text-gray-300">
        AucHub
      </Link>
      <div>
        {keycloak.authenticated ? (
          <div className="flex items-center space-x-4">
             {/* User Menu/Avatar Section */}
             <div onClick={goToProfile} className="flex items-center cursor-pointer hover:bg-gray-700 p-2 rounded">
                {/* Placeholder for Avatar */}
                <div className="w-8 h-8 rounded-full bg-gray-500 mr-2 flex items-center justify-center">
                    <span className="text-sm font-semibold">{keycloak.tokenParsed?.preferred_username?.charAt(0).toUpperCase() || 'U'}</span>
                </div>
                <span className="font-medium">{keycloak.tokenParsed?.preferred_username}</span>
             </div>
            <button
              onClick={handleLogout}
              className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
            >
              Logout
            </button>
          </div>
        ) : (
          <>
            {/* Login/Register buttons shown previously in App.jsx */}
            <button onClick={() => keycloak.login()} className="ml-2 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
              Login
            </button>
            <button onClick={() => keycloak.register()} className="ml-2 bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded">
              Register
            </button>
          </>
        )}
      </div>
    </header>
  );
}

export default Header;