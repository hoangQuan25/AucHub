package com.example.notifications.client;

// Import the DTOs defined IN THIS SERVICE (or shared lib) that match the structure
// returned by the auction services' endpoints.
import com.example.notifications.client.dto.LiveAuctionSummaryDto; // Assumes structure matches
import com.example.notifications.client.dto.TimedAuctionSummaryDto; // Assumes structure matches
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Feign client for Live Auction Service
@FeignClient(name = "liveauctions") // Match Eureka name
public interface LiveAuctionServiceClient {

    @GetMapping("/batch-summary") // Path on live auction service
    List<LiveAuctionSummaryDto> getAuctionSummariesByIds(@RequestParam("ids") Set<UUID> auctionIds);
}