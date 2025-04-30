package com.example.liveauctions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor @NoArgsConstructor
public class ChatMessageDto {
    private UUID auctionId;
    private boolean seller;         // ← true if sent by seller
    private String text;
    private LocalDateTime timestamp;
    private String username;
}


