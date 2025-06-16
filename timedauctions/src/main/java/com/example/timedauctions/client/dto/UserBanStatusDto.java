package com.example.timedauctions.client.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserBanStatusDto {
    private boolean isBanned;
    private LocalDateTime banEndsAt;
}