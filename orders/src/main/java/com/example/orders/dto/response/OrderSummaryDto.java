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
 public class OrderSummaryDto {
     private UUID id; // Order ID
     private String sellerName; // sellerUsernameSnapshot
     private OrderStatus status;
     private LocalDateTime paymentDeadline; // Nullable, only if pending payment
     private BigDecimal totalPrice; // currentAmountDue
     private String currency; // "VND"
     private List<OrderItemSummaryDto> items;

     private String eligibleSecondBidderId;
     private BigDecimal eligibleSecondBidAmount;
     private String eligibleThirdBidderId;
     private BigDecimal eligibleThirdBidAmount;

     @Data
     @Builder
     public static class OrderItemSummaryDto { // Nested for simplicity
         private Long productId;
         private String title;
         private String imageUrl;
         private int quantity; // Usually 1 for auction items
         private BigDecimal price; // The price this item contributed to the order
     }
 }