package com.exportrace.service;

import com.exportrace.dto.ModuleDTO;
import com.exportrace.entity.Module;
import com.exportrace.entity.Permission;
import com.exportrace.repository.ModuleRepository;
import com.exportrace.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModuleService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    public List<ModuleDTO> getAllModules() {
        return moduleRepository.findAllByOrderByOrdenAsc().stream().map(m -> {
            ModuleDTO dto = new ModuleDTO(m);
            List<String> perms = permissionRepository.findByModuleCodigo(m.getCodigo()).stream()
                    .map(Permission::getCodigo)
                    .collect(Collectors.toList());
            dto.setPermissions(perms);
            return dto;
        }).collect(Collectors.toList());
    }

    public List<ModuleDTO> getActiveModules() {
        return moduleRepository.findByActivoTrueOrderByOrdenAsc().stream().map(m -> {
            ModuleDTO dto = new ModuleDTO(m);
            List<String> perms = permissionRepository.findByModuleCodigo(m.getCodigo()).stream()
                    .map(Permission::getCodigo)
                    .collect(Collectors.toList());
            dto.setPermissions(perms);
            return dto;
        }).collect(Collectors.toList());
    }
}
