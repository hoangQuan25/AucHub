// In UsersService (e.g., com.example.users.dto.event.UserBannedEventDto.java)
package com.example.users.dto.event;

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
public class UserBannedEventDto {
    private UUID eventId;
    private LocalDateTime eventTimestamp;
    private String userId;
    private LocalDateTime banEndsAt;
    private int banLevel; // 1 for week, 2 for month, etc.
    private int totalDefaults; // Current default count for context
}