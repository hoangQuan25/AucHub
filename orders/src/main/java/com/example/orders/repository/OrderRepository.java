package com.example.orders.repository; // Adjust package as needed

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // For complex queries
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Page<Order> findBySellerIdAndOrderStatus(String sellerId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findBySellerId(String sellerId, Pageable pageable);

    // Find orders by current bidder ID and status
    Page<Order> findByCurrentBidderIdAndOrderStatus(String currentBidderId, OrderStatus orderStatus, Pageable pageable);
    Page<Order> findByCurrentBidderIdAndOrderStatusIn(String currentBidderId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findBySellerIdAndOrderStatusIn(String sellerId, List<OrderStatus> statuses, Pageable pageable);

    Optional<Order> findByAuctionId(UUID auctionId);

    List<Order> findBySellerId(String sellerId);
    List<Order> findByCurrentBidderId(String bidderId);

    Page<Order> findByCurrentBidderId(String currentBidderId, Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.sellerUsernameSnapshot = :newUsername WHERE o.sellerId = :sellerId")
    int updateSellerUsernameSnapshot(@Param("sellerId") String sellerId, @Param("newUsername") String newUsername);

}