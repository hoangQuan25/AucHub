// src/pages/MyOrdersPage.jsx
import React, { useState, useEffect } from 'react'; // Added useEffect
import { Link } from 'react-router-dom';
import CountdownTimer from '../components/CountdownTimer';
import { buyerOrderStatusFilters, orderStatusMap } from '../constants/orderConstants';
import apiClient from '../api/apiClient'; // Your configured Axios instance
import { useKeycloak } from '@react-keycloak/web'; // To ensure user is authenticated

function MyOrdersPage() {
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [orders, setOrders] = useState([]); // State for fetched orders
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const { keycloak, initialized } = useKeycloak();

  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      const fetchOrders = async () => {
        setIsLoading(true);
        setError(null);
        try {
          // Adjust the API endpoint and status key as per your backend implementation
          const statusParam = activeFilter === 'ALL' ? '' : `&status=${activeFilter}`;
          // Assuming your Orders service API for "my orders" is at /api/v1/orders/my
          // The actual endpoint in your gateway might be /orders/my
          const response = await apiClient.get(`/orders/my?page=0&size=10${statusParam}`); // Added pagination params
          setOrders(response.data.content || []); // Assuming Spring Pageable response
        } catch (err) {
          console.error("Failed to fetch orders:", err);
          setError(err.response?.data?.message || "Could not load your orders.");
          setOrders([]); // Clear orders on error
        } finally {
          setIsLoading(false);
        }
      };
      fetchOrders();
    } else if (initialized && !keycloak.authenticated) {
        setError("Please log in to view your orders.");
        setOrders([]);
        setIsLoading(false);
    }
  }, [activeFilter, initialized, keycloak.authenticated]); // Refetch when filter or auth status changes

  // Use 'orders' state instead of 'filteredOrders' if API does the filtering
  // If API doesn't filter by status, you'd filter 'orders' here.
  // For this example, assume API filters by status if statusParam is provided.

  if (!initialized) {
    return <div className="text-center p-10">Initializing...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 bg-gray-100 min-h-screen">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">Đơn Hàng Của Tôi</h1>

      {/* Filter Tabs */}
      <div className="mb-6 bg-white shadow-sm rounded-md overflow-hidden">
        <nav className="flex flex-wrap border-b border-gray-200">
          {Object.entries(buyerOrderStatusFilters).map(([key, value]) => (
            <button
              key={key}
              onClick={() => setActiveFilter(key)}
              className={`py-3 px-4 sm:px-6 text-sm font-medium focus:outline-none transition-colors duration-150 ease-in-out ${
                activeFilter === key
                  ? 'text-red-600 border-b-2 border-red-600'
                  : 'text-gray-600 hover:text-gray-800'
              }`}
            >
              {value}
            </button>
          ))}
        </nav>
      </div>

      {/* Order List */}
      {isLoading && <div className="text-center py-10">Loading orders...</div>}
      {!isLoading && error && <div className="text-center py-10 text-red-600 bg-white rounded-md shadow-sm">{error}</div>}
      {!isLoading && !error && orders.length === 0 && (
         <div className="text-center py-10 bg-white rounded-md shadow-sm">
            <p className="text-gray-500">Không có đơn hàng nào {activeFilter !== 'ALL' ? `cho trạng thái "${buyerOrderStatusFilters[activeFilter]}"` : ''}.</p>
         </div>
      )}
      {!isLoading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => ( // Map over fetched 'orders'
            <div key={order.id} className="bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                <span className="font-semibold text-sm text-gray-700">{order.sellerName || 'N/A'}</span> {/* Use fetched data */}
                <span className={`text-xs font-medium px-2 py-0.5 rounded ${
                  order.status === 'AWAITING_WINNER_PAYMENT' || order.status === 'AWAITING_NEXT_BIDDER_PAYMENT' || order.orderStatus === 'PENDING_PAYMENT' ? 'bg-orange-100 text-orange-700' :
                  order.status === 'PAYMENT_SUCCESSFUL' ? 'bg-green-100 text-green-700' :
                  order.status === 'AWAITING_SHIPMENT' ? 'bg-blue-100 text-blue-700' :
                  order.status?.includes('CANCELLED') ? 'bg-red-100 text-red-700' :
                  'bg-gray-100 text-gray-700'
                }`}>
                  {/* Use the main orderStatusMap for display if it has more detailed statuses */}
                  {orderStatusMap[order.status] || order.status}
                </span>
              </div>

              {/* Ensure order.items is an array and has at least one item */}
              {order.items && order.items.map((item) => (
                <Link to={`/orders/${order.id}`} key={item.productId || item.id} className="block hover:bg-gray-50 transition duration-150 ease-in-out">
                  <div className="flex items-center gap-4 p-4 border-b border-gray-100 last:border-b-0">
                    <img
                      src={item.imageUrl || '/placeholder.png'}
                      alt={item.title}
                      className="w-16 h-16 object-cover rounded border border-gray-200 flex-shrink-0"
                      onError={(e) => { e.target.onerror = null; e.target.src = '/placeholder.png'; }}
                    />
                    <div className="flex-grow">
                      <p className="text-sm font-medium text-gray-800 mb-1">{item.title}</p>
                      {item.variation && <p className="text-xs text-gray-500">{item.variation}</p>}
                      <p className="text-xs text-gray-500">x{item.quantity}</p>
                    </div>
                    <div className="text-sm font-semibold text-gray-800 text-right">
                      {item.price.toLocaleString('vi-VN')} VNĐ
                    </div>
                  </div>
                </Link>
              ))}

              <div className="px-4 py-3 bg-gray-50 flex flex-col sm:flex-row justify-end items-center gap-4">
                {order.status === 'PENDING_PAYMENT' && order.paymentDeadline && (
                  <div className="text-xs text-orange-600 font-medium flex items-center gap-1">
                    <span>Thanh toán trước:</span>
                    <CountdownTimer endTimeMillis={new Date(order.paymentDeadline).getTime()} />
                  </div>
                )}
                <div className='text-right'>
                    <span className="text-sm text-gray-600">Tổng cộng: </span>
                    <span className="text-lg font-semibold text-red-600">
                    {(order.totalPrice || 0).toLocaleString('vi-VN')} VNĐ
                    </span>
                </div>
                 {order.status === 'PENDING_PAYMENT' && (
                    <Link to={`/orders/${order.id}`}>
                      <button className="px-4 py-1.5 bg-red-600 text-white text-sm rounded hover:bg-red-700 transition duration-150 ease-in-out">
                          Thanh toán
                      </button>
                    </Link>
                 )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MyOrdersPage;