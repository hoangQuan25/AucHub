package com.example.liveauctions.repository; // Your repository package

import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.LiveAuction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // For Pessimistic Lock option
import org.springframework.data.jpa.repository.Query; // If complex queries needed
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // If querying by time
import java.util.List; // If needed
import java.util.Optional;
import java.util.Set;
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


    @Query("""
       SELECT a
         FROM LiveAuction a
        WHERE a.sellerId = :sellerId
          AND (:status   IS NULL OR a.status = :status)
          AND (:from     IS NULL OR a.startTime >= :from OR a.endTime >= :from)
          AND ( :catIdsEmpty = TRUE
                OR EXISTS (
                     SELECT 1
                       FROM a.productCategoryIdsSnapshot c
                      WHERE c IN :catIds)
              )
       """)
    Page<LiveAuction> findSellerAuctionsBySnapshot(@Param("sellerId") String sellerId,
                                                   @Param("status") AuctionStatus status,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("catIds") Set<Long> catIds,
                                                   @Param("catIdsEmpty") boolean catIdsEmpty,
                                                   Pageable pageable);

    boolean existsByIdAndSellerId(UUID id, String sellerId);

    Page<LiveAuction> findAll(Specification<LiveAuction> spec, Pageable pageable);
}