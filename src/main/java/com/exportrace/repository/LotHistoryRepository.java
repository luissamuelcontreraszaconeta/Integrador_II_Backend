package com.exportrace.repository;

import com.exportrace.entity.LotHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotHistoryRepository extends JpaRepository<LotHistory, Long> {
    List<LotHistory> findByLoteIdOrderByFechaCambioDesc(Long loteId);
}
