 package com.example.orders.mapper;
 import com.example.orders.dto.response.OrderDetailDto;
 import com.example.orders.dto.response.OrderSummaryDto;
 import com.example.orders.entity.Order;
 import com.example.orders.entity.OrderStatus;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Component;

 import java.math.BigDecimal;
 import java.util.stream.Collectors; // If mapping lists of items
 import java.util.Collections; // If mapping lists of items

 @Component
 @Slf4j
 public class OrderMapper {

     private BigDecimal determineCurrentItemPrice(Order order) {
         if (order.getCurrentBidderId() != null) {
             if (order.getCurrentBidderId().equals(order.getInitialWinnerId()) && order.getInitialWinningBidAmount() != null) {
                 return order.getInitialWinningBidAmount();
             } else if (order.getCurrentBidderId().equals(order.getEligibleSecondBidderId()) && order.getEligibleSecondBidAmount() != null) {
                 return order.getEligibleSecondBidAmount();
             } else if (order.getCurrentBidderId().equals(order.getEligibleThirdBidderId()) && order.getEligibleThirdBidAmount() != null) {
                 return order.getEligibleThirdBidAmount();
             }
             // If currentBidderId is set, but their specific amount is null, this is an issue.
             // Fallback to initial winning bid if available, otherwise log a warning.
             log.warn("Could not determine specific bid amount for current bidder {} in order {}. Defaulting or using initial.",
                     order.getCurrentBidderId(), order.getId());
             if (order.getInitialWinningBidAmount() != null) {
                 return order.getInitialWinningBidAmount(); // Fallback
             }
         } else if (order.getInitialWinningBidAmount() != null) {
             // No current bidder (e.g. before any payment attempt), or some other state. Use initial.
             return order.getInitialWinningBidAmount();
         }

         // If all else fails (e.g., no amounts set at all)
         log.error("Critical: Unable to determine item price for order {}. All relevant bid amounts might be null.", order.getId());
         return BigDecimal.ZERO; // Or handle as a more significant error
     }

     public OrderSummaryDto toOrderSummaryDto(Order order) {
         if (order == null) return null;
         BigDecimal currentItemPrice = determineCurrentItemPrice(order);
         return OrderSummaryDto.builder()
             .id(order.getId())
             .sellerName(order.getSellerUsernameSnapshot())
             .status(order.getOrderStatus())
             .paymentDeadline( (order.getOrderStatus() == OrderStatus.AWAITING_WINNER_PAYMENT || order.getOrderStatus() == OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT) ? order.getPaymentDeadline() : null )
             .totalPrice(order.getCurrentAmountDue())
             .currency(order.getCurrency())
             .items(Collections.singletonList(OrderSummaryDto.OrderItemSummaryDto.builder() // Assuming one item for now
                     .productId(order.getProductId())
                     .title(order.getProductTitleSnapshot())
                     .imageUrl(order.getProductImageUrlSnapshot())
                     .quantity(1) // Assuming 1 for auction
                     .price(currentItemPrice) // Or currentAmountDue if that's item price
                     .build()))
                 .eligibleSecondBidderId(order.getEligibleSecondBidderId())
                 .eligibleSecondBidAmount(order.getEligibleSecondBidAmount())
                 .eligibleThirdBidderId(order.getEligibleThirdBidderId())
                 .eligibleThirdBidAmount(order.getEligibleThirdBidAmount())
             .build();
     }

     public OrderDetailDto toOrderDetailDto(Order order) {
         if (order == null) return null;
         BigDecimal currentItemPrice = determineCurrentItemPrice(order);
         return OrderDetailDto.builder()
             .id(order.getId())
             .auctionId(order.getAuctionId())
             .sellerId(order.getSellerId())
             .sellerUsernameSnapshot(order.getSellerUsernameSnapshot())
             .status(order.getOrderStatus())
             .paymentDeadline( (order.getOrderStatus() == OrderStatus.AWAITING_WINNER_PAYMENT || order.getOrderStatus() == OrderStatus.AWAITING_NEXT_BIDDER_PAYMENT) ? order.getPaymentDeadline() : null )
             .currentAmountDue(order.getCurrentAmountDue())
             .currency(order.getCurrency())
             .initialWinnerId(order.getInitialWinnerId())
             .initialWinningBidAmount(order.getInitialWinningBidAmount())
             // .buyerPremium(buyerPremium.compareTo(BigDecimal.ZERO) > 0 ? buyerPremium : null)
             .currentBidderId(order.getCurrentBidderId())
             .paymentOfferAttempt(order.getPaymentOfferAttempt())
             .auctionType(order.getAuctionType())
             .items(Collections.singletonList(OrderDetailDto.OrderItemDetailDto.builder() // Assuming one item
                     .productId(order.getProductId())
                     .title(order.getProductTitleSnapshot())
                     .imageUrl(order.getProductImageUrlSnapshot())
                     .quantity(1)
                     .price(currentItemPrice) // Base price of item
                     // .variation(...) // If you store this on Order
                     .build()))
             .createdAt(order.getCreatedAt())
             .updatedAt(order.getUpdatedAt())
             .eligibleSecondBidderId(order.getEligibleSecondBidderId())
             .eligibleSecondBidAmount(order.getEligibleSecondBidAmount())
             .eligibleThirdBidderId(order.getEligibleThirdBidderId())
             .eligibleThirdBidAmount(order.getEligibleThirdBidAmount())
             .build();
     }
 }