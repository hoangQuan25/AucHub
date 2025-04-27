package com.example.liveauctions.repository; // Your repository package

import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.LiveAuction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // For Pessimistic Lock option
import org.springframework.data.jpa.repository.Query; // If complex queries needed
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // If querying by time
import java.util.List; // If needed
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType; // JPA Lock Mode


@Repository
public interface LiveAuctionRepository extends JpaRepository<LiveAuction, UUID> { // Entity is LiveAuction, ID is UUID

    /**
     * Finds auctions by their status, supporting pagination and sorting.
     * Used for fetching active auctions list.
     *
     * @param status The status to filter by (e.g., ACTIVE).
     * @param pageable Pagination and sorting information.
     * @return A Page of LiveAuction entities.
     */
    Page<LiveAuction> findByStatus(AuctionStatus status, Pageable pageable);


    /**
     * Finds an auction by ID and applies a pessimistic write lock.
     * Useful for the placeBid operation if using DB-level locking instead of Redis.
     * NOTE: Only use this if NOT using Redis distributed locks for placeBid.
     *
     * @param id The UUID of the auction.
     * @return Optional containing the locked LiveAuction entity if found.
     */
    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("SELECT a FROM LiveAuction a WHERE a.id = :id")
    // Optional<LiveAuction> findByIdWithPessimisticLock(UUID id);


    // --- Potential future methods ---
    // Find auctions ending within a certain time window
    // List<LiveAuction> findByStatusAndEndTimeBetween(AuctionStatus status, LocalDateTime start, LocalDateTime end);

    // Find auctions by seller
    // Page<LiveAuction> findBySellerId(String sellerId, Pageable pageable);

}