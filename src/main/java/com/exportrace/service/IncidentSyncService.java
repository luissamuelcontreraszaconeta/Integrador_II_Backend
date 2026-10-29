package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.entity.IncidentReport;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.repository.IncidentReportRepository;
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
import java.util.Set;

/**
 * Servicio orquestador de sincronización ExporTrace -> Jira Service Management (Fase C).
 *
 * Garantías arquitectónicas:
 * 1. Separación estricta entre transacción local de BD y llamadas HTTP externas a Jira.
 *    Nunca mantiene una transacción de base de datos abierta mientras espera respuesta de Jira.
 * 2. Idempotencia mediante estrategia LOOKUP BEFORE CREATE usando correlationId (UUID) en Jira.
 * 3. Clasificación de fallos en RETRY_PENDING (transitorios: timeout, 429, 502, 503, 504)
 *    vs ERROR (permanentes/configuración: 400, 401, 403, 404, 422, configuración inválida, conflicto duplicado).
 * 4. Trazabilidad completa en AuditService sin exponer credenciales ni tokens.
 */
@Service
public class IncidentSyncService {

    private static final Logger log = LoggerFactory.getLogger(IncidentSyncService.class);

    private static final Set<JiraSyncStatus> RETRYABLE_SYNC_STATUSES = EnumSet.of(
            JiraSyncStatus.PENDING,
            JiraSyncStatus.RETRY_PENDING,
            JiraSyncStatus.ERROR
    );

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraIncidentService jiraIncidentService;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Sincroniza una incidencia local ya persistida hacia Jira Service Management.
     * Si JIRA_ENABLED=false, no realiza ninguna llamada externa y conserva el estado local.
     */
    public IncidentDTO syncIncident(
            Long incidentId,
            Long actorUserId,
            String actorName,
            String actorRole,
            HttpServletRequest httpRequest) {

        if (!jiraProperties.isEnabled()) {
            return transactionTemplate.execute(status -> {
                IncidentReport inc = incidentReportRepository.findById(incidentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Incidencia no encontrada con ID: " + incidentId));
                return new IncidentDTO(inc);
            });
        }

        final String resolvedActorName = (actorName != null && !actorName.isBlank()) ? actorName : "SISTEMA_SYNC";
        final String resolvedActorRole = (actorRole != null && !actorRole.isBlank()) ? actorRole : "SISTEMA";

        // 1. Transacción corta: incrementar syncAttempts, tomar snapshot desacoplado y auditar INCIDENT_SYNC_ATTEMPT
        IncidentReport snapshot = transactionTemplate.execute(status -> {
            IncidentReport inc = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            // Inicializar relación lazy de lote si existe para poder usarla fuera de la transacción
            if (inc.getRelatedLot() != null) {
                inc.getRelatedLot().getCodigo();
            }

            int nextAttempt = (inc.getSyncAttempts() != null ? inc.getSyncAttempts() : 0) + 1;
            inc.setSyncAttempts(nextAttempt);
            IncidentReport saved = incidentReportRepository.saveAndFlush(inc);

            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, attempt=%d, previousSyncStatus=%s",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    nextAttempt,
                    saved.getSyncStatus() != null ? saved.getSyncStatus().name() : "PENDING"
            );

            auditService.logAction(
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    "INCIDENT_SYNC_ATTEMPT",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(saved.getId()),
                    "Intento #" + nextAttempt + " de sincronización con Jira para incidencia " + saved.getCode(),
                    null,
                    metadata,
                    "EN_PROCESO",
                    httpRequest
            );

            return saved;
        });

        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada con ID: " + incidentId);
        }

        // 2. Validar configuración Jira ANTES de realizar llamadas HTTP
        JiraIncidentService.ConfigValidationResult configResult = jiraIncidentService.validateConfiguration();
        if (!configResult.isValid()) {
            return recordFailureOutcome(
                    incidentId,
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
                    jiraIncidentService.findByCorrelationId(snapshot.getCorrelationId());

            if (lookupOutcome.getStatus() == JiraIncidentService.LookupStatus.FOUND_SINGLE) {
                // Ticket ya existía en Jira (ej. creado antes de un timeout previo): recuperar sin duplicar
                return recordRecoveredDuplicateOutcome(
                        incidentId,
                        actorUserId,
                        resolvedActorName,
                        resolvedActorRole,
                        lookupOutcome.getIssueKey(),
                        lookupOutcome.getRequestId(),
                        httpRequest
                );
            }

            if (lookupOutcome.getStatus() == JiraIncidentService.LookupStatus.DUPLICATE_CONFLICT) {
                // Múltiples tickets encontrados con el mismo correlationId: NO crear otro, marcar conflicto
                return recordDuplicateConflictOutcome(
                        incidentId,
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
                        incidentId,
                        actorUserId,
                        resolvedActorName,
                        resolvedActorRole,
                        lookupOutcome.getErrorSyncStatus(),
                        lookupOutcome.getSanitizedError(),
                        httpRequest
                );
            }
        }

        // 4. Consultar metadata de campos del Request Type en Jira (fuera de transacción de BD)
        JiraIncidentService.RequestTypeFieldsOutcome fieldsOutcome = jiraIncidentService.getRequestTypeFields();
        if (!fieldsOutcome.isSuccess()) {
            return recordFailureOutcome(
                    incidentId,
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
                jiraIncidentService.createCustomerRequest(snapshot, fieldsOutcome.getMetadata());

        if (createOutcome.isCreated()) {
            return recordSyncedOutcome(
                    incidentId,
                    actorUserId,
                    resolvedActorName,
                    resolvedActorRole,
                    createOutcome.getIssueKey(),
                    createOutcome.getRequestId(),
                    httpRequest
            );
        }

        return recordFailureOutcome(
                incidentId,
                actorUserId,
                resolvedActorName,
                resolvedActorRole,
                createOutcome.getErrorSyncStatus(),
                createOutcome.getSanitizedError(),
                httpRequest
        );
    }

    /**
     * Endpoint administrativo: POST /api/admin/incidents/{id}/retry-sync
     * Exclusivo para ADMINISTRADOR y SUPERADMIN.
     * Verifica precondiciones, audita INCIDENT_SYNC_RETRY_REQUESTED y ejecuta LOOKUP BEFORE CREATE.
     */
    public IncidentDTO retrySyncForAdmin(Long incidentId, String adminEmail, HttpServletRequest httpRequest) {
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

        // Validar precondiciones y registrar INCIDENT_SYNC_RETRY_REQUESTED en transacción corta
        transactionTemplate.executeWithoutResult(status -> {
            IncidentReport incident = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            boolean hasIssueKey = incident.getJiraIssueKey() != null && !incident.getJiraIssueKey().isBlank();
            JiraSyncStatus currentSyncStatus = incident.getSyncStatus() != null
                    ? incident.getSyncStatus()
                    : JiraSyncStatus.PENDING;

            if (hasIssueKey && currentSyncStatus == JiraSyncStatus.SYNCED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La incidencia " + incident.getCode() + " ya se encuentra sincronizada con Jira (" + incident.getJiraIssueKey() + ").");
            }

            if (!RETRYABLE_SYNC_STATUSES.contains(currentSyncStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estado de sincronización actual (" + currentSyncStatus + ") no admite reintento manual.");
            }

            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, previousSyncStatus=%s, syncAttempts=%d",
                    incident.getCode(),
                    incident.getCorrelationId(),
                    currentSyncStatus.name(),
                    incident.getSyncAttempts() != null ? incident.getSyncAttempts() : 0
            );

            auditService.logAction(
                    adminUser.getId(),
                    actorName,
                    roleName,
                    "INCIDENT_SYNC_RETRY_REQUESTED",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(incident.getId()),
                    "Reintento manual de sincronización Jira solicitado para incidencia " + incident.getCode(),
                    currentSyncStatus.name(),
                    metadata,
                    "SOLICITADO",
                    httpRequest
            );
        });

        return syncIncident(incidentId, adminUser.getId(), actorName, roleName, httpRequest);
    }

    private IncidentDTO recordSyncedOutcome(
            Long incidentId,
            Long actorUserId,
            String actorName,
            String actorRole,
            String issueKey,
            String requestId,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            IncidentReport inc = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            String previousSync = inc.getSyncStatus() != null ? inc.getSyncStatus().name() : "PENDING";
            inc.setJiraIssueKey(issueKey);
            inc.setJiraRequestId(requestId);
            inc.setJiraLastSyncAt(LocalDateTime.now());
            inc.setSyncStatus(JiraSyncStatus.SYNCED);
            inc.setLastSyncError(null);

            IncidentReport saved = incidentReportRepository.saveAndFlush(inc);

            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, jiraIssueKey=%s, jiraRequestId=%s, syncStatus=SYNCED, attempts=%d",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    issueKey,
                    requestId != null ? requestId : "N/A",
                    saved.getSyncAttempts()
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "INCIDENT_SYNCED_TO_JIRA",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(saved.getId()),
                    "Incidencia " + saved.getCode() + " sincronizada exitosamente con Jira (" + issueKey + ")",
                    previousSync,
                    metadata,
                    "EXITOSO",
                    httpRequest
            );

            return new IncidentDTO(saved);
        });
    }

    private IncidentDTO recordRecoveredDuplicateOutcome(
            Long incidentId,
            Long actorUserId,
            String actorName,
            String actorRole,
            String recoveredIssueKey,
            String recoveredRequestId,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            IncidentReport inc = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            String previousSync = inc.getSyncStatus() != null ? inc.getSyncStatus().name() : "PENDING";
            inc.setJiraIssueKey(recoveredIssueKey);
            inc.setJiraRequestId(recoveredRequestId);
            inc.setJiraLastSyncAt(LocalDateTime.now());
            inc.setSyncStatus(JiraSyncStatus.SYNCED);
            inc.setLastSyncError(null);

            IncidentReport saved = incidentReportRepository.saveAndFlush(inc);

            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, jiraIssueKey=%s, jiraRequestId=%s, syncStatus=SYNCED, attempts=%d",
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
                    "INCIDENT_SYNC_DUPLICATE_RECOVERED",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(saved.getId()),
                    "Ticket Jira existente (" + recoveredIssueKey + ") recuperado por correlationId sin duplicar solicitud para " + saved.getCode(),
                    previousSync,
                    metadata,
                    "EXITOSO",
                    httpRequest
            );

            return new IncidentDTO(saved);
        });
    }

    private IncidentDTO recordDuplicateConflictOutcome(
            Long incidentId,
            Long actorUserId,
            String actorName,
            String actorRole,
            java.util.List<String> duplicateIssueKeys,
            String sanitizedError,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            IncidentReport inc = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            String previousSync = inc.getSyncStatus() != null ? inc.getSyncStatus().name() : "PENDING";
            String safeError = jiraIncidentService.sanitizeErrorMessage(sanitizedError);
            inc.setSyncStatus(JiraSyncStatus.ERROR);
            inc.setLastSyncError(safeError);

            IncidentReport saved = incidentReportRepository.saveAndFlush(inc);

            String keysJoined = duplicateIssueKeys != null ? String.join("|", duplicateIssueKeys) : "MULTIPLE";
            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, duplicateJiraKeys=%s, syncStatus=ERROR, attempts=%d",
                    saved.getCode(),
                    saved.getCorrelationId(),
                    keysJoined,
                    saved.getSyncAttempts()
            );

            auditService.logAction(
                    actorUserId,
                    actorName,
                    actorRole,
                    "INCIDENT_SYNC_DUPLICATE_CONFLICT",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(saved.getId()),
                    "Inconsistencia detectada: múltiples tickets en Jira para correlationId de " + saved.getCode() + ". No se creó nuevo ticket.",
                    previousSync,
                    metadata,
                    "CONFLICTO",
                    httpRequest
            );

            return new IncidentDTO(saved);
        });
    }

    private IncidentDTO recordFailureOutcome(
            Long incidentId,
            Long actorUserId,
            String actorName,
            String actorRole,
            JiraSyncStatus targetSyncStatus,
            String rawOrSanitizedError,
            HttpServletRequest httpRequest) {

        return transactionTemplate.execute(status -> {
            IncidentReport inc = incidentReportRepository.findById(incidentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Incidencia no encontrada con ID: " + incidentId));

            String previousSync = inc.getSyncStatus() != null ? inc.getSyncStatus().name() : "PENDING";
            JiraSyncStatus finalStatus = targetSyncStatus != null ? targetSyncStatus : JiraSyncStatus.RETRY_PENDING;
            String safeError = jiraIncidentService.sanitizeErrorMessage(rawOrSanitizedError);

            inc.setSyncStatus(finalStatus);
            inc.setLastSyncError(safeError);

            IncidentReport saved = incidentReportRepository.saveAndFlush(inc);

            String metadata = String.format(
                    "incidentCode=%s, correlationId=%s, syncStatus=%s, attempts=%d, detail=%s",
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
                    "INCIDENT_SYNC_FAILED",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(saved.getId()),
                    "Fallo de sincronización Jira para incidencia " + saved.getCode() + " -> " + finalStatus.name(),
                    previousSync,
                    metadata,
                    finalStatus.name(),
                    httpRequest
            );

            log.warn("[IncidentSyncService] Sincronización de incidencia {} finalizó con estado {}: {}",
                    saved.getCode(), finalStatus.name(), safeError);

            return new IncidentDTO(saved);
        });
    }
}
