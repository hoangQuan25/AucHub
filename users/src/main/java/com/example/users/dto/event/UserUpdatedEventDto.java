package com.example.users.dto.event;

import com.example.users.dto.UserBasicInfoDto;
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
public class UserUpdatedEventDto {
    @Builder.Default
    private UUID eventId = UUID.randomUUID();
    @Builder.Default
    private LocalDateTime eventTimestamp = LocalDateTime.now();

    private UserBasicInfoDto updatedUser;
}