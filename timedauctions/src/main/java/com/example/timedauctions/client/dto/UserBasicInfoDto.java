package com.example.timedauctions.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoDto {
    private String id;
    private String username;
    // Add other basic fields if needed (e.g., avatarUrl)
}