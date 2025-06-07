// src/App.js
import React, { useEffect } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { BrowserRouter, Route, Routes, Navigate, Outlet } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import UserInfoPage from "./pages/UserInfoPage";
import MainLayout from "./layouts/MainLayout";
import { setupAuthInterceptor } from "./api/apiClient";
import LiveAuctionDetailPage from "./pages/LiveAuctionDetailPage";
import TimedAuctionDetailPage from "./pages/TimedAuctionDetailPage";
import { NotificationProvider } from "./context/NotificationContext";
import FollowingAuctionsPage from "./pages/FollowingAuctionsPage";
import MyOrdersPage from "./pages/MyOrdersPage";
import OrderDetailPage from "./pages/OrderDetailPage";
import PublicSellerProfilePage from "./pages/PublicSellerProfilePage";
import AuctionSearchPage from "./pages/AuctionSearchPage";
import AuctionRulesGuidePage from "./pages/AuctionRulesGuidePage";

// PrivateRoute now just checks auth, Layout handles UI structure
const PrivateRoute = ({ children }) => {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    return <div>Loading...</div>; // Or a spinner
  }

  // Redirect to login if not authenticated
  return keycloak.authenticated ? children : <Navigate to="/login" replace />;
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
    if (initialized && keycloak.authenticated) {
      setupAuthInterceptor(keycloak);
    }
  }, [initialized, keycloak.authenticated]);

  if (!initialized) {
    return <div>Loading...</div>;
  }

  return (
    <BrowserRouter>
      <ToastContainer
        position="top-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={true}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="light"
      />
      <NotificationProvider>
        <Routes>
          {/* Public Login Route */}
          <Route
            path="/login"
            element={
              !keycloak.authenticated ? <LoginPage /> : <Navigate to="/" />
            }
          />

          {/* All other routes are nested under MainLayout */}
          <Route element={<MainLayout />}>
            {/* --- PUBLIC ROUTES --- */}
            {/* These routes are accessible to everyone */}
            <Route path="/" element={<HomePage />} />
            <Route
              path="/live-auctions/:auctionId"
              element={<LiveAuctionDetailPage />}
            />
            <Route
              path="/timed-auctions/:auctionId"
              element={<TimedAuctionDetailPage />}
            />
            <Route
              path="/auction-rules-guide"
              element={<AuctionRulesGuidePage />}
            />
            <Route path="/search" element={<AuctionSearchPage />} />
            <Route
              path="/seller/:identifier"
              element={<PublicSellerProfilePage />}
            />

            {/* --- PRIVATE ROUTES --- */}
            {/* These routes require the user to be logged in */}
            <Route
              path="/profile"
              element={<PrivateRoute><UserInfoPage /></PrivateRoute>}
            />
            <Route
              path="/following"
              element={<PrivateRoute><FollowingAuctionsPage /></PrivateRoute>}
            />
            <Route
              path="/my-orders"
              element={<PrivateRoute><MyOrdersPage /></PrivateRoute>}
            />
            <Route
              path="/orders/:orderId"
              element={<PrivateRoute><OrderDetailPage /></PrivateRoute>}
            />
            
            {/* Example for a seller-specific route if needed in the future */}
            {/* <Route path="/dashboard" element={<SellerRoute><DashboardPage /></SellerRoute>} /> */}
          </Route>

          {/* Optional: Catch-all route for 404 */}
          {/* <Route path="*" element={<NotFoundPage />} /> */}
        </Routes>
      </NotificationProvider>
    </BrowserRouter>
  );
}

export default App;