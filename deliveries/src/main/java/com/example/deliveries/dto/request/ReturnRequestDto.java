package com.example.deliveries.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ReturnRequestDto {
    @NotBlank String reason;
    String comments;
}