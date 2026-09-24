package com.exportrace.repository;

import com.exportrace.entity.QualityInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {
    Optional<QualityInspection> findByLoteId(Long loteId);
}
