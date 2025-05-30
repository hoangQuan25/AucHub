import React, { useEffect } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { ToastContainer } from "react-toastify";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import UserInfoPage from "./pages/UserInfoPage"; // Renamed/New page
import MainLayout from "./layouts/MainLayout"; // Import the layout
import apiClient, { setupAuthInterceptor } from "./api/apiClient";
import LiveAuctionDetailPage from "./pages/LiveAuctionDetailPage"; // New page for auction details
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
        theme="light" // Or "dark", "colored"
      />
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
          <Route
            path="/live-auctions/:auctionId"
            element={<LiveAuctionDetailPage />}
          />
          <Route
            path="/timed-auctions/:auctionId"
            element={<TimedAuctionDetailPage />}
          />
          <Route path="/profile" element={<UserInfoPage />} />
          <Route
            path="/auction-rules-guide"
            element={<AuctionRulesGuidePage />}
          />
          <Route path="/following" element={<FollowingAuctionsPage />} />
          <Route path="/my-orders" element={<MyOrdersPage />} />
          <Route path="/orders/:orderId" element={<OrderDetailPage />} />
          <Route path="/search" element={<AuctionSearchPage />} />
          <Route
            path="/seller/:identifier"
            element={<PublicSellerProfilePage />}
          />
          {/* Seller-specific Route */}
        </Route>

        {/* Optional: Catch-all route for 404 */}
        {/* <Route path="*" element={<NotFoundPage />} /> */}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
