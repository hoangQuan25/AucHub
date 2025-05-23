package com.example.deliveries.dto.request;
import lombok.Data;

@Data
public class UpdateToDeliveredRequestDto {
    private String notes; // Optional, e.g., "Buyer confirmed via phone"
}