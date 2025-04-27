package com.example.liveauctions.dto;

import jakarta.validation.constraints.*;
import lombok.Data; // Or @Value for immutability, @Getter/@Setter etc.
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequiredArgsConstructor
public class CreateLiveAuctionDto {

    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    @NotNull(message = "Duration must be specified")
    @Min(value = 1, message = "Duration must be at least 1 minute") // Min 1 minute duration
    private Integer durationMinutes;

    @NotNull(message = "Start price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Start price must be positive") // Or 0.0 inclusive=true if 0 is allowed
    private BigDecimal startPrice;

    @DecimalMin(value = "0.0", inclusive = true, message = "Reserve price must be non-negative") // Can be 0 or positive
    private BigDecimal reservePrice; // Optional

    // Optional: If null, auction starts immediately. If present, must be in the future.
    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime startTime; // Represents the 'datetime-local' input from frontend
}