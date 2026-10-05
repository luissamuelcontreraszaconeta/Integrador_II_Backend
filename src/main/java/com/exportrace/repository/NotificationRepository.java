package com.exportrace.repository;

import com.exportrace.entity.Notification;
import com.exportrace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE (n.user = :user OR n.targetRole = :role OR n.targetRole = 'ALL' OR (n.user IS NULL AND :role IS NOT NULL AND n.targetRole = :role)) ORDER BY n.createdAt DESC")
    List<Notification> findAuthorizedNotifications(@Param("user") User user, @Param("role") String role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE (n.user = :user OR n.targetRole = :role OR n.targetRole = 'ALL' OR (n.user IS NULL AND :role IS NOT NULL AND n.targetRole = :role)) AND n.isRead = false")
    long countUnreadAuthorized(@Param("user") User user, @Param("role") String role);

    List<Notification> findTop20ByUserOrTargetRoleOrTargetRoleOrderByCreatedAtDesc(User user, String targetRole, String allRole);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE (n.user = :user OR n.targetRole = :role OR n.targetRole = 'ALL') AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") User user, @Param("role") String role);
}
