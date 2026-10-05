package com.exportrace.repository;

import com.exportrace.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    Optional<Module> findByCodigo(String codigo);
    List<Module> findAllByOrderByOrdenAsc();
    List<Module> findByActivoTrueOrderByOrdenAsc();
}
