package com.example.liveauctions.client.dto;

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
    private String avatarUrl;
    // Add other basic fields if needed (e.g., avatarUrl)
}