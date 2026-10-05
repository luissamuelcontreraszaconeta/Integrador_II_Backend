package com.exportrace.controller;

import com.exportrace.dto.*;
import com.exportrace.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private AuditService auditService;

    // 1. Dashboard Administrativo
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<AdminDashboardDTO> getDashboard() {
        AdminDashboardDTO dto = new AdminDashboardDTO();
        List<UserAdminDTO> allUsers = userService.getAllUsers(null, null, null);
        long active = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getActivo())).count();

        dto.setTotalUsers(allUsers.size());
        dto.setActiveUsers(active);
        dto.setInactiveUsers(allUsers.size() - active);
        dto.setTotalRoles(roleService.getAllRoles().size());
        dto.setTotalModules(moduleService.getAllModules().size());
        dto.setTotalAuditEvents(auditService.getTotalAuditEvents());
        dto.setAccessDeniedCount(auditService.getAccessDeniedCount());
        dto.setRecentActivity(auditService.getRecentAudits(10));

        return ResponseEntity.ok(dto);
    }

    // 2. Gestión de Usuarios
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USERS_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserAdminDTO>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(userService.getAllUsers(search, role, status));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USERS_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserAdminDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('USERS_CREATE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req,
                                        Authentication auth,
                                        HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            UserAdminDTO created = userService.createUser(req, adminEmail, request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('USERS_UPDATE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody UpdateUserRequest req,
                                        Authentication auth,
                                        HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            UserAdminDTO updated = userService.updateUser(id, req, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('USERS_DISABLE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body,
                                              Authentication auth,
                                              HttpServletRequest request) {
        try {
            Boolean activo = body.getOrDefault("activo", true);
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            UserAdminDTO updated = userService.updateUserStatus(id, activo, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-password")
    @PreAuthorize("hasAuthority('USERS_RESET_PASSWORD') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id,
                                           @RequestBody(required = false) ResetPasswordRequest req,
                                           Authentication auth,
                                           HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            UserAdminDTO updated = userService.resetPassword(id, req, adminEmail, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Contraseña restablecida exitosamente para el usuario " + updated.getEmail(),
                    "user", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/users/{id}/history")
    @PreAuthorize("hasAuthority('AUDIT_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<AuditLogDTO>> getUserHistory(
            @PathVariable Long id,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditService.getUserHistory(id, module, action, result, from, to, pageable));
    }

    // 3. Gestión de Roles y Permisos
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLES_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLES_MANAGE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> updateRolePermissions(@PathVariable Long id,
                                                   @RequestBody UpdateRolePermissionsRequest req,
                                                   Authentication auth,
                                                   HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            RoleDTO updated = roleService.updateRolePermissions(id, req.getPermissions(), adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/roles/{id}/status")
    @PreAuthorize("hasAuthority('ROLES_MANAGE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> updateRoleStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body,
                                              Authentication auth,
                                              HttpServletRequest request) {
        try {
            Boolean activo = body.getOrDefault("activo", true);
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            RoleDTO updated = roleService.updateRoleStatus(id, activo, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLES_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<PermissionDTO>> getPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    // 4. Módulos
    @GetMapping("/modules")
    @PreAuthorize("hasAuthority('ADMIN_DASHBOARD_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ModuleDTO>> getModules() {
        return ResponseEntity.ok(moduleService.getAllModules());
    }

    // 5. Auditoría General
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('AUDIT_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(auditService.getAuditLogs(userId, module, action, result, from, to, pageable));
    }
}
