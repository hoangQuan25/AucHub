package com.example.notifications.mapper;

import com.example.notifications.dto.NotificationDto;
import com.example.notifications.entity.Notification;
import org.springframework.stereotype.Component;

@Component
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
}
