package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.repository.ServiceRequestRepository;
import com.exportrace.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio orquestador de sincronización ExporTrace -> Jira Service Management
 * para Solicitudes de Servicio (Piloto SR-INF-01).
 */
@Service
public class ServiceRequestSyncService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestSyncService.class);

    private static final Set<JiraSyncStatus> RETRYABLE_SYNC_STATUSES = EnumSet.of(
            JiraSyncStatus.PENDING,
            JiraSyncStatus.RETRY_PENDING,
            JiraSyncStatus.ERROR
    );

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraServiceRequestService jiraServiceRequestService;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public ServiceRequestDTO syncServiceRequest(
            Long serviceRequestId,
            Long actorUserId,
            String actorName,
            String actorRole,
            HttpServletRequest httpRequest) {

        if (!jiraProperties.isEnabled()) {
            return transactionTemplate.execute(status -> {
                ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Solicitud de servicio no encontrada con ID: " + serviceRequestId));
                return new ServiceRequestDTO(sr);
            });
        }

        final String resolvedActorName = (actorName != null && !actorName.isBlank()) ? actorName : "SISTEMA_SYNC";
        final String resolvedActorRole = (actorRole != null && !actorRole.isBlank()) ? actorRole : "SISTEMA";

        // 1. Transacción corta: incrementar syncAttempts y auditar SERVICE_REQUEST_SYNC_ATTEMPT
        ServiceRequest snapshot = transactionTemplate.execute(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            int nextAttempt = (sr.getSyncAttempts() != null ? sr.getSyncAttempts() : 0) + 1;
            sr.setSyncAttempts(nextAttempt);
            if (sr.getJiraRequestTypeId() == null || sr.getJiraRequestTypeId().isBlank()) {
                sr.setJiraRequestTypeId(jiraServiceRequestService.resolveSrInf01RequestTypeId());
            }
            ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);

            String metadata = String.format(
                    "code=%s, serviceCode=%s, correlationId=%s, attempt=%d, previousSyncStatus=%s",
                    saved.getCode(),
                    saved.getServiceCode(),
                    saved.getCorrelationId(),
                    nextAttempt,
                    saved.getSyncStatus() != null ? saved.getSyncStatus().name() : "PENDING"
            );

            auditService.logAction(
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    "SERVICE_REQUEST_SYNC_ATTEMPT",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(saved.getId()),
                    "Intento #" + nextAttempt + " de sincronización Jira para solicitud " + saved.getCode(),
                    null,
                    metadata,
                    "EN_PROCESO",
                    httpRequest
            );

            return saved;
        });

        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Solicitud de servicio no encontrada con ID: " + serviceRequestId);
        }

        // 2. Validar configuración Jira ANTES de realizar llamadas HTTP
        JiraIncidentService.ConfigValidationResult configResult = jiraServiceRequestService.validateConfiguration();
        if (!configResult.isValid()) {
            return recordFailureOutcome(
                    serviceRequestId,
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    JiraSyncStatus.ERROR,
                    configResult.getErrorMessage(),
                    httpRequest
            );
        }

        // 3. LOOKUP BEFORE CREATE (Idempotencia fuera de transacción de BD)
        if (snapshot.getJiraIssueKey() == null || snapshot.getJiraIssueKey().isBlank()) {
            JiraIncidentService.JiraLookupOutcome lookupOutcome =
                    jiraServiceRequestService.findByCorrelationId(snapshot.getCorrelationId());

            if (lookupOutcome.getStatus() == JiraIncidentService.LookupStatus.FOUND_SINGLE) {
                return recordRecoveredDuplicateOutcome(
                        serviceRequestId,
                        actorUserId,
                        resolvedActorName,
                        resolvedActorRole,
                        lookupOutcome.getIssueKey(),
                        lookupOutcome.getRequestId(),
                        httpRequest
                );
            }

            if (lookupOutcome.getStatus() == JiraIncidentService.LookupStatus.DUPLICATE_CONFLICT) {
                return recordDuplicateConflictOutcome(
                        serviceRequestId,
                        actorUserId,
                        resolvedActorName,
                        resolvedActorRole,
                        lookupOutcome.getDuplicateIssueKeys(),
                        lookupOutcome.getSanitizedError(),
                        httpRequest
                );
            }

            if (lookupOutcome.getStatus() == JiraIncidentService.LookupStatus.FAILED) {
                return recordFailureOutcome(
                        serviceRequestId,
                        actorUserId,
                        resolvedActorName,
                        resolvedActorRole,
                        lookupOutcome.getErrorSyncStatus(),
                        lookupOutcome.getSanitizedError(),
                        httpRequest
                );
            }
        }

        // 4. Consultar metadata de campos del Request Type 10116 en Jira (fuera de transacción de BD)
        JiraServiceRequestService.SrInf01FieldsOutcome fieldsOutcome =
                jiraServiceRequestService.getSrInf01RequestTypeFields();
        if (!fieldsOutcome.isSuccess()) {
            return recordFailureOutcome(
                    serviceRequestId,
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    fieldsOutcome.getErrorSyncStatus(),
                    fieldsOutcome.getSanitizedError(),
                    httpRequest
            );
        }

        // 5. Crear Customer Request en Jira Service Management (fuera de transacción de BD)
        JiraIncidentService.JiraCreateOutcome createOutcome =
                jiraServiceRequestService.createSrInf01CustomerRequest(snapshot, fieldsOutcome.getMetadata());

        if (createOutcome.isCreated()) {
            return recordSyncedOutcome(
                    serviceRequestId,
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    createOutcome.getIssueKey(),
                    createOutcome.getRequestId(),
                    httpRequest
            );
        }

        return recordFailureOutcome(
                serviceRequestId,
                actorUserId,
                resolvedActorName,
                resolvedActorRole,
                createOutcome.getErrorSyncStatus(),
                createOutcome.getSanitizedError(),
                httpRequest
        );
    }

    public ServiceRequestDTO retrySyncForAdmin(Long serviceRequestId, String adminEmail, HttpServletRequest httpRequest) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }

        User adminUser = userRepository.findByEmail(adminEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adminUser.getActivo())) {
            throw new AccessDeniedException("La cuenta de usuario se encuentra desactivada.");
        }

        String roleName = adminUser.getRole() != null && adminUser.getRole().getNombre() != null
                ? adminUser.getRole().getNombre()
                : "USUARIO";

        if (!"ADMINISTRADOR".equalsIgnoreCase(roleName) && !"SUPERADMIN".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("Acceso denegado: Solo ADMINISTRADOR o SUPERADMIN pueden reintentar la sincronización con Jira.");
        }

        String fullName = adminUser.getNombre() != null ? adminUser.getNombre().trim() : "";
        if (adminUser.getApellido() != null && !adminUser.getApellido().isBlank()) {
            fullName = (fullName + " " + adminUser.getApellido().trim()).trim();
        }
        if (fullName.isEmpty()) {
            fullName = adminUser.getEmail();
        }
        final String actorName = fullName;

        transactionTemplate.executeWithoutResult(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            boolean hasIssueKey = sr.getJiraIssueKey() != null && !sr.getJiraIssueKey().isBlank();
            JiraSyncStatus currentSyncStatus = sr.getSyncStatus() != null
                    ? sr.getSyncStatus()
                    : JiraSyncStatus.PENDING;

            if (hasIssueKey && currentSyncStatus == JiraSyncStatus.SYNCED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La solicitud " + sr.getCode() + " ya se encuentra sincronizada con Jira (" + sr.getJiraIssueKey() + ").");
            }

            if (!RETRYABLE_SYNC_STATUSES.contains(currentSyncStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estado de sincronización actual (" + currentSyncStatus + ") no admite reintento manual.");
            }

            String metadata = String.format(
                    "code=%s, correlationId=%s, previousSyncStatus=%s, syncAttempts=%d",
                    sr.getCode(),
                    sr.getCorrelationId(),
                    currentSyncStatus.name(),
                    sr.getSyncAttempts() != null ? sr.getSyncAttempts() : 0
            );

            auditService.logAction(
                    adminUser.getId(),
                    actorName,
                    roleName,
                    "SERVICE_REQUEST_SYNC_RETRY_REQUESTED",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(sr.getId()),
                    "Reintento manual de sincronización Jira solicitado para solicitud " + sr.getCode(),
                    currentSyncStatus.name(),
                    metadata,
                    "SOLICITADO",
                    httpRequest
            );
        });

        return syncServiceRequest(serviceRequestId, adminUser.getId(), actorName, roleName, httpRequest);
    }

    private ServiceRequestDTO recordSyncedOutcome(
            Long serviceRequestId,
            Long actorUserId,
            String actorName,
            String actorRole,
            String issueKey,
            String requestId,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            String previousSync = sr.getSyncStatus() != null ? sr.getSyncStatus().name() : "PENDING";
            sr.setJiraIssueKey(issueKey);
            sr.setJiraRequestId(requestId);
            sr.setJiraRequestTypeId(jiraServiceRequestService.resolveSrInf01RequestTypeId());
            sr.setJiraLastSyncAt(LocalDateTime.now());
            sr.setSyncStatus(JiraSyncStatus.SYNCED);
            sr.setSlaAuthority("JIRA_AUTHORITATIVE");
            sr.setLastSyncError(null);

            ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);

            String metadata = String.format(
                    "code=%s, serviceCode=%s, correlationId=%s, jiraIssueKey=%s, jiraRequestId=%s, requestTypeId=%s, syncStatus=SYNCED, attempts=%d",
                    saved.getCode(),
                    saved.getServiceCode(),
                    saved.getCorrelationId(),
                    issueKey,
                    requestId != null ? requestId : "N/A",
                    saved.getJiraRequestTypeId(),
                    saved.getSyncAttempts()
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "SERVICE_REQUEST_SYNCED_TO_JIRA",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(saved.getId()),
                    "Solicitud " + saved.getCode() + " sincronizada exitosamente con Jira (" + issueKey + ")",
                    previousSync,
                    metadata,
                    "EXITOSO",
                    httpRequest
            );

            return new ServiceRequestDTO(saved);
        });
    }

    private ServiceRequestDTO recordRecoveredDuplicateOutcome(
            Long serviceRequestId,
            Long actorUserId,
            String actorName,
            String actorRole,
            String recoveredIssueKey,
            String recoveredRequestId,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            String previousSync = sr.getSyncStatus() != null ? sr.getSyncStatus().name() : "PENDING";
            sr.setJiraIssueKey(recoveredIssueKey);
            sr.setJiraRequestId(recoveredRequestId);
            sr.setJiraRequestTypeId(jiraServiceRequestService.resolveSrInf01RequestTypeId());
            sr.setJiraLastSyncAt(LocalDateTime.now());
            sr.setSyncStatus(JiraSyncStatus.SYNCED);
            sr.setSlaAuthority("JIRA_AUTHORITATIVE");
            sr.setLastSyncError(null);

            ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);

            String metadata = String.format(
                    "code=%s, correlationId=%s, jiraIssueKey=%s, jiraRequestId=%s, syncStatus=SYNCED, attempts=%d",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    recoveredIssueKey,
                    recoveredRequestId != null ? recoveredRequestId : "N/A",
                    saved.getSyncAttempts()
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "SERVICE_REQUEST_SYNC_DUPLICATE_RECOVERED",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(saved.getId()),
                    "Ticket Jira existente (" + recoveredIssueKey + ") recuperado por correlationId sin duplicar solicitud para " + saved.getCode(),
                    previousSync,
                    metadata,
                    "EXITOSO",
                    httpRequest
            );

            return new ServiceRequestDTO(saved);
        });
    }

    private ServiceRequestDTO recordDuplicateConflictOutcome(
            Long serviceRequestId,
            Long actorUserId,
            String actorName,
            String actorRole,
            List<String> duplicateIssueKeys,
            String sanitizedError,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            String previousSync = sr.getSyncStatus() != null ? sr.getSyncStatus().name() : "PENDING";
            String safeError = jiraServiceRequestService.sanitizeErrorMessage(sanitizedError);
            sr.setSyncStatus(JiraSyncStatus.ERROR);
            sr.setLastSyncError(safeError);

            ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);

            String keysJoined = duplicateIssueKeys != null ? String.join("|", duplicateIssueKeys) : "MULTIPLE";
            String metadata = String.format(
                    "code=%s, correlationId=%s, duplicateJiraKeys=%s, syncStatus=ERROR, attempts=%d",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    keysJoined,
                    saved.getSyncAttempts()
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "SERVICE_REQUEST_SYNC_DUPLICATE_CONFLICT",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(saved.getId()),
                    "Inconsistencia detectada: múltiples tickets en Jira para correlationId de " + saved.getCode() + ". No se creó nuevo ticket.",
                    previousSync,
                    metadata,
                    "CONFLICTO",
                    httpRequest
            );

            return new ServiceRequestDTO(saved);
        });
    }

    private ServiceRequestDTO recordFailureOutcome(
            Long serviceRequestId,
            Long actorUserId,
            String actorName,
            String actorRole,
            JiraSyncStatus targetSyncStatus,
            String rawOrSanitizedError,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Solicitud de servicio no encontrada con ID: " + serviceRequestId));

            String previousSync = sr.getSyncStatus() != null ? sr.getSyncStatus().name() : "PENDING";
            JiraSyncStatus finalStatus = targetSyncStatus != null ? targetSyncStatus : JiraSyncStatus.RETRY_PENDING;
            String safeError = jiraServiceRequestService.sanitizeErrorMessage(rawOrSanitizedError);

            sr.setSyncStatus(finalStatus);
            sr.setLastSyncError(safeError);

            ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);

            String metadata = String.format(
                    "code=%s, correlationId=%s, syncStatus=%s, attempts=%d, detail=%s",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    finalStatus.name(),
                    saved.getSyncAttempts(),
                    safeError
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "SERVICE_REQUEST_SYNC_FAILED",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(saved.getId()),
                    "Fallo de sincronización Jira para solicitud " + saved.getCode() + " -> " + finalStatus.name(),
                    previousSync,
                    metadata,
                    finalStatus.name(),
                    httpRequest
            );

            log.warn("[ServiceRequestSyncService] Sincronización de solicitud {} finalizó con estado {}: {}",
                    saved.getCode(), finalStatus.name(), safeError);

            return new ServiceRequestDTO(saved);
        });
    }
}
