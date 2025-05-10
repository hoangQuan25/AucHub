 package com.example.orders.mapper;
 import com.example.orders.dto.response.OrderDetailDto;
 import com.example.orders.dto.response.OrderSummaryDto;
 import com.example.orders.entity.Order;
 import com.example.orders.entity.OrderStatus;
 import org.springframework.stereotype.Component;
 import java.util.stream.Collectors; // If mapping lists of items
 import java.util.Collections; // If mapping lists of items

 @Component
 public class OrderMapper {
     public OrderSummaryDto toOrderSummaryDto(Order order) {
         if (order == null) return null;
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
                     .price(order.getInitialWinningBidAmount()) // Or currentAmountDue if that's item price
                     .build()))
             .build();
     }

     public OrderDetailDto toOrderDetailDto(Order order) {
         if (order == null) return null;
         // Calculate buyerPremium conceptually if not stored
         // BigDecimal buyerPremium = order.getCurrentAmountDue().subtract(order.getInitialWinningBidAmount());
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
                     .price(order.getInitialWinningBidAmount()) // Base price of item
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