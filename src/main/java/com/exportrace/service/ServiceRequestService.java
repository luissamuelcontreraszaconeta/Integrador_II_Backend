package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.CreateServiceRequestDTO;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.entity.enums.ServiceRequestStatus;
import com.exportrace.repository.ServiceRequestRepository;
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

/**
 * Servicio de negocio para Solicitudes de Servicio del Catálogo ExporTrace (Piloto SR-INF-01).
 *
 * Garantiza Store-and-Forward:
 * 1. Primero guarda la solicitud localmente en SQLite dentro de una transacción corta.
 * 2. Después intenta sincronizar con Jira Service Management fuera de la transacción de creación.
 */
@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    public static final String SR_INF_01_CODE = "SR-INF-01";
    public static final String SR_INF_01_TITLE = "Consultar el seguimiento completo de un lote";
    public static final String SR_INF_01_CATEGORY_KEY = "INFORMATION";
    public static final String SR_INF_01_CATEGORY_LABEL = "Solicitudes de información";
    public static final String SR_INF_01_SLA_TARGET = "4 horas hábiles";

    private final AtomicLong codeSequenceGenerator = new AtomicLong(0);

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraServiceRequestService jiraServiceRequestService;

    @Autowired
    private ServiceRequestSyncService serviceRequestSyncService;

    public ServiceRequestDTO createServiceRequest(
            CreateServiceRequestDTO req,
            String userEmail,
            HttpServletRequest httpRequest) {

        LocalCreationContext localContext = transactionTemplate.execute(status ->
                saveLocalServiceRequestInTransaction(req, userEmail, httpRequest));

        if (localContext == null || localContext.dto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible completar el registro local de la solicitud de servicio.");
        }

        if (!jiraProperties.isEnabled()) {
            return localContext.dto;
        }

        try {
            return serviceRequestSyncService.syncServiceRequest(
                    localContext.dto.getId(),
                    localContext.requesterId,
                    localContext.requesterFullName,
                    localContext.requesterRole,
                    httpRequest
            );
        } catch (Exception ex) {
            log.warn("[ServiceRequestService] La solicitud local {} fue preservada tras una excepción durante la sincronización con Jira.",
                    localContext.dto.getCode());
            return serviceRequestRepository.findById(localContext.dto.getId())
                    .map(ServiceRequestDTO::new)
                    .orElse(localContext.dto);
        }
    }

    private LocalCreationContext saveLocalServiceRequestInTransaction(
            CreateServiceRequestDTO req,
            String userEmail,
            HttpServletRequest httpRequest) {

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Autenticación requerida para registrar una solicitud de servicio.");
        }

        User requester = userRepository.findByEmail(userEmail.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado."));

        if (!Boolean.TRUE.equals(requester.getActivo())) {
            throw new AccessDeniedException(
                    "La cuenta de usuario se encuentra desactivada. No puede registrar solicitudes de servicio.");
        }

        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es obligatorio.");
        }

        String serviceCode = req.getServiceCode() != null ? req.getServiceCode().trim().toUpperCase() : "";
        if (!SR_INF_01_CODE.equals(serviceCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "En este piloto solo se encuentra habilitado el servicio SR-INF-01 (Consultar el seguimiento completo de un lote).");
        }

        if (req.getSummary() == null || req.getSummary().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El resumen de la solicitud es obligatorio.");
        }
        if (req.getLotCode() == null || req.getLotCode().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código del lote es obligatorio.");
        }
        if (req.getRequestedInformation() == null || req.getRequestedInformation().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La información requerida es obligatoria.");
        }

        String fullName = requester.getNombre() != null ? requester.getNombre().trim() : "";
        if (requester.getApellido() != null && !requester.getApellido().isBlank()) {
            fullName = (fullName + " " + requester.getApellido().trim()).trim();
        }
        if (fullName.isEmpty()) {
            fullName = requester.getEmail();
        }
        String roleName = requester.getRole() != null && requester.getRole().getNombre() != null
                ? requester.getRole().getNombre()
                : "USUARIO";

        LocalDateTime now = LocalDateTime.now();
        String requestTypeId = jiraServiceRequestService.resolveSrInf01RequestTypeId();

        ServiceRequest saved = null;
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String correlationId = generateUniqueCorrelationId();
            String code = generateUniqueServiceRequestCode();

            ServiceRequest sr = new ServiceRequest();
            sr.setCode(code);
            sr.setServiceCode(SR_INF_01_CODE);
            sr.setServiceTitle(SR_INF_01_TITLE);
            sr.setCategoryKey(SR_INF_01_CATEGORY_KEY);
            sr.setCategoryLabel(SR_INF_01_CATEGORY_LABEL);
            sr.setCorrelationId(correlationId);
            sr.setRequester(requester);
            sr.setRequesterUsername(fullName);
            sr.setRequesterEmail(requester.getEmail());
            sr.setRequesterRole(roleName);
            sr.setSummary(req.getSummary().trim());
            sr.setLotCode(req.getLotCode().trim());
            sr.setRequestedInformation(req.getRequestedInformation().trim());
            sr.setAdditionalDetail(req.getAdditionalDetail() != null ? req.getAdditionalDetail().trim() : null);
            sr.setStatus(ServiceRequestStatus.REGISTERED);
            sr.setCreatedAt(now);
            sr.setUpdatedAt(now);
            sr.setSlaTarget(SR_INF_01_SLA_TARGET);
            sr.setSlaAuthority("LOCAL_ESTIMATE");
            sr.setJiraRequestTypeId(requestTypeId);
            sr.setSyncStatus(JiraSyncStatus.PENDING);
            sr.setSyncAttempts(0);

            try {
                saved = serviceRequestRepository.saveAndFlush(sr);
                break;
            } catch (DataIntegrityViolationException dive) {
                log.warn("[ServiceRequestService] Collision detected on service request code '{}' (attempt {}/{}). Retrying...",
                        code, attempt, maxRetries);
                if (attempt == maxRetries) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Conflicto de concurrencia al generar el código único de solicitud. Por favor intente nuevamente.");
                }
            }
        }

        String auditMetadata = String.format(
                "code=%s, serviceCode=%s, lotCode=%s, status=%s, syncStatus=%s, slaTarget=%s",
                saved.getCode(),
                saved.getServiceCode(),
                saved.getLotCode(),
                saved.getStatus().name(),
                saved.getSyncStatus().name(),
                saved.getSlaTarget()
        );

        auditService.logAction(
                requester.getId(),
                fullName,
                roleName,
                "SERVICE_REQUEST_CREATED",
                "SOPORTE",
                "ServiceRequest",
                String.valueOf(saved.getId()),
                "Solicitud de servicio registrada localmente: " + saved.getCode() + " [" + saved.getServiceCode() + "]",
                null,
                auditMetadata,
                "EXITOSO",
                httpRequest
        );

        notificationService.createNotification(
                requester,
                null,
                "Solicitud " + saved.getCode() + " registrada",
                "Tu solicitud " + saved.getCode() + " (" + saved.getServiceTitle() + ") ha sido registrada con SLA objetivo de " + saved.getSlaTarget() + ".",
                "INFO",
                "NORMAL",
                "SOPORTE",
                "ServiceRequest",
                saved.getCode(),
                "/support/requests/" + saved.getId()
        );

        return new LocalCreationContext(new ServiceRequestDTO(saved), requester.getId(), fullName, roleName);
    }

    private static class LocalCreationContext {
        final ServiceRequestDTO dto;
        final Long requesterId;
        final String requesterFullName;
        final String requesterRole;

        LocalCreationContext(ServiceRequestDTO dto, Long requesterId, String requesterFullName, String requesterRole) {
            this.dto = dto;
            this.requesterId = requesterId;
            this.requesterFullName = requesterFullName;
            this.requesterRole = requesterRole;
        }
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getMyServiceRequests(String userEmail) {
        User user = getActiveAuthenticatedUser(userEmail);
        return serviceRequestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ServiceRequestDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(noRollbackFor = AccessDeniedException.class)
    public ServiceRequestDTO getServiceRequestById(Long id, String userEmail, HttpServletRequest httpRequest) {
        User currentUser = getActiveAuthenticatedUser(userEmail);
        ServiceRequest sr = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Solicitud de servicio no encontrada con ID: " + id));

        String roleName = currentUser.getRole() != null && currentUser.getRole().getNombre() != null
                ? currentUser.getRole().getNombre()
                : "USUARIO";

        boolean isAdminOrSuperAdmin = "ADMINISTRADOR".equalsIgnoreCase(roleName) || "SUPERADMIN".equalsIgnoreCase(roleName);
        boolean isOwner = sr.getRequester() != null
                && sr.getRequester().getId() != null
                && sr.getRequester().getId().equals(currentUser.getId());

        if (!isOwner && !isAdminOrSuperAdmin) {
            String fullName = currentUser.getNombre() != null ? currentUser.getNombre() : currentUser.getEmail();
            auditService.logAction(
                    currentUser.getId(),
                    fullName,
                    roleName,
                    "SERVICE_REQUEST_VIEW_DENIED",
                    "SOPORTE",
                    "ServiceRequest",
                    String.valueOf(sr.getId()),
                    "Acceso denegado al consultar solicitud de servicio ajena: " + sr.getCode(),
                    null,
                    "code=" + sr.getCode() + ", serviceCode=" + sr.getServiceCode(),
                    "DENEGADO",
                    httpRequest
            );
            throw new AccessDeniedException(
                    "Acceso denegado: No tiene autorización para consultar solicitudes registradas por otro usuario.");
        }

        return new ServiceRequestDTO(sr);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDTO> getAllServiceRequestsForAdmin(String userEmail) {
        User currentUser = getActiveAuthenticatedUser(userEmail);
        String roleName = currentUser.getRole() != null && currentUser.getRole().getNombre() != null
                ? currentUser.getRole().getNombre()
                : "USUARIO";

        if (!"ADMINISTRADOR".equalsIgnoreCase(roleName) && !"SUPERADMIN".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("Acceso denegado: Se requiere rol ADMINISTRADOR o SUPERADMIN.");
        }

        return serviceRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ServiceRequestDTO::new)
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

    private synchronized String generateUniqueServiceRequestCode() {
        long baseCount = serviceRequestRepository.count();
        long candidate = Math.max(baseCount + 1, codeSequenceGenerator.incrementAndGet());
        String code;
        do {
            code = String.format("SR-2026-%06d", candidate);
            if (serviceRequestRepository.existsByCode(code)) {
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
        } while (serviceRequestRepository.existsByCorrelationId(uuid));
        return uuid;
    }
}
