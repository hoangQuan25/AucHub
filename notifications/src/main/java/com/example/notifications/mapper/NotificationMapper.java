package com.example.notifications.mapper;

import com.example.notifications.client.dto.LiveAuctionSummaryDto;
import com.example.notifications.client.dto.TimedAuctionSummaryDto;
import com.example.notifications.dto.FollowingAuctionSummaryDto;
import com.example.notifications.dto.NotificationDto;
import com.example.notifications.entity.AuctionStatus;
import com.example.notifications.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class NotificationMapper {

    public NotificationDto mapEntityToDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        // Simple mapping using BeanUtils or manual builder
        NotificationDto dto = NotificationDto.builder()
                .type(entity.getType())
                .message(entity.getMessage())
                .timestamp(entity.getCreatedAt())
                .relatedAuctionId(entity.getRelatedAuctionId())
                .isRead(entity.isRead())
                // Include ID if needed by frontend (e.g., for marking as read)
                // .id(entity.getId().toString())
                .build();
        // Alternatively: BeanUtils.copyProperties(entity, dto); // Requires NotificationDto to have matching fields + setters
        return dto;
    }

    public FollowingAuctionSummaryDto mapToCommonSummary(Object specificSummaryDto, String type) {
        FollowingAuctionSummaryDto common = new FollowingAuctionSummaryDto();
        common.setAuctionType(type); // Set the type passed in

        try {
            if (specificSummaryDto instanceof LiveAuctionSummaryDto liveDto) {
                common.setId(liveDto.getId());
                common.setProductTitleSnapshot(liveDto.getProductTitleSnapshot());
                common.setProductImageUrlSnapshot(liveDto.getProductImageUrlSnapshot());
                common.setCurrentBid(liveDto.getCurrentBid());
                common.setEndTime(liveDto.getEndTime());
                // Map status using the helper - assumes LiveAuctionSummaryDto status field name is getStatus()
                common.setStatus(mapStatus(Objects.toString(liveDto.getStatus(), null)));
                common.setCategoryIds(liveDto.getCategoryIds());// Pass string representation
            } else if (specificSummaryDto instanceof TimedAuctionSummaryDto timedDto) {
                common.setId(timedDto.getId());
                common.setProductTitleSnapshot(timedDto.getProductTitleSnapshot());
                common.setProductImageUrlSnapshot(timedDto.getProductImageUrlSnapshot());
                common.setCurrentBid(timedDto.getCurrentBid());
                common.setEndTime(timedDto.getEndTime());
                // Map status using the helper - assumes TimedAuctionSummaryDto status field name is getStatus()
                common.setStatus(mapStatus(Objects.toString(timedDto.getStatus(), null))); // Pass string representation
                common.setCategoryIds(timedDto.getCategoryIds());
            } else if (specificSummaryDto != null) {
                log.warn("Received unexpected DTO type in mapToCommonSummary: {}", specificSummaryDto.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Error mapping specific auction summary DTO to common DTO: {}", e.getMessage(), e);
            // Return partially mapped DTO or null/empty DTO?
        }
        return common;
    }

    private AuctionStatus mapStatus(String statusString) {
        if (statusString == null) return null;
        try {
            // Convert the string name to this service's enum value
            return AuctionStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Could not map auction status string '{}' to local enum.", statusString);
            return null; // Or return a default/unknown status
        }
    }
}
