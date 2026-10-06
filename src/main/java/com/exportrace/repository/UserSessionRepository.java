package com.exportrace.repository;

import com.exportrace.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);
    List<UserSession> findByUserIdAndStatus(Long userId, String status);
    List<UserSession> findByUserId(Long userId);
    List<UserSession> findByStatus(String status);
    List<UserSession> findAllByOrderByLastActivityAtDesc();
    long countByStatus(String status);
}
