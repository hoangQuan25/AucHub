import React, { useState } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import apiClient from './apiClient';

function AccountSettingsPage() {
  const { keycloak } = useKeycloak();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const isSeller = keycloak.hasRealmRole('ROLE_SELLER'); 

  const handleBecomeSeller = async () => {
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      await apiClient.post('users/me/activate-seller');
      setSuccess('Account successfully upgraded to Seller! Please refresh or re-login to see changes.');
      await keycloak.updateToken(-1);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upgrade account.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold mb-4">Account Settings</h2>

      <div className="mt-6 border-t pt-4">
        <h3 className="text-xl font-semibold mb-2">Seller Status</h3>
        {error && <p className="text-red-500 mb-2">{error}</p>}
        {success && <p className="text-green-500 mb-2">{success}</p>}
        {isSeller ? (
          <p className="text-green-600 font-medium">You are already registered as a Seller.</p>
        ) : (
          <div>
            <p className="mb-2">Upgrade your account to start selling products.</p>
            <button
              onClick={handleBecomeSeller}
              disabled={isLoading}
              className="bg-purple-500 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded disabled:opacity-50"
            >
              {isLoading ? 'Processing...' : 'Become a Seller'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default AccountSettingsPage;