package com.exportrace.repository;

import com.exportrace.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    Optional<Dispatch> findByLoteId(Long loteId);
}
