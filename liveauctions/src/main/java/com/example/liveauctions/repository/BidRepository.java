package com.example.liveauctions.repository;

import com.example.liveauctions.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
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


    /**
     * Finds all unique bidder IDs who have placed a bid on a specific live auction.
     * @param liveAuctionId The ID of the live auction.
     * @return A Set of unique bidder ID strings.
     */
    @Query("SELECT DISTINCT b.bidderId FROM Bid b WHERE b.liveAuctionId = :liveAuctionId")
    Set<String> findDistinctBidderIdsByLiveAuctionId(@Param("liveAuctionId") UUID liveAuctionId);

}