package com.example.orders.repository; // Adjust package as needed

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // For complex queries
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    // Example custom query methods you might need later:

    // Find orders by seller ID and status
    Page<Order> findBySellerIdAndOrderStatus(String sellerId, OrderStatus orderStatus, Pageable pageable);

    Page<Order> findBySellerId(String sellerId, Pageable pageable);

    // Find orders by current bidder ID and status
    Page<Order> findByCurrentBidderIdAndOrderStatus(String currentBidderId, OrderStatus orderStatus, Pageable pageable);
    Page<Order> findByCurrentBidderIdAndOrderStatusIn(String currentBidderId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findBySellerIdAndOrderStatusIn(String sellerId, List<OrderStatus> statuses, Pageable pageable);

    Optional<Order> findByAuctionId(UUID auctionId);

    Page<Order> findByCurrentBidderId(String currentBidderId, Pageable pageable);

}