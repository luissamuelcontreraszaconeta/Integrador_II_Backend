package com.exportrace.service;

import com.exportrace.dto.PermissionDTO;
import com.exportrace.entity.Permission;
import com.exportrace.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionDTO::new)
                .collect(Collectors.toList());
    }

    public List<PermissionDTO> getActivePermissions() {
        return permissionRepository.findByActivoTrue().stream()
                .map(PermissionDTO::new)
                .collect(Collectors.toList());
    }
}
