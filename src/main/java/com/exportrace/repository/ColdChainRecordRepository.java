package com.exportrace.repository;

import com.exportrace.entity.ColdChainRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColdChainRecordRepository extends JpaRepository<ColdChainRecord, Long> {
    List<ColdChainRecord> findByLoteIdOrderByFechaHoraDesc(Long loteId);
}
