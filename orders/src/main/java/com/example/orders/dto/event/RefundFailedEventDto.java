// Create this file in: com.example.orders.dto.event.RefundFailedEventDto.java
package com.example.orders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundFailedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID orderId;
    private String buyerId;
    private String paymentIntentId;
    private String failureReason;
    private String failureCode;
}