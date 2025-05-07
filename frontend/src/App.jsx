import React, { useEffect } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import UserInfoPage from "./pages/UserInfoPage"; // Renamed/New page
import ProductsPage from "./pages/ProductsPage"; // New page for sellers
import MainLayout from "./layouts/MainLayout"; // Import the layout
import apiClient, { setupAuthInterceptor } from "./api/apiClient";
import LiveAuctionDetailPage from "./pages/LiveAuctionDetailPage"; // New page for auction details
import TestPage from "./pages/TestPage";
import MyAuctionsPage from "./pages/MyAuctionsPage";
import TimedAuctionDetailPage from "./pages/TimedAuctionDetailPage";
import AllAuctionsPage from "./pages/AllAuctionsPage";
import { NotificationProvider } from "./context/NotificationContext";
import FollowingAuctionsPage from "./pages/FollowingAuctionsPage";

// PrivateRoute now just checks auth, Layout handles UI structure
const PrivateRoute = ({ children }) => {
  const { keycloak } = useKeycloak();
  return keycloak.authenticated ? children : <Navigate to="/login" />;
};

// Component to specifically check for Seller role
const SellerRoute = ({ children }) => {
  const { keycloak } = useKeycloak();
  // Must be authenticated AND have the seller role
  return keycloak.authenticated && keycloak.hasRealmRole("ROLE_SELLER") ? (
    children
  ) : (
    <Navigate to="/" />
  ); // Or redirect to an "unauthorized" page
};

function App() {
  const { keycloak, initialized } = useKeycloak();

  useEffect(() => {
    if (initialized && keycloak) {
      setupAuthInterceptor(keycloak);
    }
  }, [initialized, keycloak]);

  if (!initialized) {
    return <div>Loading...</div>;
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* Public Login Route */}
        <Route
          path="/login"
          element={
            !keycloak.authenticated ? <LoginPage /> : <Navigate to="/" />
          }
        />

        {/* Protected Routes using MainLayout */}
        <Route
          element={
            <PrivateRoute>
              <NotificationProvider>
                <MainLayout />
              </NotificationProvider>
            </PrivateRoute>
          }
        >
          <Route path="/" element={<HomePage />} />
          <Route path="/all-auctions" element={<AllAuctionsPage />} />
          <Route
            path="/live-auctions/:auctionId"
            element={<LiveAuctionDetailPage />}
          />
          <Route
            path="/timed-auctions/:auctionId"
            element={<TimedAuctionDetailPage />}
          />
          <Route path="/profile" element={<UserInfoPage />} />
          <Route path="/following" element={<FollowingAuctionsPage />} />
          {/* Seller-specific Route */}
          <Route
            path="/my-products"
            element={
              <SellerRoute>
                <ProductsPage />
              </SellerRoute>
            }
          />
          <Route
            path="/my-auctions"
            element={
              <SellerRoute>
                <MyAuctionsPage />
              </SellerRoute>
            }
          />
          {/* Add other protected routes here */}
        </Route>

        {/* Optional: Catch-all route for 404 */}
        {/* <Route path="*" element={<NotFoundPage />} /> */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
