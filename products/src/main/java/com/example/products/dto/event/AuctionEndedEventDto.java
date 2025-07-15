package com.example.products.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionEndedEventDto {
    private UUID eventId;
    private Long productId;
    private String finalStatus; // "SOLD", "RESERVE_NOT_MET", "CANCELLED"
}