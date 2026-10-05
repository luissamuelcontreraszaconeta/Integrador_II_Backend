package com.exportrace.repository;

import com.exportrace.entity.UserModuleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserModuleAccessRepository extends JpaRepository<UserModuleAccess, Long> {
    List<UserModuleAccess> findByUserId(Long userId);
    List<UserModuleAccess> findByUserIdAndActivoTrue(Long userId);
    List<UserModuleAccess> findByModuleIdAndActivoTrue(Long moduleId);
    Optional<UserModuleAccess> findByUserIdAndModuleId(Long userId, Long moduleId);
    long countByModuleIdAndActivoTrue(Long moduleId);
}
