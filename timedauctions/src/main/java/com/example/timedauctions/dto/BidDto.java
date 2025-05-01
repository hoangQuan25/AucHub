package com.example.timedauctions.dto;

// Reusable Bid DTO (represents visible bids)
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BidDto {
    private String bidderId;
    private String bidderUsernameSnapshot;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private boolean isAutoBid; // Include the flag
}