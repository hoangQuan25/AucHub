package com.example.timedauctions.repository;

import com.example.timedauctions.dto.TimedAuctionSummaryDto;
import com.example.timedauctions.entity.TimedAuction;
import com.example.timedauctions.entity.AuctionStatus; // Assuming same package or import
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Repository
public interface TimedAuctionRepository extends JpaRepository<TimedAuction, UUID> {
    // Add methods for finding by status, sellerId etc. later if needed
    Page<TimedAuction> findByStatus(AuctionStatus status, Pageable pageable);

    Page<TimedAuction> findAll(Specification<TimedAuction> spec, Pageable pageable);
}