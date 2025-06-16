package com.example.deliveries.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryIssueReportedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;

    private UUID deliveryId;
    private UUID orderId;
    private String sellerId; // Seller of the order
    private String buyerId;
    private String reporterId;
    private String issueNotes;
    private String newStatus; // e.g. "ISSUE_REPORTED"
    private String productInfoSnapshot; // Brief description of items
}