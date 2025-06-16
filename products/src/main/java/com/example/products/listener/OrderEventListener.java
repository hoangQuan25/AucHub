 package com.example.products.listener;

 import com.example.products.config.RabbitMqConfig;
 import com.example.products.dto.event.OrderCompletedEventDto;
 import com.example.products.service.ProductService;
 import jakarta.transaction.Transactional;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.amqp.rabbit.annotation.RabbitListener;
 import org.springframework.stereotype.Service;

 @Service
 @RequiredArgsConstructor
 @Slf4j
 public class OrderEventListener {

     private final ProductService productService;

     @RabbitListener(queues = RabbitMqConfig.PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE)
     public void handleOrderCompleted(OrderCompletedEventDto event) {
         log.info("ProductService received OrderCompletedEvent for order ID: {}, product ID: {}", event.getOrderId(), event.getProductId());
         if (event.getProductId() != null) {
             try {
                 productService.markProductAsSold(event.getProductId());
             } catch (Exception e) {
                 log.error("Error marking product {} as sold: {}", event.getProductId(), e.getMessage(), e);
             }
         } else {
             log.warn("OrderCompletedEvent received without productId, orderId: {}", event.getOrderId());
         }
     }
 }