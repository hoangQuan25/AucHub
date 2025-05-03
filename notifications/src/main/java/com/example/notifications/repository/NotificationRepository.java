package com.example.notifications.repository;

import com.example.notifications.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds notifications for a specific user, ordered by creation date descending.
     * Useful for displaying notification history.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Finds *unread* notifications for a specific user, ordered by creation date descending.
     * Useful for notification indicators or dropdowns.
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * Counts unread notifications for a specific user.
     */
    long countByUserIdAndIsReadFalse(String userId);

    /**
     * Marks specific notifications as read for a given user.
     * Uses @Modifying and @Query for bulk update.
     * @return the number of notifications updated.
     */
    @Modifying // Indicates this query modifies data
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.id IN :notificationIds")
    int markAsRead(@Param("userId") String userId, @Param("notificationIds") Collection<Long> notificationIds);

    /**
     * Marks ALL notifications as read for a given user.
     * @return the number of notifications updated.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") String userId);

    // Optional: Method for deleting old, read notifications
    // @Modifying
    // @Query("DELETE Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    // int deleteReadOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

}