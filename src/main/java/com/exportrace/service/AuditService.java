package com.exportrace.service;

import com.exportrace.dto.AuditLogDTO;
import com.exportrace.entity.AuditLog;
import com.exportrace.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(Long userId, String usernameSnapshot, String userRole,
                              String action, String module, String entityType, String entityId,
                              String description, String previousValue, String newValue,
                              String result, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);

        AuditLog log = new AuditLog(
                userId,
                usernameSnapshot != null ? usernameSnapshot : "SISTEMA",
                userRole != null ? userRole : "SYSTEM",
                action,
                module != null ? module : "SISTEMA",
                entityType,
                entityId,
                description,
                previousValue,
                newValue,
                result != null ? result : "EXITOSO",
                ipAddress,
                userAgent
        );

        return auditLogRepository.save(log);
    }

    @Transactional
    public AuditLog logAction(String action, String module, String entityType, String entityId,
                              String description, String usernameSnapshot, String userRole) {
        AuditLog log = new AuditLog(
                null,
                usernameSnapshot != null ? usernameSnapshot : "SISTEMA",
                userRole != null ? userRole : "SYSTEM",
                action,
                module != null ? module : "FRIO",
                entityType,
                entityId,
                description,
                null,
                null,
                "EXITOSO",
                "127.0.0.1",
                "Backend-Service"
        );
        return auditLogRepository.save(log);
    }

    public Page<AuditLogDTO> getAuditLogs(Long userId, String module, String action, String result,
                                         LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (module != null && !module.isBlank() && !"ALL".equalsIgnoreCase(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (result != null && !result.isBlank() && !"ALL".equalsIgnoreCase(result)) {
                predicates.add(cb.equal(root.get("result"), result));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable).map(AuditLogDTO::new);
    }

    public Page<AuditLogDTO> getUserHistory(Long userId, String module, String action, String result,
                                           LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return getAuditLogs(userId, module, action, result, fromDate, toDate, pageable);
    }

    public List<AuditLogDTO> getRecentAudits(int limit) {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(AuditLogDTO::new)
                .collect(Collectors.toList());
    }

    public long getTotalAuditEvents() {
        return auditLogRepository.count();
    }

    public long getAccessDeniedCount() {
        return auditLogRepository.countByResult("DENEGADO");
    }

    public static String getClientIp(HttpServletRequest request) {
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
        return ip;
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) return "N/A";
        String ua = request.getHeader("User-Agent");
        return ua != null ? (ua.length() > 250 ? ua.substring(0, 250) : ua) : "N/A";
    }
}
