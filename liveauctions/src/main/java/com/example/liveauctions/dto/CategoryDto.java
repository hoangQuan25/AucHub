package com.example.liveauctions.dto; // Or a shared DTO package

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryDto {
    Long id; // Or String if UUIDs are used in Products service
    String name;
}