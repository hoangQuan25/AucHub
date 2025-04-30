package com.example.liveauctions.service;

import com.example.liveauctions.dto.ChatMessageDto;

import java.util.List;
import java.util.UUID;

public interface AuctionChatService {

    void processIncoming(UUID auctionId,
                         String userId,
                         ChatMessageDto payload);

    List<ChatMessageDto> loadHistory(UUID auctionId, int limit);
}
