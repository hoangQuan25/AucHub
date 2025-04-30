package com.example.liveauctions.controller;

import com.example.liveauctions.dto.ChatMessageDto;
import com.example.liveauctions.service.AuctionChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuctionChatController {

    private final AuctionChatService chatService;

    /* STOMP frame from browser -> service */
    @MessageMapping("/chat.send.{auctionId}")
    public void handleStomp(@DestinationVariable UUID auctionId,
                            ChatMessageDto msg,
                            SimpMessageHeaderAccessor accessor) {

        String senderId = (String) accessor.getSessionAttributes()
                .get("wsUserId");   // may be null

        log.info("Received STOMP message for auction {}: {}, from {}", auctionId, msg, senderId);
        chatService.processIncoming(auctionId, senderId, msg);
    }

    /* preload history */
    @GetMapping("/{auctionId}/chat")
    public List<ChatMessageDto> history(@PathVariable UUID auctionId,
                                        @RequestParam(defaultValue="100") int limit) {
        return chatService.loadHistory(auctionId, limit);
    }
}
