package com.exportrace.service;

import com.exportrace.dto.RoleDTO;
import com.exportrace.entity.Permission;
import com.exportrace.entity.Role;
import com.exportrace.entity.User;
import com.exportrace.repository.PermissionRepository;
import com.exportrace.repository.RoleRepository;
import com.exportrace.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream().map(role -> {
            long count = userRepository.countByRoleId(role.getId());
            return new RoleDTO(role, count);
        }).collect(Collectors.toList());
    }

    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + id));
        long count = userRepository.countByRoleId(role.getId());
        return new RoleDTO(role, count);
    }

    @Transactional
    public RoleDTO updateRolePermissions(Long roleId, List<String> permissionCodes,
                                         String adminEmail, HttpServletRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + roleId));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        String previousPerms = role.getPermissions().stream()
                .map(Permission::getCodigo)
                .sorted()
                .collect(Collectors.joining(", "));

        Set<Permission> updatedPermissions = new HashSet<>();
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                permissionRepository.findByCodigo(code).ifPresent(updatedPermissions::add);
            }
        }

        role.setPermissions(updatedPermissions);
        Role saved = roleRepository.save(role);

        String newPerms = saved.getPermissions().stream()
                .map(Permission::getCodigo)
                .sorted()
                .collect(Collectors.joining(", "));

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                "ROLE_PERMISSIONS_UPDATED",
                "ROLES",
                "Role",
                String.valueOf(saved.getId()),
                "Permisos actualizados para el rol " + saved.getNombre() + " (" + updatedPermissions.size() + " permisos)",
                previousPerms,
                newPerms,
                "EXITOSO",
                request
        );

        long count = userRepository.countByRoleId(saved.getId());
        return new RoleDTO(saved, count);
    }

    @Transactional
    public RoleDTO updateRoleStatus(Long roleId, Boolean activo, String adminEmail, HttpServletRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + roleId));

        if ("ADMINISTRADOR".equalsIgnoreCase(role.getNombre()) && Boolean.FALSE.equals(activo)) {
            throw new RuntimeException("No es posible desactivar el rol ADMINISTRADOR del sistema");
        }

        if (Boolean.FALSE.equals(activo)) {
            long assignedUsers = userRepository.countByRoleId(roleId);
            if (assignedUsers > 0) {
                throw new RuntimeException("No se puede desactivar un rol que tiene " + assignedUsers + " usuario(s) asignado(s)");
            }
        }

        String prevStatus = role.getActivo() ? "ACTIVO" : "INACTIVO";
        role.setActivo(activo);
        Role saved = roleRepository.save(role);
        String newStatus = saved.getActivo() ? "ACTIVO" : "INACTIVO";

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        Long adminId = admin != null ? admin.getId() : null;
        String adminName = admin != null ? admin.getNombre() : "ADMIN";
        String adminRole = admin != null && admin.getRole() != null ? admin.getRole().getNombre() : "ADMINISTRADOR";

        auditService.logAction(
                adminId,
                adminName,
                adminRole,
                Boolean.TRUE.equals(activo) ? "ROLE_ENABLED" : "ROLE_DISABLED",
                "ROLES",
                "Role",
                String.valueOf(saved.getId()),
                "Cambio de estado de rol: " + saved.getNombre() + " -> " + newStatus,
                prevStatus,
                newStatus,
                "EXITOSO",
                request
        );

        long count = userRepository.countByRoleId(saved.getId());
        return new RoleDTO(saved, count);
    }
}
