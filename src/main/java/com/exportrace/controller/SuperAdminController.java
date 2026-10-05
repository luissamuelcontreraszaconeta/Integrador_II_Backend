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
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;

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

    // 1. Dashboard de Superadministración
    @GetMapping("/dashboard")
    public ResponseEntity<SuperAdminDashboardDTO> getDashboard(
            @RequestParam(defaultValue = "quarter") String period) {
        return ResponseEntity.ok(superAdminService.getDashboard(period));
    }

    // 2. Seguridad & Alertas
    @GetMapping("/security")
    public ResponseEntity<SuperAdminSecurityDTO> getSecurityOverview() {
        return ResponseEntity.ok(superAdminService.getSecurityOverview());
    }

    // 3. Gestión de Usuarios
    @GetMapping("/users")
    public ResponseEntity<List<UserAdminDTO>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(userService.getAllUsers(search, role, status));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserAdminDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req,
                                        Authentication auth,
                                        HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            UserAdminDTO created = userService.createUser(req, adminEmail, request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody UpdateUserRequest req,
                                        Authentication auth,
                                        HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            UserAdminDTO updated = userService.updateUser(id, req, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body,
                                              Authentication auth,
                                              HttpServletRequest request) {
        try {
            Boolean activo = body.getOrDefault("activo", true);
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            UserAdminDTO updated = userService.updateUserStatus(id, activo, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id,
                                           @RequestBody(required = false) ResetPasswordRequest req,
                                           Authentication auth,
                                           HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            UserAdminDTO updated = userService.resetPassword(id, req, adminEmail, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Contraseña restablecida exitosamente para el usuario " + updated.getEmail(),
                    "user", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. Módulos Individuales por Usuario (UserModuleAccess Excepciones)
    @GetMapping("/users/{id}/modules")
    public ResponseEntity<SuperAdminUserModulesDTO> getUserModules(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminService.getUserModules(id));
    }

    @PutMapping("/users/{id}/modules")
    public ResponseEntity<?> updateUserModules(@PathVariable Long id,
                                               @RequestBody GrantModuleAccessRequest req,
                                               Authentication auth,
                                               HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            SuperAdminUserModulesDTO updated = superAdminService.updateUserModules(id, req.getModuleIds(), adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. Historial de Actividad del Usuario
    @GetMapping("/users/{id}/history")
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

    // 6. Gestión de Roles
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDTO>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/roles/{id}/permissions")
    public ResponseEntity<?> updateRolePermissions(@PathVariable Long id,
                                                   @RequestBody UpdateRolePermissionsRequest req,
                                                   Authentication auth,
                                                   HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            RoleDTO updated = roleService.updateRolePermissions(id, req.getPermissions(), adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/roles/{id}/status")
    public ResponseEntity<?> updateRoleStatus(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body,
                                              Authentication auth,
                                              HttpServletRequest request) {
        try {
            Boolean activo = body.getOrDefault("activo", true);
            String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
            RoleDTO updated = roleService.updateRoleStatus(id, activo, adminEmail, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. Catálogo de Permisos y Módulos
    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDTO>> getPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDTO>> getModules() {
        return ResponseEntity.ok(moduleService.getAllModules());
    }

    // 8. Auditoría General
    @GetMapping("/audit")
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

    // 9. Configuración del Sistema
    @GetMapping("/settings")
    public ResponseEntity<SuperAdminSettingsDTO> getSettings() {
        return ResponseEntity.ok(new SuperAdminSettingsDTO());
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody SuperAdminSettingsDTO settings,
                                            Authentication auth,
                                            HttpServletRequest request) {
        String adminEmail = auth != null ? auth.getName() : "superadmin@exportrace.pe";
        auditService.logAction(
                null,
                adminEmail,
                "SUPERADMIN",
                "SETTINGS_UPDATED",
                "SISTEMA",
                "Settings",
                "SYSTEM_CONFIG",
                "Actualización de parámetros generales de seguridad y retención del sistema",
                null,
                "AuditRetentionDays: " + settings.getAuditRetentionDays() + ", MaxFailedLogins: " + settings.getMaxFailedLoginAttempts(),
                "EXITOSO",
                request
        );
        return ResponseEntity.ok(settings);
    }
}
