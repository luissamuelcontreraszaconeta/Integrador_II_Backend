package com.exportrace.repository;

import com.exportrace.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCodigo(String codigo);
    List<Permission> findByActivoTrue();
    List<Permission> findByModuleCodigo(String moduleCodigo);
}
