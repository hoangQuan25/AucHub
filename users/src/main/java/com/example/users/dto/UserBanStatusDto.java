package com.example.users.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBanStatusDto {
    private boolean isBanned;
    private LocalDateTime banEndsAt;
}