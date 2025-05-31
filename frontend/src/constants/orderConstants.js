// src/constants/orderConstants.js

// Master map for ALL statuses and their display names.
// This should be the single source of truth for how a status key is displayed.
export const orderStatusMap = {
  // --- Buyer & Seller Viewable & Filterable ---
  AWAITING_WINNER_PAYMENT: 'Chờ Thanh Toán (Winner)', // Buyer: Chờ Thanh Toán (Bạn) | Seller: Chờ Khách TT
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Thanh Toán (Next Bidder)', // Buyer: Chờ Thanh Toán (Bạn) | Seller: Chờ Khách (Sau) TT
  PAYMENT_SUCCESSFUL: 'Thanh Toán Thành Công', // Buyer & Seller: Đã Thanh Toán
  AWAITING_FULFILLMENT_CONFIRMATION: 'Chờ Xác Nhận Giao Hàng', // Seller: Chờ Xác Nhận | Buyer: Đã TT, Chờ Shop Xác Nhận
  AWAITING_SHIPMENT: 'Chờ Giao Hàng', // Buyer: Chờ Giao | Seller: Chờ Giao Đi
  COMPLETED: 'Hoàn Thành', // Buyer & Seller

  RETURN_REQUESTED_BY_BUYER: 'Yêu Cầu Trả Hàng', // Buyer: Đã Y/C Trả Hàng | Seller: Khách Y/C Trả Hàng
  RETURN_APPROVED_BY_SELLER: 'Đã Chấp Nhận Trả Hàng', // Buyer: Shop Đã Đồng Ý | Seller: Đã Đồng Ý Trả Hàng
  REFUND_PROCESSING: 'Đang Hoàn Tiền', // A good transient status
  REFUND_COMPLETED: 'Đã Hoàn Tiền',
  REFUND_FAILED: 'Hoàn Tiền Thất Bại',

  // --- Seller Specific States (primarily for seller's view/action) ---
  AWAITING_SELLER_DECISION: 'Cần Quyết Định Bán', // Seller view

  // --- Cancellation States (viewable by both) ---
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Huỷ (Không Thanh Toán)', // Buyer & Seller
  ORDER_CANCELLED_BY_SELLER: 'Huỷ (Người Bán)',          // Buyer & Seller
  ORDER_CANCELLED_SYSTEM: 'Huỷ (Hệ Thống)',           // Buyer & Seller

  // --- States related to Reopening (primarily for seller, buyer might see final state) ---
  // AUCTION_REOPEN_INITIATED: 'Y/C Mở Lại Đấu Giá', // Keep for audit/log if backend still sets it, but maybe not for filtering
  ORDER_SUPERSEDED_BY_REOPEN: 'Đã Mở Lại Đấu Giá', // NEW: For the original order after new auction is up

  // --- Internal/Less Used for Filtering (but good for display completeness) ---
  PAYMENT_WINDOW_EXPIRED_WINNER: 'Winner Quá Hạn Thanh Toán',
  PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER: 'Next Bidder Quá Hạn Thanh Toán',

  // --- Generic terms if needed (can be overridden by more specific ones) ---
  PENDING_PAYMENT: 'Chờ Thanh Toán', // General for buyer if status is AWAITING_..._PAYMENT
  CANCELLED: 'Đã Huỷ', // General if a specific cancellation reason isn't shown
};

// Filter options for the BUYER'S "My Orders" page
export const buyerOrderStatusFilters = {
  ALL: 'Tất Cả',
  PENDING_PAYMENT: 'Chờ Thanh Toán', // This will cover AWAITING_WINNER_PAYMENT, AWAITING_NEXT_BIDDER_PAYMENT
  PAYMENT_SUCCESSFUL: 'Đã Thanh Toán',
  AWAITING_SHIPMENT: 'Chờ Giao Hàng', // This covers AWAITING_FULFILLMENT_CONFIRMATION and AWAITING_SHIPMENT
  COMPLETED: 'Hoàn Thành',
  ORDER_RETURNED: 'Trả Hàng / Hoàn Tiền',
  CANCELLED: 'Đã Huỷ', // This can cover all ORDER_CANCELLED_... types
};

// Filter options for the SELLER'S "My Sales" page
export const sellerOrderStatusFilters = {
  ALL: 'Tất Cả',
  AWAITING_PAYMENT: 'Chờ Khách Thanh Toán', // Covers AWAITING_WINNER_PAYMENT, AWAITING_NEXT_BIDDER_PAYMENT
  AWAITING_SELLER_DECISION: 'Cần Quyết Định',
  PAYMENT_SUCCESSFUL: 'Khách Đã Thanh Toán',
  AWAITING_SHIPMENT: 'Chờ Giao Đi', // Covers AWAITING_FULFILLMENT_CONFIRMATION and AWAITING_SHIPMENT
  COMPLETED: 'Hoàn Thành',
  ORDER_RETURNED: 'Yêu Cầu Trả Hàng',
  ORDER_SUPERSEDED_BY_REOPEN: 'Đã Mở Lại Đấu Giá', // Seller might want to see these
  CANCELLED: 'Đã Huỷ', // Covers all ORDER_CANCELLED_... types
  // AUCTION_REOPEN_INITIATED can be removed as a filter if it's transient and leads to SUPERSEDED
};

// API values for seller decision (remains good)
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