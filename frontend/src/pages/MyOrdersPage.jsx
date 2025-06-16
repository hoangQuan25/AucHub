// src/pages/MyOrdersPage.jsx
import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import CountdownTimer from "../components/CountdownTimer";
import {
  buyerOrderStatusFilters,
  orderStatusMap,
} from "../constants/orderConstants";
import apiClient from "../api/apiClient";
import { useKeycloak } from "@react-keycloak/web";

function MyOrdersPage() {
  const [activeFilter, setActiveFilter] = useState("ALL"); // e.g., 'ALL', 'PENDING_PAYMENT'
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const { keycloak, initialized } = useKeycloak();

  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      const fetchOrders = async () => {
        setIsLoading(true);
        setError(null);
        try {
          // Backend needs to map 'PENDING_PAYMENT' or 'CANCELLED' to multiple statuses
          const statusParamForApi =
            activeFilter === "ALL" ? "" : `&status=${activeFilter}`;
          const response = await apiClient.get(
            `/orders/my?page=0&size=10${statusParamForApi}`
          );
          setOrders(response.data.content || []);
        } catch (err) {
          console.error("Failed to fetch orders:", err);
          setError(
            err.response?.data?.message || "Could not load your orders."
          );
          setOrders([]);
        } finally {
          setIsLoading(false);
        }
      };
      fetchOrders();
    } else if (initialized && !keycloak.authenticated) {
    }
  }, [activeFilter, initialized, keycloak.authenticated]);

  if (!initialized) {
    return <div className="text-center p-10">Initializing...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 bg-gray-100 min-h-screen">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">
        My orders
      </h1>

      <div className="mb-6 bg-white shadow-sm rounded-md overflow-hidden">
        <nav className="flex flex-wrap border-b border-gray-200">
          {Object.entries(buyerOrderStatusFilters).map(
            ([key, displayValue]) => (
              <button
                key={key}
                onClick={() => setActiveFilter(key)} // key is 'ALL', 'PENDING_PAYMENT', etc.
                className={`py-3 px-4 sm:px-6 text-sm font-medium focus:outline-none transition-colors duration-150 ease-in-out ${
                  activeFilter === key
                    ? "text-red-600 border-b-2 border-red-600 font-semibold" // Added font-semibold
                    : "text-gray-600 hover:text-gray-800 hover:border-gray-300 border-b-2 border-transparent"
                }`}
              >
                {displayValue}{" "}
                {/* Display value from buyerOrderStatusFilters */}
              </button>
            )
          )}
        </nav>
      </div>

      {/* Order List */}
      {isLoading && (
        <div className="text-center py-10">Loading orders...</div>
      )}
      {!isLoading && error && (
        <div className="text-center py-10 text-red-600 bg-white rounded-md shadow-sm">
          {error}
        </div>
      )}
      {!isLoading && !error && orders.length === 0 && (
        <div className="text-center py-10 bg-white rounded-md shadow-sm">
          <p className="text-gray-500">
            No orders available{" "}
            {activeFilter !== "ALL"
              ? `cho trạng thái "${buyerOrderStatusFilters[activeFilter]}"`
              : ""}
            .
          </p>
        </div>
      )}
      {!isLoading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map(
            (
              order // order.status will be actual backend status key
            ) => (
              <div
                key={order.id}
                className="bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden"
              >
                <div className="px-4 py-3 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                  <span className="font-semibold text-sm text-gray-700">
                    {/* Assuming order DTO for buyer has sellerUsernameSnapshot or similar */}
                    Seller:{" "}
                    {order.sellerUsernameSnapshot || order.sellerName || "N/A"}
                  </span>
                  <span
                    className={`inline-flex items-center gap-1 text-sm font-semibold px-3 py-1 rounded-full shadow-sm border ${
                      order.status === "COMPLETED"
                        ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                        : order.status?.includes("CANCELLED")
                        ? "bg-rose-100 text-rose-700 border-rose-300"
                        : [
                            "AWAITING_WINNER_PAYMENT",
                            "AWAITING_NEXT_BIDDER_PAYMENT",
                          ].includes(order.status)
                        ? "bg-yellow-100 text-yellow-800 border-yellow-300"
                        : order.status === "PAYMENT_SUCCESSFUL"
                        ? "bg-green-100 text-green-800 border-green-300"
                        : [
                            "AWAITING_FULFILLMENT_CONFIRMATION",
                            "AWAITING_SHIPMENT",
                          ].includes(order.status)
                        ? "bg-sky-100 text-sky-800 border-sky-300"
                        : order.status === "ORDER_SUPERSEDED_BY_REOPEN"
                        ? "bg-gray-200 text-gray-800 border-gray-300"
                        : "bg-slate-100 text-slate-700 border-slate-300"
                    }`}
                  >
                    {order.status?.includes("CANCELLED") && <span>❌</span>}
                    {order.status === "COMPLETED" && <span>✅</span>}
                    {[
                      "AWAITING_WINNER_PAYMENT",
                      "AWAITING_NEXT_BIDDER_PAYMENT",
                    ].includes(order.status) && <span>💰</span>}
                    {order.status === "PAYMENT_SUCCESSFUL" && <span>✔️</span>}
                    {[
                      "AWAITING_SHIPMENT",
                      "AWAITING_FULFILLMENT_CONFIRMATION",
                    ].includes(order.status) && <span>📦</span>}

                    {orderStatusMap[order.status] ||
                      order.status.replace(/_/g, " ")}
                  </span>
                </div>

                {order.items &&
                  order.items.map((item) => (
                    <Link
                      to={`/orders/${order.id}`}
                      key={item.productId || item.id}
                      className="block hover:bg-gray-50 transition duration-150 ease-in-out"
                    >
                      <div className="flex items-center gap-4 p-4 border-b border-gray-100 last:border-b-0">
                        <img
                          src={item.imageUrl || "/placeholder.png"}
                          alt={item.title}
                          className="w-16 h-16 object-cover rounded border border-gray-200 flex-shrink-0"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = "/placeholder.png";
                          }}
                        />
                        <div className="flex-grow">
                          <p className="text-sm font-medium text-gray-800 mb-1">
                            {item.title}
                          </p>
                          {item.variation && (
                            <p className="text-xs text-gray-500">
                              {item.variation}
                            </p>
                          )}
                          <p className="text-xs text-gray-500">
                            x{item.quantity}
                          </p>
                        </div>
                        <div className="text-sm font-semibold text-gray-800 text-right">
                          {item.price.toLocaleString("vi-VN")} VNĐ
                        </div>
                      </div>
                    </Link>
                  ))}

                <div className="px-4 py-3 bg-gray-50 flex flex-col sm:flex-row justify-end items-center gap-4">
                  {(order.status === "AWAITING_WINNER_PAYMENT" ||
                    order.status === "AWAITING_NEXT_BIDDER_PAYMENT") &&
                    order.paymentDeadline && (
                      <div className="text-xs text-orange-600 font-medium flex items-center gap-1 mr-auto">
                        {" "}
                        {/* Pushed to left */}
                        <span>Please make payment in:</span>
                        <CountdownTimer
                          endTimeMillis={new Date(
                            order.paymentDeadline
                          ).getTime()}
                        />
                      </div>
                    )}
                  <div className="text-right">
                    <span className="text-sm text-gray-600">Total: </span>
                    <span className="text-lg font-semibold text-red-600">
                      {(order.totalPrice || 0).toLocaleString("vi-VN")} VNĐ
                    </span>
                  </div>

                </div>
              </div>
            )
          )}
        </div>
      )}
    </div>
  );
}

export default MyOrdersPage;
