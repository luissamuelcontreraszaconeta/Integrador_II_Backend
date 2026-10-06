package com.exportrace.repository;

import com.exportrace.entity.SessionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionPolicyRepository extends JpaRepository<SessionPolicy, Long> {
    Optional<SessionPolicy> findByRole(String role);
}
