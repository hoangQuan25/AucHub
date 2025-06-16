import React from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { Navigate } from 'react-router-dom';

function LoginPage() {
  const { keycloak } = useKeycloak();

  if (keycloak.authenticated) {
    return <Navigate to="/" />;
  }

  return (
    <div className="text-center mt-20">
      <h1 className="text-2xl font-bold mb-4">Welcome!</h1>
      <p className="mb-6">Please log in or register to continue.</p>
      <button
        onClick={() => keycloak.login()}
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded mr-2"
      >
        Login
      </button>
      <button
        onClick={() => keycloak.register()}
        className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
      >
        Register
      </button>
    </div>
  );
}

export default LoginPage;