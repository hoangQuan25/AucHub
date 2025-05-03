package com.example.timedauctions.repository;

import com.example.timedauctions.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    // Find visible bids for an auction, ordered by time (uses index)
    Page<Bid> findByTimedAuctionId(UUID timedAuctionId, Pageable pageable);

    /**
     * Finds all unique bidder IDs who have placed a visible bid on a specific timed auction.
     * @param timedAuctionId The ID of the timed auction.
     * @return A Set of unique bidder ID strings.
     */
    @Query("SELECT DISTINCT b.bidderId FROM Bid b WHERE b.timedAuctionId = :timedAuctionId")
    Set<String> findDistinctBidderIdsByTimedAuctionId(@Param("timedAuctionId") UUID timedAuctionId);
}