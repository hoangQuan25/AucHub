// package com.example.users.listener;
package com.example.users.listener;

import com.example.users.config.RabbitMqConfig; // Use the User service's RabbitMQ config
import com.example.users.dto.event.DeliveryCompletionEventPayload; // The DTO User service expects
import com.example.users.entity.ReviewEligibility;
import com.example.users.repository.ReviewEligibilityRepository;
import com.example.users.repository.UserRepository; // To check if buyer/seller exist
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCompletionListener {

    private final ReviewEligibilityRepository reviewEligibilityRepository;
    private final UserRepository userRepository; // To validate user existence

    @Transactional
    @RabbitListener(queues = {
            RabbitMqConfig.DELIVERY_RECEIPT_CONFIRMED_FOR_REVIEW_QUEUE,
            RabbitMqConfig.DELIVERY_AUTO_COMPLETED_FOR_REVIEW_QUEUE
    })
    public void handleDeliveryCompletedEvent(@Payload DeliveryCompletionEventPayload event) {
        log.info("Received DeliveryCompletionEvent for orderId: {}, buyerId: {}, sellerId: {}",
                event.getOrderId(), event.getBuyerId(), event.getSellerId());

        if (event.getOrderId() == null || event.getBuyerId() == null || event.getSellerId() == null) {
            log.error("DeliveryCompletionEvent for order {} is missing crucial information. Cannot create review eligibility.", event.getOrderId());
            return;
        }

        // Basic validation: Check if buyer and seller exist
        if (!userRepository.existsById(event.getBuyerId()) || !userRepository.existsById(event.getSellerId())) {
            log.error("Buyer or Seller ID from event does not exist. Buyer: {}, Seller: {}. Order: {}",
                    event.getBuyerId(), event.getSellerId(), event.getOrderId());
            return; // Or handle more gracefully
        }

        // Idempotency: Check if eligibility already exists for this order
        if (reviewEligibilityRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.warn("Review eligibility for orderId {} already exists. Skipping creation.", event.getOrderId());
            return;
        }

        ReviewEligibility eligibility = ReviewEligibility.builder()
                .orderId(event.getOrderId())
                .buyerId(event.getBuyerId())
                .sellerId(event.getSellerId())
                .eligibleFromTimestamp(event.getEventTimestamp() != null ? event.getEventTimestamp() : LocalDateTime.now())
                .reviewSubmitted(false)
                .build();

        try {
            reviewEligibilityRepository.save(eligibility);
            log.info("Review eligibility created for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to save review eligibility for orderId {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}