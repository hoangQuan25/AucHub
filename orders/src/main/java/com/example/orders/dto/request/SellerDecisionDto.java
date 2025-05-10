package com.example.orders.dto.request; // Suggested package for request DTOs

import com.example.orders.entity.SellerDecisionType; // Assuming this enum is in entity package
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDecisionDto {
    @NotNull(message = "Decision type cannot be null")
    private SellerDecisionType decisionType; // OFFER_TO_NEXT_BIDDER, REOPEN_AUCTION, CANCEL_SALE
}