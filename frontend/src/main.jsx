// src/index.js
import React from 'react';
import ReactDOM from 'react-dom/client';
import { ReactKeycloakProvider } from '@react-keycloak/web';
import Keycloak from 'keycloak-js';
import App from './App';
import './index.css';

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
});

const root = ReactDOM.createRoot(document.getElementById('root'));

const handleTokens = (tokens) => {
  if (tokens.token) localStorage.setItem('accessToken', tokens.token);
};

root.render(
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
         onLoad: 'check-sso', 
         silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
         pkceMethod: 'S256',
      }}
      onTokens={handleTokens}
    >
      <App />
    </ReactKeycloakProvider>
);