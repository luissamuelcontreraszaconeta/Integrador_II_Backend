package com.exportrace.repository;

import com.exportrace.entity.SanitaryCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SanitaryCertificationRepository extends JpaRepository<SanitaryCertification, Long> {
    Optional<SanitaryCertification> findByLoteId(Long loteId);
}
