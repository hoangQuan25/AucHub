package com.example.timedauctions.repository;

import com.example.timedauctions.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    // Find visible bids for an auction, ordered by time (uses index)
    Page<Bid> findByTimedAuctionId(UUID timedAuctionId, Pageable pageable);
}