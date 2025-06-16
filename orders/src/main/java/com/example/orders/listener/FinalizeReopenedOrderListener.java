// src/main/java/com/example/orders/listener/FinalizeReopenedOrderListener.java
package com.example.orders.listener;

import com.example.orders.config.RabbitMqConfig;
import com.example.orders.dto.event.NewLiveAuctionFromReopenedOrderEventDto;
import com.example.orders.dto.event.NewTimedAuctionFromReopenedOrderEventDto;
import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import com.example.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Component // Mark as a Spring managed component
@RequiredArgsConstructor
@Slf4j
public class FinalizeReopenedOrderListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMqConfig.ORDERS_FINALIZE_REOPENED_TIMED_AUCTION_QUEUE)
    @Transactional
    public void handleNewTimedAuctionFromReopenedOrder(NewTimedAuctionFromReopenedOrderEventDto event) {
        log.info("Listener: Received NewTimedAuctionFromReopenedOrderEvent: originalOrderId={}, newTimedAuctionId={}, productId={}",
                event.getOriginalOrderId(), event.getNewTimedAuctionId(), event.getProductId());

        try {
            Order originalOrder = orderRepository.findById(event.getOriginalOrderId())
                    .orElseThrow(() -> {
                        log.error("Listener: Original order not found for ID: {} from NewTimedAuctionFromReopenedOrderEvent.", event.getOriginalOrderId());
                        return new NoSuchElementException("Original order not found: " + event.getOriginalOrderId());
                    });

            // Validations
            if (originalOrder.getOrderStatus() == OrderStatus.ORDER_SUPERSEDED_BY_REOPEN) {
                log.info("Listener: Order {} already superseded. Acknowledging event idempotently.", originalOrder.getId());
                return; // Idempotent: already processed
            }

            if (originalOrder.getOrderStatus() != OrderStatus.AWAITING_SELLER_DECISION) {
                log.warn("Listener: Original order {} is not in AWAITING_SELLER_DECISION state. Current status: {}. Cannot supersede automatically for new timed auction {}.",
                        originalOrder.getId(), originalOrder.getOrderStatus(), event.getNewTimedAuctionId());
            }
            if (!originalOrder.getProductId().equals(event.getProductId()) || !originalOrder.getSellerId().equals(event.getSellerId())) {
                log.error("Listener: Mismatch in product/seller details for original order {} and NewTimedAuctionFromReopenedOrderEvent. Event: {}",
                        originalOrder.getId(), event);
                throw new IllegalArgumentException("Product/Seller mismatch for reopen finalization of order " + originalOrder.getId());
            }

            originalOrder.setOrderStatus(OrderStatus.ORDER_SUPERSEDED_BY_REOPEN); // Use the new status
            String note = String.format("Order superseded. Auction reopened as new Timed Auction ID: %s.", event.getNewTimedAuctionId());
            originalOrder.setInternalNotes(originalOrder.getInternalNotes() == null ? note : originalOrder.getInternalNotes() + "; " + note);

            orderRepository.save(originalOrder);
            log.info("Listener: Original order {} status updated to ORDER_SUPERSEDED_BY_REOPEN, superseded by new timed auction {}.",
                    originalOrder.getId(), event.getNewTimedAuctionId());

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.error("Listener: Error processing NewTimedAuctionFromReopenedOrderEvent for originalOrderId {}: {}", event.getOriginalOrderId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Listener: Unexpected error processing NewTimedAuctionFromReopenedOrderEvent for originalOrderId {}: {}", event.getOriginalOrderId(), e.getMessage(), e);
            throw e; // Rethrow for DLQ
        }
    }

    @RabbitListener(queues = RabbitMqConfig.ORDERS_FINALIZE_REOPENED_LIVE_AUCTION_QUEUE)
    @Transactional
    public void handleNewLiveAuctionFromReopenedOrder(NewLiveAuctionFromReopenedOrderEventDto event) {
        log.info("Listener: Received NewLiveAuctionFromReopenedOrderEvent: originalOrderId={}, newLiveAuctionId={}, productId={}",
                event.getOriginalOrderId(), event.getNewLiveAuctionId(), event.getProductId());

        try {
            Order originalOrder = orderRepository.findById(event.getOriginalOrderId())
                    .orElseThrow(() -> {
                        log.error("Listener: Original order not found for ID: {} from NewLiveAuctionFromReopenedOrderEvent.", event.getOriginalOrderId());
                        return new NoSuchElementException("Original order not found: " + event.getOriginalOrderId());
                    });


            if (originalOrder.getOrderStatus() == OrderStatus.ORDER_SUPERSEDED_BY_REOPEN) {
                log.info("Listener: Order {} already superseded. Acknowledging event idempotently.", originalOrder.getId());
                return; // Idempotent
            }

            if (originalOrder.getOrderStatus() != OrderStatus.AWAITING_SELLER_DECISION) {
                log.warn("Listener: Original order {} is not in AWAITING_SELLER_DECISION state. Current status: {}. Cannot supersede automatically for new live auction {}.",
                        originalOrder.getId(), originalOrder.getOrderStatus(), event.getNewLiveAuctionId());
            }
            if (!originalOrder.getProductId().equals(event.getProductId()) || !originalOrder.getSellerId().equals(event.getSellerId())) {
                log.error("Listener: Mismatch in product/seller details for original order {} and NewLiveAuctionFromReopenedOrderEvent. Event: {}",
                        originalOrder.getId(), event);
                throw new IllegalArgumentException("Product/Seller mismatch for reopen finalization of order " + originalOrder.getId());
            }

            originalOrder.setOrderStatus(OrderStatus.ORDER_SUPERSEDED_BY_REOPEN); // Use the new status
            String note = String.format("Order superseded. Auction reopened as new Live Auction ID: %s.", event.getNewLiveAuctionId());
            originalOrder.setInternalNotes(originalOrder.getInternalNotes() == null ? note : originalOrder.getInternalNotes() + "; " + note);

            orderRepository.save(originalOrder);
            log.info("Listener: Original order {} status updated to ORDER_SUPERSEDED_BY_REOPEN, superseded by new live auction {}.",
                    originalOrder.getId(), event.getNewLiveAuctionId());

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.error("Listener: Error processing NewLiveAuctionFromReopenedOrderEvent for originalOrderId {}: {}", event.getOriginalOrderId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Listener: Unexpected error processing NewLiveAuctionFromReopenedOrderEvent for originalOrderId {}: {}", event.getOriginalOrderId(), e.getMessage(), e);
            throw e;
        }
    }
}