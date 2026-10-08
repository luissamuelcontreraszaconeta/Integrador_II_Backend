package com.exportrace.repository;

import com.exportrace.entity.QualityInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QualityInspectionRepository extends JpaRepository<QualityInspection, Long> {
    List<QualityInspection> findByLoteIdOrderByNumeroInspeccionAsc(Long loteId);
    List<QualityInspection> findByLoteIdOrderByNumeroInspeccionDesc(Long loteId);
    Optional<QualityInspection> findFirstByLoteIdOrderByNumeroInspeccionDesc(Long loteId);
    Optional<QualityInspection> findByLoteIdAndNumeroInspeccion(Long loteId, Integer numeroInspeccion);
    long countByLoteId(Long loteId);
    boolean existsByLoteIdAndNumeroInspeccion(Long loteId, Integer numeroInspeccion);

    // Default backward compatible method returning the latest inspection
    default Optional<QualityInspection> findByLoteId(Long loteId) {
        return findFirstByLoteIdOrderByNumeroInspeccionDesc(loteId);
    }
}
