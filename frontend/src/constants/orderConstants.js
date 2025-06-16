export const orderStatusMap = {
  AWAITING_WINNER_PAYMENT: 'Awaiting Payment (Winner)', 
  AWAITING_NEXT_BIDDER_PAYMENT: 'Awaiting Payment (Next Bidder)', 
  PAYMENT_SUCCESSFUL: 'Payment Successful', 
  AWAITING_FULFILLMENT_CONFIRMATION: 'Awaiting Fulfillment Confirmation', 
  AWAITING_SHIPMENT: 'Awaiting Shipment',
  COMPLETED: 'Completed', 

  RETURN_REQUESTED_BY_BUYER: 'Return Requested', 
  RETURN_APPROVED_BY_SELLER: 'Return Approved',
  REFUND_PROCESSING: 'Refund Processing',
  REFUND_COMPLETED: 'Refund Completed',
  REFUND_FAILED: 'Refund Failed',

  AWAITING_SELLER_DECISION: 'Awaiting Seller Decision', 

  ORDER_CANCELLED_NO_PAYMENT_FINAL: 'Cancelled (No Payment)',
  ORDER_CANCELLED_BY_SELLER: 'Cancelled (Seller)',          
  ORDER_CANCELLED_SYSTEM: 'Cancelled (System)',           

  ORDER_SUPERSEDED_BY_REOPEN: 'Auction Reopened', 

  PAYMENT_WINDOW_EXPIRED_WINNER: 'Winner Payment Window Expired',
  PAYMENT_WINDOW_EXPIRED_NEXT_BIDDER: 'Next Bidder Payment Window Expired',

  PENDING_PAYMENT: 'Pending Payment',
  CANCELLED: 'Cancelled', 
};

export const buyerOrderStatusFilters = {
  ALL: 'All',
  PENDING_PAYMENT: 'Pending Payment',
  PAYMENT_SUCCESSFUL: 'Paid',
  AWAITING_SHIPMENT: 'Awaiting Shipment', 
  COMPLETED: 'Completed',
  ORDER_RETURNED: 'Return / Refund',
  CANCELLED: 'Cancelled', 
};

export const sellerOrderStatusFilters = {
  ALL: 'All',
  AWAITING_PAYMENT: 'Awaiting Buyer Payment', 
  AWAITING_SELLER_DECISION: 'Awaiting Decision',
  PAYMENT_SUCCESSFUL: 'Buyer Paid',
  AWAITING_SHIPMENT: 'Awaiting Dispatch',
  COMPLETED: 'Completed',
  ORDER_RETURNED: 'Return Request',
  ORDER_SUPERSEDED_BY_REOPEN: 'Auction Reopened', 
  CANCELLED: 'Cancelled', 
};

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