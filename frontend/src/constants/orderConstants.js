// src/constants/orderConstants.js

// This remains your master map for display purposes (e.g., on OrderDetailPage)
export const orderStatusMap = {
  ALL: 'Tất Cả',
  AWAITING_WINNER_PAYMENT: 'Chờ Thanh Toán (Winner)',
  PAYMENT_WINDOW_EXPIRED_WINNER: 'Winner Quá Hạn TT',
  AWAITING_SELLER_DECISION: 'Chờ Quyết Định Bán',
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Thanh Toán (Next Bidder)',
  PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER: 'Next Bidder Quá Hạn TT',
  PAYMENT_SUCCESSFUL: 'Thanh Toán Thành Công',
  AWAITING_SHIPMENT: 'Chờ Giao Hàng',
  // ORDER_SHIPPED: 'Đã Giao Hàng', // To be handled by Delivery Service/Tab
  // ORDER_DELIVERED: 'Đã Nhận Hàng', // To be handled by Delivery Service/Tab
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Huỷ (Không Thanh Toán)',
  ORDER_CANCELLED_BY_SELLER: 'Huỷ (Người Bán)',
  ORDER_CANCELLED_SYSTEM: 'Huỷ (Hệ Thống)',
  AUCTION_REOPEN_INITIATED: 'Y/C Mở Lại Đấu Giá',
  PENDING_PAYMENT: 'Chờ Thanh Toán', // General term, useful for display if needed
  COMPLETED: 'Hoàn Thành', // General term for orders that went through delivery
  CANCELLED: 'Đã Huỷ', // General term
  // DELIVERING: 'Đang Giao', // This should be part of a separate delivery status map
};

export const buyerOrderStatusFilters = {
  ALL:     'Tất Cả',
  AWAITING_WINNER_PAYMENT:      'Chờ Thanh Toán (Bạn)',
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Thanh Toán (Bạn)',
  PAYMENT_SUCCESSFUL:           'Đã Thanh Toán',
  AWAITING_SHIPMENT:            'Chờ Giao Hàng',
  ORDER_CANCELLED_BY_SELLER:    'Huỷ (Người Bán)',
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Huỷ (Không Thanh Toán)',
  ORDER_CANCELLED_SYSTEM:       'Huỷ (Hệ Thống)',
};

// --- NEW: Statuses for "My Sales" Page (Seller's Perspective) ---
export const sellerOrderStatusFilters = {
  ALL: 'Tất Cả',
  AWAITING_WINNER_PAYMENT: 'Chờ Khách Thanh Toán',
  // PAYMENT_WINDOW_EXPIRED_WINNER: 'Winner Quá Hạn TT', // Often leads to AWAITING_SELLER_DECISION
  AWAITING_SELLER_DECISION: 'Cần Quyết Định',
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Khách (Sau) TT',
  PAYMENT_SUCCESSFUL: 'Đã Thanh Toán', // Or 'Khách Đã Thanh Toán'
  AWAITING_SHIPMENT: 'Chờ Giao Đi',
  // Seller might see 'Đã Giao Hàng' (after they mark as shipped) in a delivery tab/flow
  ORDER_CANCELLED_BY_SELLER: 'Bạn Đã Huỷ',
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Huỷ (Khách Không TT)',
  ORDER_CANCELLED_SYSTEM: 'Huỷ (Hệ Thống)',
  AUCTION_REOPEN_INITIATED: 'Y/C Mở Lại Đấu Giá',
};


// For SellerDecisionModal (this remains the same)
export const SELLER_DECISION_TYPES = {
  OFFER_TO_NEXT_BIDDER: 'Offer to Next Eligible Bidder',
  REOPEN_AUCTION: 'Re-open Auction',
  CANCEL_SALE: 'Cancel Sale (No Winner This Round)',
};

export const SELLER_DECISION_API_VALUES = {
  OFFER_TO_NEXT_BIDDER: 'OFFER_TO_NEXT_BIDDER',
  REOPEN_AUCTION: 'REOPEN_AUCTION',
  CANCEL_SALE: 'CANCEL_SALE',
};