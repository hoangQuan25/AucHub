import React from 'react';
import ReactDOM from 'react-dom/client';
import { ReactKeycloakProvider } from '@react-keycloak/web';
import Keycloak from 'keycloak-js';
import App from './App';
import './index.css'; // Import Tailwind CSS

// Khởi tạo instance Keycloak
const keycloak = new Keycloak({
  url: 'https://localhost:8443', // Thay bằng URL Keycloak của bạn
  realm: 'auction-realm',
  clientId: 'auction-react-client',
});

const root = ReactDOM.createRoot(document.getElementById('root'));

// (Optional) Callback khi token được cập nhật
const handleTokens = (tokens) => {
  if (tokens.token) localStorage.setItem('accessToken', tokens.token);
  // console.log('Tokens Refreshed:', tokens);
};

root.render(
  // <React.StrictMode>
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
         onLoad: 'login-required', // 'check-sso' hoặc 'login-required'
        //  silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
         pkceMethod: 'S256',
      }}
      onTokens={handleTokens}
      // Có thể thêm các event listener khác ở đây nếu cần
    >
      <App />
    </ReactKeycloakProvider>
  // </React.StrictMode>
);