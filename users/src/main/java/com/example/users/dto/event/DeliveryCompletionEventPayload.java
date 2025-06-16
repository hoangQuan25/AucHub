// package com.example.users.dto.event; // Or a shared events package
package com.example.users.dto.event; // Or a shared events package

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor // Needed for Jackson deserialization
public class DeliveryCompletionEventPayload {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private UUID deliveryId; // This might be the same as orderId or a separate delivery entity ID
    private String orderId; // Crucial for linking
    private String buyerId;
    private String sellerId;
}