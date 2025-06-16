package com.example.deliveries.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class ReturnRequestDto {

    @NotBlank(message = "A reason for the return is required.")
    private String reason;

    private String comments;


    @NotBlank(message = "Please provide the courier service you are using for the return.")
    private String returnCourier;

    @NotBlank(message = "Please provide the tracking number for the return shipment.")
    private String returnTrackingNumber;

    private List<String> imageUrls;
}