export const orderStatusMap = {
  ALL: 'Tất cả', // For filtering only
  PENDING_PAYMENT: 'Chờ thanh toán',
  PENDING_CONFIRMATION: 'Chờ xác nhận', // Added status for seller confirmation after payment
  DELIVERING: 'Vận chuyển', // Maybe combine with Chờ giao hàng? Or keep separate
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  // Add other backend statuses your OrderStatus enum might have
  AWAITING_SELLER_DECISION: 'Chờ Quyết Định Bán',
  PAYMENT_WINDOW_EXPIRED_WINNER: 'Hết Hạn Thanh Toán',
  PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER: 'Hết Hạn Thanh Toán',
  AWAITING_NEXT_BIDDER_PAYMENT: 'Chờ Người Kế Tiếp TT',
  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Đã Hủy (Không TT)',
  ORDER_CANCELLED_BY_SELLER: 'Đã Hủy (Người Bán)',
  ORDER_CANCELLED_SYSTEM: 'Đã Hủy (Hệ Thống)',
  AUCTION_REOPEN_INITIATED: 'Yêu Cầu Mở Lại',
  AWAITING_SHIPMENT: 'Chờ Giao Hàng', // Often used after payment confirmation
};