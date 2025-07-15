package com.example.deliveries.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class MarkAsShippedRequestDto {
    @NotBlank(message = "Courier name is required.")
    private String courierName;

    @NotBlank(message = "Tracking number is required.")
    private String trackingNumber;

    @NotBlank(message = "Estimated delivery date is required.")
    private String estimatedDeliveryDate;

    private String notes; // Optional
}