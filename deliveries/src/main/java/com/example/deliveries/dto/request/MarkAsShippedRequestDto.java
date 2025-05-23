package com.example.deliveries.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class MarkAsShippedRequestDto {
    @NotBlank(message = "Courier name is required.")
    private String courierName;

    @NotBlank(message = "Tracking number is required.")
    private String trackingNumber;

    private String notes; // Optional
}