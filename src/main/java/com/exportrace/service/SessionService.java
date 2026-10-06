package com.exportrace.service;

import com.exportrace.dto.*;
import com.exportrace.entity.Role;
import com.exportrace.entity.SessionPolicy;
import com.exportrace.entity.User;
import com.exportrace.entity.UserSession;
import com.exportrace.repository.SessionPolicyRepository;
import com.exportrace.repository.UserRepository;
import com.exportrace.repository.UserSessionRepository;
import com.exportrace.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private SessionPolicyRepository sessionPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    /**
     * Hashing SHA-256 for opaque refresh tokens so raw tokens are never persisted.
     */
    public String hashToken(String token) {
        if (token == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash de token", e);
        }
    }

    /**
     * Returns effective session policy for role with fallback defaults.
     */
    public SessionPolicy getEffectivePolicyForRole(String role) {
        if (role == null) role = "PRODUCCION";
        String normalizedRole = role.toUpperCase().trim();

        return sessionPolicyRepository.findByRole(normalizedRole)
                .orElseGet(() -> {
                    // Fallback default policy per role
                    return switch (normalizedRole) {
                        case "SUPERADMIN" -> new SessionPolicy("SUPERADMIN", 15, 120, 2, "SYSTEM");
                        case "ADMINISTRADOR" -> new SessionPolicy("ADMINISTRADOR", 20, 240, 2, "SYSTEM");
                        case "QA" -> new SessionPolicy("QA", 30, 360, 2, "SYSTEM");
                        case "LOGISTICA" -> new SessionPolicy("LOGISTICA", 45, 480, 5, "SYSTEM");
                        case "GERENCIA" -> new SessionPolicy("GERENCIA", 30, 240, 2, "SYSTEM");
                        default -> new SessionPolicy("PRODUCCION", 60, 480, 5, "SYSTEM");
                    };
                });
    }

    /**
     * Creates a new user session upon successful login.
     */
    @Transactional
    public AuthResponse createSession(User user, HttpServletRequest httpRequest) {
        String roleName = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        SessionPolicy policy = getEffectivePolicyForRole(roleName);

        String sessionId = UUID.randomUUID().toString();
        String rawRefreshToken = UUID.randomUUID().toString().replace("-", "") + "-" + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawRefreshToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime absoluteExpiresAt = now.plusMinutes(policy.getAbsoluteTimeoutMinutes());

        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Unknown";
        if (userAgent != null && userAgent.length() > 490) {
            userAgent = userAgent.substring(0, 490);
        }

        UserSession session = new UserSession(
                sessionId,
                user,
                tokenHash,
                now,
                absoluteExpiresAt,
                ipAddress,
                userAgent
        );
        userSessionRepository.save(session);

        String accessToken = jwtUtil.generateAccessToken(user, sessionId);
        UserDTO userDTO = new UserDTO(user);
        SessionPolicyDTO policyDTO = new SessionPolicyDTO(policy);

        return new AuthResponse(accessToken, rawRefreshToken, sessionId, policyDTO, userDTO);
    }

    /**
     * Validates refresh token against dynamic session policy, updates last activity and returns a fresh access token.
     */
    @Transactional
    public AuthResponse refreshSession(String rawRefreshToken, HttpServletRequest httpRequest) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new RuntimeException("Token de refresco requerido.");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada o token de refresco inválido."));

        LocalDateTime now = LocalDateTime.now();

        // 1. Check if session was revoked
        if ("REVOKED".equalsIgnoreCase(session.getStatus())) {
            String reason = session.getRevocationReason() != null ? session.getRevocationReason() : "Sesión revocada por seguridad";
            throw new RuntimeException("SESSION_REVOKED: " + reason);
        }

        // 2. Check if user is still active
        User user = session.getUser();
        if (user == null || Boolean.FALSE.equals(user.getActivo())) {
            session.setStatus("REVOKED");
            session.setRevokedAt(now);
            session.setRevocationReason("Usuario desactivado o eliminado");
            userSessionRepository.save(session);
            throw new RuntimeException("SESSION_REVOKED: La cuenta de usuario se encuentra desactivada.");
        }

        // 3. Dynamic policy check
        String roleName = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        SessionPolicy policy = getEffectivePolicyForRole(roleName);

        // Check Idle Timeout
        long idleMinutes = ChronoUnit.MINUTES.between(session.getLastActivityAt(), now);
        if (idleMinutes > policy.getIdleTimeoutMinutes()) {
            session.setStatus("EXPIRED");
            session.setRevocationReason("Inactividad superada (" + idleMinutes + "m > " + policy.getIdleTimeoutMinutes() + "m)");
            userSessionRepository.save(session);

            auditService.logAction(
                    user.getId(),
                    user.getNombre(),
                    roleName,
                    "SESSION_EXPIRED",
                    "SEGURIDAD",
                    "UserSession",
                    session.getId(),
                    "Sesión expirada por inactividad (" + idleMinutes + " min)",
                    null,
                    "Inactividad máxima: " + policy.getIdleTimeoutMinutes() + " min",
                    "EXPIRADO",
                    httpRequest
            );

            throw new RuntimeException("SESSION_IDLE_EXPIRED: Sesión expirada por inactividad.");
        }

        // Check Absolute Duration Timeout
        if (now.isAfter(session.getExpiresAt())) {
            session.setStatus("EXPIRED");
            session.setRevocationReason("Duración máxima absoluta alcanzada (" + policy.getAbsoluteTimeoutMinutes() + "m)");
            userSessionRepository.save(session);

            auditService.logAction(
                    user.getId(),
                    user.getNombre(),
                    roleName,
                    "SESSION_EXPIRED",
                    "SEGURIDAD",
                    "UserSession",
                    session.getId(),
                    "Sesión expirada por duración absoluta máxima",
                    null,
                    "Duración máxima: " + policy.getAbsoluteTimeoutMinutes() + " min",
                    "EXPIRADO",
                    httpRequest
            );

            throw new RuntimeException("SESSION_ABSOLUTE_EXPIRED: Sesión expirada por duración máxima alcanzada. Por favor inicie sesión nuevamente.");
        }

        // Session is valid -> update activity
        session.setLastActivityAt(now);
        userSessionRepository.save(session);

        String newAccessToken = jwtUtil.generateAccessToken(user, session.getId());
        UserDTO userDTO = new UserDTO(user);
        SessionPolicyDTO policyDTO = new SessionPolicyDTO(policy);

        auditService.logAction(
                user.getId(),
                user.getNombre(),
                roleName,
                "TOKEN_REFRESH",
                "AUTENTICACION",
                "UserSession",
                session.getId(),
                "Token de acceso JWT renovado exitosamente",
                null,
                null,
                "EXITOSO",
                httpRequest
        );

        return new AuthResponse(newAccessToken, rawRefreshToken, session.getId(), policyDTO, userDTO);
    }

    /**
     * Keep-alive ping from frontend activity handler.
     */
    @Transactional
    public void keepAlive(String sessionId, User user) {
        if (sessionId == null) return;
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            if ("ACTIVE".equalsIgnoreCase(session.getStatus())) {
                session.setLastActivityAt(LocalDateTime.now());
                userSessionRepository.save(session);
            }
        });
    }

    /**
     * Revokes session on explicit user logout.
     */
    @Transactional
    public void logout(String rawRefreshToken, String sessionId, User user, HttpServletRequest httpRequest) {
        LocalDateTime now = LocalDateTime.now();

        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = hashToken(rawRefreshToken.trim());
            userSessionRepository.findByRefreshTokenHash(tokenHash).ifPresent(session -> {
                session.setStatus("REVOKED");
                session.setRevokedAt(now);
                session.setRevocationReason("Cierre de sesión voluntario (Logout)");
                userSessionRepository.save(session);
            });
        } else if (sessionId != null && !sessionId.isBlank()) {
            userSessionRepository.findById(sessionId).ifPresent(session -> {
                session.setStatus("REVOKED");
                session.setRevokedAt(now);
                session.setRevocationReason("Cierre de sesión voluntario (Logout)");
                userSessionRepository.save(session);
            });
        }

        if (user != null) {
            String roleName = user.getRole() != null ? user.getRole().getNombre() : "UNKNOWN";
            auditService.logAction(
                    user.getId(),
                    user.getNombre(),
                    roleName,
                    "LOGOUT",
                    "AUTENTICACION",
                    "User",
                    String.valueOf(user.getId()),
                    "Cierre de sesión exitoso",
                    null,
                    null,
                    "EXITOSO",
                    httpRequest
            );
        }
    }

    /**
     * Revokes a specific session (e.g. by SUPERADMIN).
     */
    @Transactional
    public void revokeSessionById(String sessionId, String reason, String actorUsername, HttpServletRequest httpRequest) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada con ID: " + sessionId));

        session.setStatus("REVOKED");
        session.setRevokedAt(LocalDateTime.now());
        session.setRevocationReason(reason != null ? reason : "Revocada por el SuperAdministrador (" + actorUsername + ")");
        userSessionRepository.save(session);

        auditService.logAction(
                session.getUser() != null ? session.getUser().getId() : null,
                actorUsername,
                "SUPERADMIN",
                "SESSION_REVOKED",
                "SEGURIDAD",
                "UserSession",
                sessionId,
                "Sesión revocada manualmente por SuperAdministrador",
                null,
                "Motivo: " + session.getRevocationReason(),
                "EXITOSO",
                httpRequest
        );
    }

    /**
     * Revokes all sessions for a user when deactivated, password changed or permissions updated.
     */
    @Transactional
    public void revokeAllUserSessions(Long userId, String reason, String actorUsername, HttpServletRequest httpRequest) {
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        LocalDateTime now = LocalDateTime.now();

        for (UserSession session : activeSessions) {
            session.setStatus("REVOKED");
            session.setRevokedAt(now);
            session.setRevocationReason(reason);
            userSessionRepository.save(session);
        }

        auditService.logAction(
                userId,
                actorUsername,
                "SUPERADMIN",
                "SESSION_REVOKED",
                "SEGURIDAD",
                "User",
                String.valueOf(userId),
                "Todas las sesiones activas del usuario fueron revocadas. Total: " + activeSessions.size(),
                null,
                "Motivo: " + reason,
                "EXITOSO",
                httpRequest
        );
    }

    /**
     * Retrieves all sessions for the SuperAdmin view.
     */
    public List<UserSessionDTO> getAllSessions() {
        return userSessionRepository.findAllByOrderByLastActivityAtDesc().stream()
                .map(UserSessionDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all configured dynamic session policies.
     */
    public List<SessionPolicyDTO> getAllPolicies() {
        return sessionPolicyRepository.findAll().stream()
                .map(SessionPolicyDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Updates dynamic session policy per role.
     */
    @Transactional
    public SessionPolicyDTO updatePolicy(String role, UpdateSessionPolicyRequest req, String superAdminUsername, HttpServletRequest httpRequest) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        req.validate();

        String normalizedRole = role.toUpperCase().trim();
        SessionPolicy policy = sessionPolicyRepository.findByRole(normalizedRole)
                .orElseGet(() -> new SessionPolicy(normalizedRole, 30, 360, 2, superAdminUsername));

        String previousValues = String.format("Inactividad: %dm, Absoluto: %dm, Aviso: %dm, Activa: %b",
                policy.getIdleTimeoutMinutes(), policy.getAbsoluteTimeoutMinutes(),
                policy.getWarningBeforeMinutes(), policy.getEnabled());

        policy.setIdleTimeoutMinutes(req.getIdleTimeoutMinutes());
        policy.setAbsoluteTimeoutMinutes(req.getAbsoluteTimeoutMinutes());
        policy.setWarningBeforeMinutes(req.getWarningBeforeMinutes());
        if (req.getEnabled() != null) {
            policy.setEnabled(req.getEnabled());
        }
        policy.setUpdatedBy(superAdminUsername);
        policy.setUpdatedAt(LocalDateTime.now());

        SessionPolicy saved = sessionPolicyRepository.save(policy);

        String newValues = String.format("Inactividad: %dm, Absoluto: %dm, Aviso: %dm, Activa: %b",
                saved.getIdleTimeoutMinutes(), saved.getAbsoluteTimeoutMinutes(),
                saved.getWarningBeforeMinutes(), saved.getEnabled());

        auditService.logAction(
                null,
                superAdminUsername,
                "SUPERADMIN",
                "SESSION_POLICY_UPDATED",
                "SEGURIDAD",
                "SessionPolicy",
                normalizedRole,
                "Política de sesión actualizada para el rol " + normalizedRole,
                previousValues,
                newValues,
                "EXITOSO",
                httpRequest
        );

        return new SessionPolicyDTO(saved);
    }

    public Map<String, Object> getSecurityOverviewMetrics() {
        long activeSessions = userSessionRepository.countByStatus("ACTIVE");
        long expiredSessions = userSessionRepository.countByStatus("EXPIRED");
        long revokedSessions = userSessionRepository.countByStatus("REVOKED");
        long totalPolicies = sessionPolicyRepository.count();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeSessions", activeSessions);
        metrics.put("expiredSessions", expiredSessions);
        metrics.put("revokedSessions", revokedSessions);
        metrics.put("totalPolicies", totalPolicies);
        return metrics;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}
