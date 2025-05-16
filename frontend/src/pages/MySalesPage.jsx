// src/pages/MySalesPage.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient'; // Your configured Axios instance
import { useKeycloak } from '@react-keycloak/web';
import { orderStatusMap } from '../constants/orderConstants'; // Assuming this map is still relevant
import SellerDecisionModal from '../components/SellerDecisionModal'; // We'll define this

function MySalesPage() {
  const [activeFilter, setActiveFilter] = useState('ALL');
  const [orders, setOrders] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const { keycloak, initialized } = useKeycloak();

  const [selectedOrderForDecision, setSelectedOrderForDecision] = useState(null);
  const [isDecisionModalOpen, setIsDecisionModalOpen] = useState(false);

  const fetchSalesOrders = async () => {
    if (!initialized || !keycloak.authenticated) {
      if (initialized && !keycloak.authenticated) {
        setError("Please log in to view your sales.");
        setOrders([]);
      }
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      // IMPORTANT: This endpoint /orders/my-sales is hypothetical.
      // Your backend needs to provide an endpoint to fetch orders for the logged-in seller.
      // It might be /orders/my?role=seller or a dedicated one.
      const statusParam = activeFilter === 'ALL' ? '' : `&status=${activeFilter}`;
      const response = await apiClient.get(`/orders/my-sales?page=0&size=10${statusParam}`);
      setOrders(response.data.content || []);
    } catch (err) {
      console.error("Failed to fetch sales orders:", err);
      setError(err.response?.data?.message || "Could not load your sales orders.");
      setOrders([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSalesOrders();
  }, [activeFilter, initialized, keycloak.authenticated]);

  const handleOpenDecisionModal = (order) => {
    setSelectedOrderForDecision(order);
    setIsDecisionModalOpen(true);
  };

  const handleCloseDecisionModal = () => {
    setSelectedOrderForDecision(null);
    setIsDecisionModalOpen(false);
    fetchSalesOrders(); // Refetch orders after a decision is made
  };

  if (!initialized) {
    return <div className="text-center p-10">Initializing...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 bg-gray-100 min-h-screen">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">Đơn Bán Của Tôi</h1>

      <div className="mb-6 bg-white shadow-sm rounded-md overflow-hidden">
        <nav className="flex flex-wrap border-b border-gray-200">
          {Object.entries(orderStatusMap).map(([key, value]) => (
            <button
              key={key}
              onClick={() => setActiveFilter(key)}
              className={`py-3 px-4 sm:px-6 text-sm font-medium focus:outline-none transition-colors duration-150 ease-in-out ${
                activeFilter === key
                  ? 'text-blue-600 border-b-2 border-blue-600' // Seller theme color
                  : 'text-gray-600 hover:text-gray-800'
              }`}
            >
              {value}
            </button>
          ))}
        </nav>
      </div>

      {isLoading && <div className="text-center py-10">Đang tải đơn bán...</div>}
      {!isLoading && error && <div className="text-center py-10 text-red-600 bg-white rounded-md shadow-sm">{error}</div>}
      {!isLoading && !error && orders.length === 0 && (
         <div className="text-center py-10 bg-white rounded-md shadow-sm">
            <p className="text-gray-500">Không có đơn bán nào {activeFilter !== 'ALL' ? `cho trạng thái "${orderStatusMap[activeFilter]}"` : ''}.</p>
         </div>
      )}
      {!isLoading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <div key={order.id} className="bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                <div>
                  <span className="font-semibold text-sm text-gray-700">
                    Đơn hàng #{order.id.substring(0, 8)}
                  </span>
                  <span className="text-xs text-gray-500 ml-2">
                    (Người mua: {order.currentBidderUsernameSnapshot || order.initialWinnerUsernameSnapshot || 'N/A'})
                  </span>
                </div>
                <span className={`text-xs font-medium px-2 py-0.5 rounded ${
                  order.status === 'AWAITING_WINNER_PAYMENT' || order.status === 'AWAITING_NEXT_BIDDER_PAYMENT' ? 'bg-orange-100 text-orange-700' :
                  order.status === 'AWAITING_SELLER_DECISION' ? 'bg-yellow-100 text-yellow-700' :
                  order.status === 'PAYMENT_SUCCESSFUL' ? 'bg-green-100 text-green-700' :
                  order.status === 'AWAITING_SHIPMENT' ? 'bg-blue-100 text-blue-700' :
                  order.status.includes('CANCELLED') ? 'bg-red-100 text-red-700' :
                  'bg-gray-100 text-gray-700'
                }`}>
                  {orderStatusMap[order.status] || order.status}
                </span>
              </div>

              {order.items && order.items.map((item) => (
                <Link to={`/orders/${order.id}`} key={item.productId || order.id} className="block hover:bg-gray-50 transition duration-150 ease-in-out">
                  <div className="flex items-center gap-4 p-4 border-b border-gray-100 last:border-b-0">
                    <img
                      src={item.imageUrl || '/placeholder.png'}
                      alt={item.title}
                      className="w-16 h-16 object-cover rounded border border-gray-200 flex-shrink-0"
                      onError={(e) => { e.target.onerror = null; e.target.src = '/placeholder.png'; }}
                    />
                    <div className="flex-grow">
                      <p className="text-sm font-medium text-gray-800 mb-1">{item.title}</p>
                      <p className="text-xs text-gray-500">Số lượng: {item.quantity}</p>
                    </div>
                    <div className="text-sm font-semibold text-gray-800 text-right">
                      Giá bán: {(order.winningBid || order.initialWinningBidAmount || item.price).toLocaleString('vi-VN')} VNĐ
                    </div>
                  </div>
                </Link>
              ))}

              <div className="px-4 py-3 bg-gray-50 flex flex-col sm:flex-row justify-between items-center gap-3">
                <div className="text-sm text-gray-600">
                  Ngày tạo: {new Date(order.createdAt).toLocaleDateString('vi-VN')}
                </div>
                <div className='text-right'>
                    <span className="text-sm text-gray-600">Tổng tiền (dự kiến): </span>
                    <span className="text-lg font-semibold text-green-600">
                      {(order.currentAmountDue || order.winningBid || order.initialWinningBidAmount || 0).toLocaleString('vi-VN')} VNĐ
                    </span>
                </div>
                 {order.status === 'AWAITING_SELLER_DECISION' && (
                    <button
                      onClick={() => handleOpenDecisionModal(order)}
                      className="px-4 py-1.5 bg-yellow-500 text-white text-sm rounded hover:bg-yellow-600 transition duration-150 ease-in-out"
                    >
                        Cần Quyết Định
                    </button>
                 )}
                 {/* Add other seller-specific actions here, e.g., Mark as Shipped */}
                 {order.status === 'AWAITING_SHIPMENT' && (
                     <button
                       // onClick={() => handleMarkAsShipped(order.id)} // Implement this
                       className="px-4 py-1.5 bg-blue-500 text-white text-sm rounded hover:bg-blue-600 transition duration-150 ease-in-out"
                     >
                         Đánh Dấu Đã Giao
                     </button>
                 )}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedOrderForDecision && isDecisionModalOpen && (
        <SellerDecisionModal
          order={selectedOrderForDecision}
          isOpen={isDecisionModalOpen}
          onClose={handleCloseDecisionModal}
          // onDecisionMade={handleCloseDecisionModal} // The modal itself will call onClose, which refetches
        />
      )}
    </div>
  );
}

export default MySalesPage;