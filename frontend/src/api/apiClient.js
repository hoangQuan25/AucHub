// apiClient.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, // Adjust to your API Gateway base path if needed
  // Other Axios configurations
});

let interceptorId = null; // Keep track of the interceptor

// Function to set up the interceptor with the keycloak instance
export const setupAuthInterceptor = (keycloakInstance) => {
  // Eject previous interceptor if it exists, to avoid duplicates on re-renders/updates
  if (interceptorId !== null) {
    apiClient.interceptors.request.eject(interceptorId);
  }

  interceptorId = apiClient.interceptors.request.use(
    (config) => {
      // Check if Keycloak is authenticated and has a token
      if (keycloakInstance?.authenticated && keycloakInstance?.token) {
        config.headers.Authorization = `Bearer ${keycloakInstance.token}`;
      }
      return config;
    },
    (error) => {
      // Handle request errors
      return Promise.reject(error);
    }
  );
  console.log('Auth interceptor set up.'); // For debugging
};

// Optional: Interceptor for response handling (e.g., refresh token - more advanced)
// apiClient.interceptors.response.use(...)

export default apiClient;