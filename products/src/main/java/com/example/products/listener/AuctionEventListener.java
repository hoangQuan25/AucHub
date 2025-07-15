package com.example.products.listener;

import com.example.products.config.RabbitMqConfig;
import com.example.products.dto.event.AuctionEndedEventDto; // You will need to create/share this DTO
import com.example.products.dto.event.ProductLockedInAuctionEventDto;
import com.example.products.entity.ProductStatus;
import com.example.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

    private final ProductService productService;

    @RabbitListener(queues = RabbitMqConfig.PRODUCT_SERVICE_AUCTION_ENDED_QUEUE)
    public void handleAuctionEnded(AuctionEndedEventDto event) {
        final Long productId = event.getProductId();
        final String finalStatus = event.getFinalStatus(); // e.g., "SOLD", "RESERVE_NOT_MET", "CANCELLED"

        log.info("ProductService received AuctionEndedEvent for product ID: {}. Final auction status: {}", productId, finalStatus);
        if (productId == null) return;

        try {
            switch (finalStatus) {
                case "SOLD":
                    // Auction was successful, order process begins. Lock the product.
                    productService.updateProductStatus(productId, ProductStatus.AWAITING_COMPLETION);
                    break;

                case "RESERVE_NOT_MET":
                case "CANCELLED":
                    // Auction failed or was cancelled. Make the product available again.
                    productService.updateProductStatus(productId, ProductStatus.AVAILABLE);
                    break;

                default:
                    log.warn("Unhandled final auction status '{}' for product {}", finalStatus, productId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error updating product {} status from AuctionEndedEvent: {}", productId, e.getMessage(), e);
        }
    }


    @RabbitListener(queues = RabbitMqConfig.PRODUCT_SERVICE_PRODUCT_LOCKED_QUEUE)
    public void handleProductLocked(ProductLockedInAuctionEventDto event) {
        final Long productId = event.getProductId();
        log.info("ProductService received ProductLockedInAuctionEvent for product ID: {}", productId);
        if (productId == null) return;

        try {
            productService.updateProductStatus(productId, ProductStatus.IN_AUCTION);
        } catch (Exception e) {
            log.error("Error setting product {} status to IN_AUCTION: {}", productId, e.getMessage(), e);
        }
    }
}