package com.example.liveauctions.client.dto;

import lombok.Getter;

@Getter
public enum ProductCondition {
    NEW_WITH_TAGS("New with Tags"), // Mới nguyên tag
    LIKE_NEW("Like New (No Tags)"), // Như mới (Không tag)
    VERY_GOOD("Very Good"),         // Rất tốt
    GOOD("Good"),                   // Tốt
    FAIR("Fair"),                   // Khá (Có lỗi nhỏ)
    POOR("Poor");                   // Cũ (Nhiều lỗi)

    private final String displayName;

    ProductCondition(String displayName) {
        this.displayName = displayName;
    }

    // Optional: Method to find enum by display name if needed
}