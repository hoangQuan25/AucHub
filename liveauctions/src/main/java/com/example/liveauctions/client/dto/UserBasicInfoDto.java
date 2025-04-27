package com.example.liveauctions.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserBasicInfoDto {
    private String id;
    private String username;
    // Add other basic fields if needed (e.g., avatarUrl)
}