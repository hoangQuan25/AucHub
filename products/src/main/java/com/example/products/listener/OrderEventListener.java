package com.example.products.listener;

import com.example.products.config.RabbitMqConfig;
import com.example.products.dto.event.OrderCancelledEventDto; // You will need to create/share this DTO
import com.example.products.dto.event.OrderCompletedEventDto;
import com.example.products.entity.ProductStatus; // Import status
import com.example.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductService productService;

    // This method is now modified
    @RabbitListener(queues = RabbitMqConfig.PRODUCT_SERVICE_ORDER_COMPLETED_QUEUE)
    public void handleOrderCompleted(OrderCompletedEventDto event) {
        log.info("ProductService received OrderCompletedEvent for product ID: {}", event.getProductId());
        if (event.getProductId() != null) {
            try {
                // The old `markProductAsSold` logic is now a status update
                productService.updateProductStatus(event.getProductId(), ProductStatus.SOLD);
            } catch (Exception e) {
                log.error("Error setting product {} status to SOLD: {}", event.getProductId(), e.getMessage(), e);
            }
        } else {
            log.warn("OrderCompletedEvent received without productId, orderId: {}", event.getOrderId());
        }
    }

    @RabbitListener(queues = RabbitMqConfig.PRODUCT_SERVICE_ORDER_CANCELLED_QUEUE)
    public void handleOrderCancelled(OrderCancelledEventDto event) {
        log.info("ProductService received OrderCancelledEvent for product ID: {}", event.getProductId());
        if (event.getProductId() != null) {
            try {
                // The product is now free again
                productService.updateProductStatus(event.getProductId(), ProductStatus.AVAILABLE);
            } catch (Exception e) {
                log.error("Error setting product {} status to AVAILABLE after order cancellation: {}", event.getProductId(), e.getMessage(), e);
            }
        }
    }
}

