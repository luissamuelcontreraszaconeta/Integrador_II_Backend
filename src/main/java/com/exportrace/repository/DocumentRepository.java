package com.exportrace.repository;

import com.exportrace.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByLoteId(Long loteId);
    List<Document> findByLoteIdOrderByFechaSubidaDesc(Long loteId);
    List<Document> findByLoteIdAndActiveTrue(Long loteId);
    List<Document> findByLoteIdAndTipoOrderByVersionDesc(Long loteId, String tipo);
    Optional<Document> findByLoteIdAndTipoAndVersion(Long loteId, String tipo, Integer version);
}
