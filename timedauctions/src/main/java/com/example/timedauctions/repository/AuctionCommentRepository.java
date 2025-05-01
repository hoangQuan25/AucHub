package com.example.timedauctions.repository;

import com.example.timedauctions.entity.AuctionComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionCommentRepository extends JpaRepository<AuctionComment, Long> {
    // Find top-level comments for an auction, ordered by time
    Page<AuctionComment> findByTimedAuctionIdAndParentIdIsNullOrderByCreatedAtAsc(UUID timedAuctionId, Pageable pageable);

    List<AuctionComment> findByTimedAuctionIdOrderByCreatedAtAsc(UUID timedAuctionId);

    // Find replies to a specific comment, ordered by time
    Page<AuctionComment> findByParentIdOrderByCreatedAtAsc(Long parentId, Pageable pageable);
}