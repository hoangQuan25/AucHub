package com.example.liveauctions.service.impl;

import com.example.liveauctions.client.UserServiceClient;
import com.example.liveauctions.client.dto.UserBasicInfoDto;
import com.example.liveauctions.dto.ChatMessageDto;
import com.example.liveauctions.repository.LiveAuctionRepository;
import com.example.liveauctions.service.AuctionChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionChatServiceImpl implements AuctionChatService {

    private final LiveAuctionRepository auctionRepository;        // DAO lives here
    private final RedisTemplate<String, ChatMessageDto> redis;
    private final SimpMessagingTemplate broker;
    private final UserServiceClient userServiceClient;

    private static final int MAX_MSGS = 200;

    @Override
    public void processIncoming(UUID auctionId,
                                String userId,
                                ChatMessageDto payload) {

        // --- seller flag + timestamp + auctionId as before ---
        boolean seller = userId != null &&
                auctionRepository.existsByIdAndSellerId(auctionId, userId);
        payload.setAuctionId(auctionId);
        payload.setSeller(seller);
        payload.setTimestamp(LocalDateTime.now());

        // --- new: fetch username in batch ---
        if (userId != null) {
            // call your open /batch endpoint
            Map<String, UserBasicInfoDto> infos =
                    userServiceClient.getUsersBasicInfoByIds(List.of(userId));
            UserBasicInfoDto info = infos.get(userId);
            payload.setUsername(
                    info != null
                            ? info.getUsername()
                            : "Unknown"
            );
        } else {
            payload.setUsername("Anonymous");
        }

        String key = "chat:" + auctionId;
        redis.opsForList().rightPush(key, payload);
        redis.opsForList().trim(key, -MAX_MSGS, -1);

        broker.convertAndSend("/topic/chat." + auctionId, payload);
    }

    /** history API */
    @Override
    public List<ChatMessageDto> loadHistory(UUID auctionId, int limit) {
        String key = "chat:" + auctionId;
        long size = redis.opsForList().size(key);
        return redis.opsForList().range(key,
                Math.max(0, size - limit), size - 1);
    }
}

