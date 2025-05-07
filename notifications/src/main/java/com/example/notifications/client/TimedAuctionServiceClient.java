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

@FeignClient(name = "timedauctions") // Match Eureka name
public interface TimedAuctionServiceClient {

    @GetMapping("/batch-summary") // Path on timed auction service
    List<TimedAuctionSummaryDto> getAuctionSummariesByIds(@RequestParam("ids") Set<UUID> auctionIds);
}