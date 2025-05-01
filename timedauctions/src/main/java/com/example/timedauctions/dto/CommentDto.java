package com.example.timedauctions.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; // For replies

@Data
@Builder
public class CommentDto {
    private Long id;
    private String userId;
    private String usernameSnapshot;
    private String commentText;
    private LocalDateTime createdAt;
    private Long parentId;
    private List<CommentDto> replies; // For nested structure
    private int replyCount; // Alternative: just show count
}