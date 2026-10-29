package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.entity.IncidentReport;
import com.exportrace.entity.Lot;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.*;
import com.exportrace.repository.IncidentReportRepository;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final AtomicLong codeSequenceGenerator = new AtomicLong(0);

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private IncidentPriorityService incidentPriorityService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private IncidentSyncService incidentSyncService;

    /**
     * Flujo Fase A + Fase C:
     * 1. TRANSACCIÓN LOCAL: Guarda primero IncidentReport, auditoría INCIDENT_CREATED y notificación local.
     * 2. SINCRONIZACIÓN EXTERNA (fuera de la transacción de creación local):
     *    Si JIRA_ENABLED=true, intenta sincronizar con Jira Service Management (Lookup-Before-Create + Create Request).
     *    Si Jira falla o hace timeout, conserva la incidencia local intacta y marca RETRY_PENDING o ERROR.
     */
    public IncidentDTO createIncident(CreateIncidentRequest req, String userEmail, HttpServletRequest httpRequest) {
        LocalCreationContext localContext = transactionTemplate.execute(status ->
                saveLocalIncidentInTransaction(req, userEmail, httpRequest));

        if (localContext == null || localContext.dto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible completar el registro local de la incidencia.");
        }

        if (!jiraProperties.isEnabled()) {
            return localContext.dto;
        }

        try {
            return incidentSyncService.syncIncident(
                    localContext.dto.getId(),
                    localContext.reporterId,
                    localContext.reporterFullName,
                    localContext.reporterRole,
                    httpRequest
            );
        } catch (Exception ex) {
            log.warn("[IncidentService] La incidencia local {} fue preservada tras una excepción durante la sincronización con Jira.",
                    localContext.dto.getCode());
            return incidentReportRepository.findById(localContext.dto.getId())
                    .map(IncidentDTO::new)
                    .orElse(localContext.dto);
        }
    }

    private LocalCreationContext saveLocalIncidentInTransaction(
            CreateIncidentRequest req,
            String userEmail,
            HttpServletRequest httpRequest) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida para reportar una incidencia.");
        }

        // 1. Obtener usuario autenticado y verificar que esté activo
        User reporter = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario autenticado no encontrado."));

        if (!Boolean.TRUE.equals(reporter.getActivo())) {
            throw new AccessDeniedException("La cuenta de usuario se encuentra desactivada. No puede reportar incidencias.");
        }

        // 2. Validar request
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }
        if (req.getCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría de la incidencia es obligatoria.");
        }
        if (req.getSummary() == null || req.getSummary().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El resumen de la incidencia es obligatorio.");
        }
        if (req.getDescription() == null || req.getDescription().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción de la incidencia es obligatoria.");
        }
        if (req.getImpact() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nivel de impacto es obligatorio.");
        }
        if (req.getWorkContinuity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe responder si puede continuar con su trabajo (workContinuity).");
        }

        // 3. Validar relatedLotId si fue proporcionado
        Lot relatedLot = null;
        if (req.getRelatedLotId() != null) {
            relatedLot = lotRepository.findById(req.getRelatedLotId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "El lote relacionado con ID " + req.getRelatedLotId() + " no existe en el sistema."));
        }

        // 4. Mapear workContinuity -> IncidentUrgency y calcular Prioridad en IncidentPriorityService
        IncidentUrgency urgency = incidentPriorityService.resolveUrgency(req.getWorkContinuity());
        IncidentPriority priority = incidentPriorityService.calculatePriority(req.getImpact(), urgency);

        // 5. Capturar snapshots del usuario y rol
        String fullName = reporter.getNombre() != null ? reporter.getNombre().trim() : "";
        if (reporter.getApellido() != null && !reporter.getApellido().isBlank()) {
            fullName = (fullName + " " + reporter.getApellido().trim()).trim();
        }
        if (fullName.isEmpty()) {
            fullName = reporter.getEmail();
        }
        String roleName = reporter.getRole() != null && reporter.getRole().getNombre() != null
                ? reporter.getRole().getNombre()
                : "USUARIO";

        String resolvedModule = (req.getModule() != null && !req.getModule().isBlank())
                ? req.getModule().trim().toUpperCase()
                : req.getCategory().getDefaultModule();

        String resolvedRoute = (req.getRoute() != null && !req.getRoute().isBlank())
                ? req.getRoute().trim()
                : "/dashboard";

        LocalDateTime now = LocalDateTime.now();

        // 6. Construir entidad IncidentReport con protección de unicidad en code y correlationId
        IncidentReport saved = null;
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String correlationId = generateUniqueCorrelationId();
            String code = generateUniqueIncidentCode();

            IncidentReport incident = new IncidentReport();
            incident.setCode(code);
            incident.setCorrelationId(correlationId);
            incident.setReporterUser(reporter);
            incident.setReporterUsername(fullName);
            incident.setReporterEmail(reporter.getEmail());
            incident.setReporterRole(roleName);
            incident.setCategory(req.getCategory());
            incident.setSummary(req.getSummary().trim());
            incident.setDescription(req.getDescription().trim());
            incident.setImpact(req.getImpact());
            incident.setUrgency(urgency);
            incident.setPriority(priority);
            incident.setStatus(IncidentStatus.REPORTED);
            incident.setSyncStatus(JiraSyncStatus.PENDING);
            incident.setSyncAttempts(0);
            incident.setModule(resolvedModule);
            incident.setRoute(resolvedRoute);
            incident.setRelatedLot(relatedLot);
            incident.setCreatedAt(now);
            incident.setUpdatedAt(now);
            incident.setSlaFirstResponseTargetAt(incidentPriorityService.calculateFirstResponseTarget(now, priority));
            incident.setSlaResolutionTargetAt(incidentPriorityService.calculateResolutionTarget(now, priority));
            incident.setSlaBreached(false);

            try {
                saved = incidentReportRepository.saveAndFlush(incident);
                break;
            } catch (DataIntegrityViolationException dive) {
                log.warn("[IncidentService] Collision detected on incident code '{}' (attempt {}/{}). Retrying...", code, attempt, maxRetries);
                if (attempt == maxRetries) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Conflicto de concurrencia al generar el código único de incidencia. Por favor intente nuevamente.");
                }
            }
        }

        // 7. Emitir auditoría inmutable INCIDENT_CREATED (sin almacenar descripción completa sensible)
        String auditMetadata = String.format("code=%s, category=%s (%s), impact=%s, urgency=%s, priority=%s, status=%s, syncStatus=%s",
                saved.getCode(),
                saved.getCategory().name(),
                saved.getCategory().getCatalogCode(),
                saved.getImpact().name(),
                saved.getUrgency().name(),
                saved.getPriority().name(),
                saved.getStatus().name(),
                saved.getSyncStatus().name());

        auditService.logAction(
                reporter.getId(),
                fullName,
                roleName,
                "INCIDENT_CREATED",
                "SOPORTE",
                "IncidentReport",
                String.valueOf(saved.getId()),
                "Incidencia registrada localmente: " + saved.getCode() + " [" + saved.getCategory().getLabel() + "]",
                null,
                auditMetadata,
                "EXITOSO",
                httpRequest
        );

        // 8. Crear notificación local reutilizando NotificationService
        String notifPriority = switch (priority) {
            case P1_CRITICAL -> "URGENT";
            case P2_HIGH -> "HIGH";
            case P3_MEDIUM -> "NORMAL";
            case P4_LOW -> "LOW";
        };

        notificationService.createNotification(
                reporter,
                null,
                "Incidencia " + saved.getCode() + " registrada",
                "Incidencia " + saved.getCode() + " registrada correctamente con prioridad " + priority.getLabel() + ".",
                "INFO",
                notifPriority,
                "SOPORTE",
                "IncidentReport",
                saved.getCode(),
                "/support/incidents/" + saved.getId()
        );

        return new LocalCreationContext(new IncidentDTO(saved), reporter.getId(), fullName, roleName);
    }

    private static class LocalCreationContext {
        final IncidentDTO dto;
        final Long reporterId;
        final String reporterFullName;
        final String reporterRole;

        LocalCreationContext(IncidentDTO dto, Long reporterId, String reporterFullName, String reporterRole) {
            this.dto = dto;
            this.reporterId = reporterId;
            this.reporterFullName = reporterFullName;
            this.reporterRole = reporterRole;
        }
    }

    /**
     * Lista únicamente las incidencias reportadas por el usuario autenticado, ordenadas por createdAt DESC.
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getMyIncidents(String userEmail) {
        User user = getActiveAuthenticatedUser(userEmail);
        return incidentReportRepository.findByReporterUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(IncidentDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el detalle de una incidencia aplicando aislamiento estricto por propietario.
     * Usuarios operacionales solo pueden ver sus propias incidencias; si intentan ver una ajena -> HTTP 403 + AuditLog INCIDENT_VIEW_DENIED.
     * ADMINISTRADOR y SUPERADMIN pueden supervisar todas las incidencias.
     */
    @Transactional(noRollbackFor = AccessDeniedException.class)
    public IncidentDTO getIncidentById(Long id, String userEmail, HttpServletRequest httpRequest) {
        User currentUser = getActiveAuthenticatedUser(userEmail);
        IncidentReport incident = incidentReportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada con ID: " + id));

        String roleName = currentUser.getRole() != null && currentUser.getRole().getNombre() != null
                ? currentUser.getRole().getNombre()
                : "USUARIO";

        boolean isAdminOrSuperAdmin = "ADMINISTRADOR".equalsIgnoreCase(roleName) || "SUPERADMIN".equalsIgnoreCase(roleName);
        boolean isOwner = incident.getReporterUser() != null
                && incident.getReporterUser().getId() != null
                && incident.getReporterUser().getId().equals(currentUser.getId());

        if (!isOwner && !isAdminOrSuperAdmin) {
            String fullName = currentUser.getNombre() != null ? currentUser.getNombre() : currentUser.getEmail();
            auditService.logAction(
                    currentUser.getId(),
                    fullName,
                    roleName,
                    "INCIDENT_VIEW_DENIED",
                    "SOPORTE",
                    "IncidentReport",
                    String.valueOf(incident.getId()),
                    "Acceso denegado al consultar incidencia ajena: " + incident.getCode(),
                    null,
                    "incidentCode=" + incident.getCode() + ", category=" + incident.getCategory().name() + ", priority=" + incident.getPriority().name(),
                    "DENEGADO",
                    httpRequest
            );
            throw new AccessDeniedException("Acceso denegado: No tiene autorización para consultar incidencias reportadas por otro usuario.");
        }

        return new IncidentDTO(incident);
    }

    /**
     * Consulta administrativa de todas las incidencias para ADMINISTRADOR y SUPERADMIN.
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getAllIncidentsForAdmin(String userEmail) {
        User currentUser = getActiveAuthenticatedUser(userEmail);
        String roleName = currentUser.getRole() != null && currentUser.getRole().getNombre() != null
                ? currentUser.getRole().getNombre()
                : "USUARIO";

        if (!"ADMINISTRADOR".equalsIgnoreCase(roleName) && !"SUPERADMIN".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("Acceso denegado: Se requiere rol ADMINISTRADOR o SUPERADMIN.");
        }

        return incidentReportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(IncidentDTO::new)
                .collect(Collectors.toList());
    }

    private User getActiveAuthenticatedUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticación requerida.");
        }
        User user = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new AccessDeniedException("La cuenta de usuario se encuentra desactivada.");
        }
        return user;
    }

    /**
     * Generación de código correlativo humano (INC-2026-XXXXXX).
     *
     * Nota de arquitectura y concurrencia (Fase A):
     * 1. AtomicLong + synchronized protege la concurrencia de hilos dentro de una única instancia JVM.
     * 2. El constraint UNIQUE(code) en base de datos junto con el bloque de reintento ante
     *    DataIntegrityViolationException constituye la garantía final de integridad en BD.
     * 3. Riesgo / evolución futura: Si el despliegue en Render escala horizontalmente a múltiples
     *    instancias JVM concurrentes, deberá evolucionarse a una secuencia nativa de BD (SEQUENCE)
     *    o tabla correlativa atómica sin introducir arquitectura distribuida compleja en Fase A.
     */
    private synchronized String generateUniqueIncidentCode() {
        long baseCount = incidentReportRepository.count();
        long candidate = Math.max(baseCount + 1, codeSequenceGenerator.incrementAndGet());
        String code;
        do {
            code = String.format("INC-2026-%06d", candidate);
            if (incidentReportRepository.existsByCode(code)) {
                candidate++;
                codeSequenceGenerator.set(candidate);
            } else {
                break;
            }
        } while (true);
        return code;
    }

    private String generateUniqueCorrelationId() {
        String uuid;
        do {
            uuid = UUID.randomUUID().toString();
        } while (incidentReportRepository.existsByCorrelationId(uuid));
        return uuid;
    }
}
