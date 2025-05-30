package com.example.timedauctions.client.dto; // Adjust package

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserBanStatusDto {
    private boolean isBanned;
    private LocalDateTime banEndsAt;
}