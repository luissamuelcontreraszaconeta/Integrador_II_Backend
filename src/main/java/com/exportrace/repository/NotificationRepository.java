package com.exportrace.repository;

import com.exportrace.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRolDestinoInOrderByFechaCreacionDesc(List<String> roles);
    List<Notification> findAllByOrderByFechaCreacionDesc();
}
