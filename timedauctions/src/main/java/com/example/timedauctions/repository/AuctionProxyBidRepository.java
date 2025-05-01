package com.example.timedauctions.repository;

import com.example.timedauctions.entity.AuctionProxyBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuctionProxyBidRepository extends JpaRepository<AuctionProxyBid, Long> {
    // Find all proxy bids for an auction, ordered for processing (uses index)
    List<AuctionProxyBid> findByTimedAuctionIdOrderByMaxBidDescSubmissionTimeAsc(UUID timedAuctionId);

    // Find a specific bidder's proxy bid for an auction (uses index)
    Optional<AuctionProxyBid> findByTimedAuctionIdAndBidderId(UUID timedAuctionId, String bidderId);
}