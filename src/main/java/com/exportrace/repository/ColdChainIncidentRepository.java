package com.exportrace.repository;

import com.exportrace.entity.ColdChainIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColdChainIncidentRepository extends JpaRepository<ColdChainIncident, Long> {
    List<ColdChainIncident> findByLoteIdOrderByFechaCreacionDesc(Long loteId);
    List<ColdChainIncident> findByLoteIdAndEstadoIn(Long loteId, List<String> estados);
    Optional<ColdChainIncident> findFirstByLoteIdAndEstadoInOrderByFechaCreacionDesc(Long loteId, List<String> estados);
    long countByLoteIdAndEstadoIn(Long loteId, List<String> estados);
}
