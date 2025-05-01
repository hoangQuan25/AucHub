package com.example.timedauctions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCommentDto {
    @NotBlank(message = "Comment text cannot be blank")
    @Size(max = 5000, message = "Comment too long") // Example max length
    private String commentText;

    private Long parentId; // Optional: for replies
}