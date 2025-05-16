// src/constants/orderConstants.js

export const orderStatusMap = {
  ALL: 'Tất Cả',
  AWAITING_WINNER_PAYMENT: 'Chờ Thanh Toán (Winner)',
  PAYMENT_WINDOW_EXPIRED_WINNER: 'Winner Quá Hạn TT',
  AWAITING_SELLER_DECISION: 'Chờ Quyết Định Bán',
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Thanh Toán (Next Bidder)',
  PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER: 'Next Bidder Quá Hạn TT',
  PAYMENT_SUCCESSFUL: 'Thanh Toán Thành Công',
  AWAITING_SHIPMENT: 'Chờ Giao Hàng',
  // ORDER_SHIPPED: 'Đã Giao Hàng', -> From Delivery Service
  // ORDER_DELIVERED: 'Đã Nhận Hàng', -> From Delivery Service
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Huỷ (Không Thanh Toán)',
  ORDER_CANCELLED_BY_SELLER: 'Huỷ (Người Bán)',
  ORDER_CANCELLED_SYSTEM: 'Huỷ (Hệ Thống)',
  AUCTION_REOPEN_INITIATED: 'Y/C Mở Lại Đấu Giá',
  // Add other statuses your system uses and wants to display
  // PENDING_PAYMENT is a common alias used in frontend for multiple awaitng payment states
  PENDING_PAYMENT: 'Chờ Thanh Toán',
  COMPLETED: 'Hoàn Thành',
  CANCELLED: 'Đã Huỷ',
  DELIVERING: 'Đang Giao',
};


// For SellerDecisionModal
export const SELLER_DECISION_TYPES = {
  OFFER_TO_NEXT_BIDDER: 'Offer to Next Eligible Bidder',
  REOPEN_AUCTION: 'Re-open Auction',
  CANCEL_SALE: 'Cancel Sale (No Winner This Round)',
};

// This maps the display selection to the actual enum/string value your backend expects
// Make sure these values (OFFER_TO_NEXT_BIDDER, etc.) match your backend SellerDecisionType enum values.
export const SELLER_DECISION_API_VALUES = {
  OFFER_TO_NEXT_BIDDER: 'OFFER_TO_NEXT_BIDDER',
  REOPEN_AUCTION: 'REOPEN_AUCTION',
  CANCEL_SALE: 'CANCEL_SALE',
};