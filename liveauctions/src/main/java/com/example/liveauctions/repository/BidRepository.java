package com.example.liveauctions.repository;

import com.example.liveauctions.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List; // If needed

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> { // Entity is Bid, ID is Long

    /**
     * Finds bids for a specific auction, supporting pagination and sorting.
     * Used to fetch recent bid history in getAuctionDetails.
     * The Pageable should specify sorting by bidTime DESC.
     *
     * @param liveAuctionId The UUID of the auction.
     * @param pageable Pagination and sorting (e.g., PageRequest.of(0, 20, Sort.by(DESC, "bidTime"))).
     * @return A Page of Bid entities.
     */
    Page<Bid> findByLiveAuctionId(UUID liveAuctionId, Pageable pageable);


    // --- Optional convenience methods ---

    /**
     * Finds the single most recent bid for an auction.
     * Useful if you only need the very last bid details quickly.
     *
     * @param liveAuctionId The UUID of the auction.
     * @return Optional containing the most recent Bid if one exists.
     */
    // Optional<Bid> findTopByLiveAuctionIdOrderByBidTimeDesc(UUID liveAuctionId);


    /**
     * Counts the number of bids for an auction.
     *
     * @param liveAuctionId The UUID of the auction.
     * @return The total count of bids.
     */
    // long countByLiveAuctionId(UUID liveAuctionId);

}