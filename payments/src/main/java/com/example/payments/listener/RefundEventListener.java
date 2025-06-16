package com.example.payments.listener;

import com.example.payments.config.RabbitMqConfig;
import com.example.payments.dto.request.RefundRequestedEventDto;
import com.example.payments.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = RabbitMqConfig.REFUND_REQUESTED_QUEUE)
    public void handleRefundRequested(@Payload RefundRequestedEventDto event) {
        log.info("Received RefundRequestedEvent for orderId: {}, buyerId: {}, paymentIntentRef: {}, amount: {} {}",
                event.getOrderId(), event.getBuyerId(), event.getPaymentTransactionRef(), event.getAmountToRefund(), event.getCurrency());
        try {
            paymentService.processRefundRequest(event);
        } catch (StripeException e) {
            log.error("StripeException while processing refund request for order {}: {}", event.getOrderId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing refund request for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
}