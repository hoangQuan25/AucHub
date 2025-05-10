 package com.example.orders.dto.response;
 import com.example.orders.entity.OrderStatus;
 import lombok.Builder;
 import lombok.Data;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;
 import java.util.List;
 import java.util.UUID;

 @Data
 @Builder
 public class OrderDetailDto {
     private UUID id;
     private UUID auctionId;
     private String sellerId;
     private String sellerUsernameSnapshot;
     private OrderStatus status;
     private LocalDateTime paymentDeadline; // If applicable
     private BigDecimal currentAmountDue;
     private String currency; // "VND"
     private String initialWinnerId;
     private BigDecimal initialWinningBidAmount;
     // private BigDecimal buyerPremium; // If you calculate and store this
     private String currentBidderId;
     private int paymentOfferAttempt;
     private String auctionType; // From Order entity
     private List<OrderItemDetailDto> items; // Similar to OrderItemSummaryDto, maybe more detail
     private LocalDateTime createdAt;
     private LocalDateTime updatedAt;

     // Info for potential next bidders (can be useful for FE display if seller is viewing)
     private String eligibleSecondBidderId;
     private BigDecimal eligibleSecondBidAmount;
     private String eligibleThirdBidderId;
     private BigDecimal eligibleThirdBidAmount;

     @Data
     @Builder
     public static class OrderItemDetailDto {
         private Long productId;
         private String title;
         private String imageUrl;
         private String variation; // If applicable
         private int quantity;
         private BigDecimal price;
         // Potentially more product details if needed
     }
 }

