// src/pages/MySalesPage.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { useKeycloak } from '@react-keycloak/web';
import { sellerOrderStatusFilters, orderStatusMap } from '../constants/orderConstants';
import SellerDecisionModal from '../components/SellerDecisionModal';

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
      const params = { page: 0, size: 20 };
      if (activeFilter !== 'ALL') {
        params.status = activeFilter;
      }
      const response = await apiClient.get(`/orders/my-sales`, { params });
      setOrders(response.data.content || []);
    } catch (err) {
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
    fetchSalesOrders(); 
  };

  if (!initialized) {
    return <div className="text-center p-10">Initializing...</div>;
  }

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 bg-gray-100 min-h-screen">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-6">Đơn Bán Của Tôi</h1>

      <div className="mb-6 bg-white shadow-sm rounded-md overflow-hidden">
        <nav className="flex flex-wrap border-b border-gray-200">
          {Object.entries(sellerOrderStatusFilters).map(([key, value]) => (
            <button
              key={key}
              onClick={() => setActiveFilter(key)}
              className={`py-3 px-4 sm:px-6 text-sm font-medium focus:outline-none transition-colors duration-150 ease-in-out ${
                activeFilter === key
                  ? 'text-blue-600 border-b-2 border-blue-600'
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
            <p className="text-gray-500">Không có đơn bán nào {activeFilter !== 'ALL' ? `cho trạng thái "${sellerOrderStatusFilters[activeFilter]}"` : ''}.</p>
         </div>
      )}

      {!isLoading && !error && orders.length > 0 && (
        <div className="space-y-4">
          {orders.map((order) => (
            <div key={order.id} className="bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden">
              <div className="px-4 py-3 border-b border-gray-200 bg-gray-50 flex flex-wrap justify-between items-center gap-2">
                <div>
                  <span className="font-semibold text-sm text-gray-700">
                    Đơn hàng #{order.id.substring(0, 8)}
                  </span>
                  {/* Buyer username display removed as per your request */}
                </div>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
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
                    <img src={item.imageUrl || '/placeholder.png'} alt={item.title} className="w-16 h-16 object-cover rounded border border-gray-200 flex-shrink-0" onError={(e) => { e.target.onerror = null; e.target.src = '/placeholder.png'; }}/>
                    <div className="flex-grow">
                      <p className="text-sm font-medium text-gray-800 mb-1">{item.title}</p>
                      <p className="text-xs text-gray-500">Số lượng: {item.quantity}</p>
                    </div>
                    <div className="text-sm font-semibold text-gray-800 text-right">
                      Giá bán: {(order.totalPrice || item.price || 0).toLocaleString('vi-VN')} VNĐ
                    </div>
                  </div>
                </Link>
              ))}
              
              {order.status === 'AWAITING_SELLER_DECISION' && (
                <div className="px-4 pt-3 pb-2 bg-yellow-50 border-t border-yellow-200">
                  <p className="text-xs text-yellow-700 font-semibold mb-1">Thông tin các lựa chọn (nếu có):</p>
                  {order.eligibleSecondBidderId && order.eligibleSecondBidAmount != null ? (
                    <p className="text-xs text-yellow-600">
                      - Ưu đãi cho người mua hạng 2: {order.eligibleSecondBidAmount.toLocaleString('vi-VN')} VNĐ
                    </p>
                  ) : (
                    <p className="text-xs text-yellow-500">- Không có người mua hạng 2 đủ điều kiện.</p>
                  )}
                  {order.eligibleThirdBidderId && order.eligibleThirdBidAmount != null ? ( // Display 3rd if available
                    <p className="text-xs text-yellow-600 mt-0.5">
                      - Ưu đãi cho người mua hạng 3: {order.eligibleThirdBidAmount.toLocaleString('vi-VN')} VNĐ
                    </p>
                  ) : (
                     <p className="text-xs text-yellow-500 mt-0.5">- Không có người mua hạng 3 đủ điều kiện.</p>
                  )}
                </div>
              )}

              <div className="px-4 py-3 bg-gray-50 flex flex-col sm:flex-row justify-between items-center gap-3 border-t">
                <div className="text-sm text-gray-600">
                  Ngày tạo: {new Date(order.createdAt || Date.now()).toLocaleDateString('vi-VN')}
                </div>
                <div className='text-right'>
                    <span className="text-sm text-gray-600">Tổng tiền: </span>
                    <span className="text-lg font-semibold text-green-600">
                      {(order.totalPrice || 0).toLocaleString('vi-VN')} VNĐ
                    </span>
                </div>
                 {order.status === 'AWAITING_SELLER_DECISION' && (
                    <button
                      onClick={() => handleOpenDecisionModal(order)}
                      className="px-4 py-1.5 bg-yellow-500 text-white text-sm font-semibold rounded-md hover:bg-yellow-600 transition duration-150 ease-in-out shadow-sm"
                    >
                        Xử Lý Quyết Định
                    </button>
                 )}
                 {order.status === 'AWAITING_SHIPMENT' && (
                     <button
                       className="px-4 py-1.5 bg-blue-500 text-white text-sm font-semibold rounded-md hover:bg-blue-600 transition duration-150 ease-in-out shadow-sm"
                     >
                         Đánh Dấu Đã Giao {/* Implement onClick later */}
                     </button>
                 )}
              </div>
            </div>
          ))}
        </div>
      )}

      {selectedOrderForDecision && isDecisionModalOpen && (
        <SellerDecisionModal
          order={selectedOrderForDecision} // This order object now contains eligibility fields
          isOpen={isDecisionModalOpen}
          onClose={handleCloseDecisionModal}
        />
      )}
    </div>
  );
}

export default MySalesPage;