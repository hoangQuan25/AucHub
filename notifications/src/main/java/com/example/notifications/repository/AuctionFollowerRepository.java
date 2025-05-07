package com.example.notifications.repository;

import com.example.notifications.entity.AuctionFollower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AuctionFollowerRepository extends JpaRepository<AuctionFollower, Long> {

    // Find all AuctionFollower entities for a specific user
    // This is needed by NotificationServiceImpl to get both auctionId and auctionType
    List<AuctionFollower> findByUserId(String userId);

    // Find all auction IDs followed by a user
    // Use projection to only select the auctionId for efficiency
    @Query("SELECT af.auctionId FROM AuctionFollower af WHERE af.userId = :userId")
    Set<UUID> findAuctionIdsByUserId(@Param("userId") String userId);

    // Check if a specific follow relationship exists
    boolean existsByUserIdAndAuctionId(String userId, UUID auctionId);

    // Find a specific follow relationship (for deletion)
    Optional<AuctionFollower> findByUserIdAndAuctionId(String userId, UUID auctionId);

    // Find all followers (user IDs) for a specific auction
    @Query("SELECT af.userId FROM AuctionFollower af WHERE af.auctionId = :auctionId")
    List<String> findUserIdsByAuctionId(@Param("auctionId") UUID auctionId);

    // Delete by user and auction (for unfollow) - returns number deleted
    long deleteByUserIdAndAuctionId(String userId, UUID auctionId);
}