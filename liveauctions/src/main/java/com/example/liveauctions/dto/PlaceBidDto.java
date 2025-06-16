package com.example.liveauctions.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PlaceBidDto {

    @NotNull(message = "Bid amount cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Bid amount must be positive")
    private BigDecimal amount;

}