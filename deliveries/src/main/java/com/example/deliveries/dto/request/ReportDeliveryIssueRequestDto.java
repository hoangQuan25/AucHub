package com.example.deliveries.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ReportDeliveryIssueRequestDto {
    @NotBlank(message = "Issue notes/reason is required.")
    private String notes;
}