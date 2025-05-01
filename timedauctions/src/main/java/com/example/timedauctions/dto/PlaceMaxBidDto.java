package com.example.timedauctions.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PlaceMaxBidDto {
    @NotNull(message = "Max bid amount cannot be null")
    @Positive(message = "Max bid amount must be positive")
    private BigDecimal maxBid;
}