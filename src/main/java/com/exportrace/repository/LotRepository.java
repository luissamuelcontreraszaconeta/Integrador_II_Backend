package com.exportrace.repository;

import com.exportrace.entity.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {
    Optional<Lot> findByCodigo(String codigo);
    Optional<Lot> findByQrToken(String qrToken);
    List<Lot> findByEstado(String estado);
    List<Lot> findAllByOrderByFechaCreacionDesc();
    Optional<Lot> findTopByOrderByIdDesc();
}
