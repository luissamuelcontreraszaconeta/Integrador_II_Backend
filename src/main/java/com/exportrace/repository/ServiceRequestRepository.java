package com.exportrace.repository;

import com.exportrace.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    boolean existsByCode(String code);

    boolean existsByCorrelationId(String correlationId);

    Optional<ServiceRequest> findByCode(String code);

    Optional<ServiceRequest> findByCorrelationId(String correlationId);

    List<ServiceRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}
