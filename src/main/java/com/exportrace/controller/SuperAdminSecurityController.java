package com.exportrace.controller;

import com.exportrace.dto.SessionPolicyDTO;
import com.exportrace.dto.UpdateSessionPolicyRequest;
import com.exportrace.dto.UserSessionDTO;
import com.exportrace.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/security")
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminSecurityController {

    @Autowired
    private SessionService sessionService;

    // 1. Obtener todas las sesiones de usuarios
    @GetMapping("/sessions")
    public ResponseEntity<List<UserSessionDTO>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    // 2. Revocar sesión específica
    @PostMapping("/sessions/{sessionId}/revoke")
    public ResponseEntity<?> revokeSession(@PathVariable String sessionId,
                                           @RequestBody(required = false) Map<String, String> body,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        try {
            String actor = authentication != null ? authentication.getName() : "SUPERADMIN";
            String reason = body != null ? body.get("reason") : "Revocada manualmente por SuperAdministrador";
            sessionService.revokeSessionById(sessionId, reason, actor, request);
            return ResponseEntity.ok(Map.of("message", "Sesión revocada exitosamente."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Listar políticas de sesión por rol
    @GetMapping("/session-policies")
    public ResponseEntity<List<SessionPolicyDTO>> getSessionPolicies() {
        return ResponseEntity.ok(sessionService.getAllPolicies());
    }

    // 4. Obtener política de sesión por rol específico
    @GetMapping("/session-policies/{role}")
    public ResponseEntity<SessionPolicyDTO> getSessionPolicyByRole(@PathVariable String role) {
        return ResponseEntity.ok(new SessionPolicyDTO(sessionService.getEffectivePolicyForRole(role)));
    }

    // 5. Actualizar política de sesión por rol (Dinámica)
    @PutMapping("/session-policies/{role}")
    public ResponseEntity<?> updateSessionPolicy(@PathVariable String role,
                                                 @RequestBody UpdateSessionPolicyRequest req,
                                                 Authentication authentication,
                                                 HttpServletRequest request) {
        try {
            String actor = authentication != null ? authentication.getName() : "SUPERADMIN";
            SessionPolicyDTO updated = sessionService.updatePolicy(role, req, actor, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error al actualizar política: " + e.getMessage()));
        }
    }
}
