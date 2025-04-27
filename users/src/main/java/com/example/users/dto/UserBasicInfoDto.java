package com.example.users.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
 public class UserBasicInfoDto {
    private String id;
    private String username;
    // Add other basic fields if needed (e.g., avatarUrl)
 }
