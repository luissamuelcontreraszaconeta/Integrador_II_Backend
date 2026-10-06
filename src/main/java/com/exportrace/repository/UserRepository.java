package com.exportrace.repository;

import com.exportrace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    long countByActivoTrue();
    long countByActivoFalse();
    long countByRoleNombreAndActivoTrue(String roleNombre);
    long countByRoleId(Long roleId);
    List<User> findByRoleId(Long roleId);
    List<User> findByRoleNombre(String roleNombre);
}
