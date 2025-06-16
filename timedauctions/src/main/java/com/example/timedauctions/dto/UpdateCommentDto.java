package com.example.timedauctions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentDto {

    @NotBlank(message = "Comment text cannot be blank.")
    private String commentText;
}