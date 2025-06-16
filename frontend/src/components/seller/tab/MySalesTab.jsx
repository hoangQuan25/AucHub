import React from "react";
import { Link } from "react-router-dom";
// Use the updated constants
import {
  sellerOrderStatusFilters,
  orderStatusMap,
} from "../../../constants/orderConstants";

const MySalesTab = ({
  salesOrders,
  isLoadingSales,
  salesError,
  activeSalesFilter,
  setActiveSalesFilter,
  onOpenDecisionModal,
}) => {
  return (
    <div className="bg-white p-4 sm:p-6 rounded-lg shadow-md">
      {/* Filter Tabs */}
      <div className="mb-6 border-b border-gray-200 px-4 sm:px-6">
        <nav className="flex overflow-x-auto space-x-6">
          {Object.entries(sellerOrderStatusFilters).map(([key, label]) => (
            <button
              key={key}
              onClick={() => setActiveSalesFilter(key)}
              className={`inline-block pb-2 cursor-pointer whitespace-nowrap text-sm font-medium transition-colors duration-150 ease-in-out ${
                activeSalesFilter === key
                  ? "text-blue-600 border-b-2 border-blue-600 font-semibold"
                  : "text-gray-500 hover:text-gray-700 border-b-2 border-transparent"
              }`}
            >
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* Empty, Error & Loading States */}
      {isLoadingSales && (
        <p className="text-center py-10 text-gray-500">Đang tải đơn bán...</p>
      )}
      {salesError && (
        <p className="text-center py-10 text-red-500">
          Đã xảy ra lỗi: {salesError}
        </p>
      )}
      {!isLoadingSales && !salesError && salesOrders.length === 0 && (
        <div className="text-center py-10">
          <p className="text-gray-500">
            Không có đơn bán nào
            {activeSalesFilter !== "ALL"
              ? ` cho trạng thái "${sellerOrderStatusFilters[activeSalesFilter]}"`
              : ""}
            .
          </p>
        </div>
      )}

      {/* Order List */}
      {!isLoadingSales && !salesError && salesOrders.length > 0 && (
        <div className="space-y-4">
          {salesOrders.map((order) => (
            <div
              key={order.id}
              className="bg-white rounded-md shadow-sm border border-gray-200 overflow-hidden"
            >
              {/* Header */}
              <div className="px-4 py-3 border-b border-gray-200 bg-gray-50 flex flex-wrap justify-between items-center gap-2">
                <div>
                  <span className="font-semibold text-sm text-gray-700">
                    Đơn hàng #{order.id.substring(0, 8)}
                  </span>
                  {order.buyerUsernameSnapshot && (
                    <span className="text-xs text-gray-500 ml-2">
                      (Người mua: {order.buyerUsernameSnapshot})
                    </span>
                  )}
                </div>
                <span
                  className={`inline-flex items-center gap-1.5 text-sm font-semibold px-3 py-1 rounded-full shadow-sm border transition-colors duration-200 ${
                    [ "AWAITING_WINNER_PAYMENT", "AWAITING_NEXT_BIDDER_PAYMENT" ].includes(order.status)
                      ? "bg-yellow-50 text-yellow-800 border-yellow-300"
                    : order.status === "AWAITING_SELLER_DECISION"
                      ? "bg-amber-100 text-amber-800 border-amber-300"
                    : [ "PAYMENT_SUCCESSFUL", "AWAITING_FULFILLMENT_CONFIRMATION", "AWAITING_SHIPMENT" ].includes(order.status)
                      ? "bg-sky-100 text-sky-800 border-sky-300"
                    : order.status === 'RETURN_APPROVED_BY_SELLER'
                      ? "bg-orange-100 text-orange-800 border-orange-300"
                    : order.status === 'REFUND_FAILED'
                      ? "bg-red-200 text-red-900 border-red-400 font-bold"
                    : order.status === 'ORDER_RETURNED'
                      ? "bg-slate-200 text-slate-800 border-slate-400"
                    : order.status === "COMPLETED"
                      ? "bg-emerald-100 text-emerald-800 border-emerald-300"
                    : order.status === "ORDER_SUPERSEDED_BY_REOPEN"
                      ? "bg-gray-200 text-gray-800 border-gray-400"
                    : order.status.includes("CANCELLED")
                      ? "bg-rose-100 text-rose-700 border-rose-300"
                      : "bg-gray-100 text-gray-700 border-gray-300"
                  }`}
                >
                  {/* Emojis for quick recognition */}
                  {order.status.includes("CANCELLED") && <span>❌</span>}
                  {order.status === "COMPLETED" && <span>✅</span>}
                  {order.status === "AWAITING_SELLER_DECISION" && <span>⏳</span>}
                  {order.status === 'RETURN_APPROVED_BY_SELLER' && <span className="animate-spin">⚙️</span>}
                  {order.status === 'ORDER_RETURNED' && <span>↩️</span>}
                  {order.status === 'REFUND_FAILED' && <span>❗️</span>}
                  {[ "AWAITING_SHIPMENT", "AWAITING_FULFILLMENT_CONFIRMATION" ].includes(order.status) && <span>📦</span>}
                  
                  {orderStatusMap[order.status] || order.status.replace(/_/g, " ")}
                </span>
              </div>

              {/* Items */}
              {order.items.map((item) => (
                <Link
                  to={`/orders/${order.id}`}
                  key={item.productId || order.id}
                  className="block hover:bg-gray-50 transition duration-150 ease-in-out cursor-pointer"
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
                      <p className="text-xs text-gray-500">
                        Số lượng: {item.quantity}
                      </p>
                    </div>
                    <div className="text-sm font-semibold text-gray-800 text-right">
                      Giá bán:{" "}
                      {(order.totalPrice || item.price || 0).toLocaleString(
                        "vi-VN"
                      )}{" "}
                      VNĐ
                    </div>
                  </div>
                </Link>
              ))}

              {/* Seller Decision */}
              {order.status === "AWAITING_SELLER_DECISION" && (
                <div className="px-4 pt-3 pb-2 bg-yellow-50 border-t border-yellow-200">
                  <p className="text-xs text-yellow-700 font-semibold mb-1">
                    Thông tin các lựa chọn (nếu có):
                  </p>
                  {order.eligibleSecondBidderId &&
                  order.eligibleSecondBidAmount != null ? (
                    <p className="text-xs text-yellow-600">
                      - Ưu đãi cho người mua hạng 2:{" "}
                      {order.eligibleSecondBidAmount.toLocaleString("vi-VN")}{" "}
                      VNĐ
                    </p>
                  ) : (
                    <p className="text-xs text-yellow-500">
                      - Không có người mua hạng 2 đủ điều kiện.
                    </p>
                  )}
                  {order.eligibleThirdBidderId &&
                  order.eligibleThirdBidAmount != null ? (
                    <p className="text-xs text-yellow-600 mt-0.5">
                      - Ưu đãi cho người mua hạng 3:{" "}
                      {order.eligibleThirdBidAmount.toLocaleString("vi-VN")} VNĐ
                    </p>
                  ) : (
                    <p className="text-xs text-yellow-500 mt-0.5">
                      - Không có người mua hạng 3 đủ điều kiện.
                    </p>
                  )}
                </div>
              )}

              {/* Footer */}
              <div className="px-4 py-3 bg-gray-50 flex flex-col sm:flex-row justify-between items-center gap-3 border-t">
                <div className="text-sm text-gray-600">
                  Ngày tạo:{" "}
                  {new Date(order.createdAt || Date.now()).toLocaleDateString(
                    "vi-VN"
                  )}
                </div>
                <div className="text-right">
                  <span className="text-sm text-gray-600">Tổng tiền: </span>
                  <span className="text-lg font-semibold text-green-600">
                    {(order.totalPrice || 0).toLocaleString("vi-VN")} VNĐ
                  </span>
                </div>

                {/* Action Buttons */}
                {order.status === "AWAITING_SELLER_DECISION" && (
                  <button
                    onClick={() => onOpenDecisionModal(order)}
                    className="cursor-pointer px-4 py-1.5 bg-yellow-500 text-white text-sm font-semibold rounded-md hover:bg-yellow-600 transition duration-150 ease-in-out shadow-sm"
                  >
                    Xử Lý Quyết Định
                  </button>
                )}
                
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MySalesTab;
