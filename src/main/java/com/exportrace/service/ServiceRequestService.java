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
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para Solicitudes de Servicio del Catálogo ExporTrace (6 servicios completos).
 *
 * Garantiza Store-and-Forward:
 * 1. Primero guarda la solicitud localmente en SQLite dentro de una transacción corta.
 * 2. Después intenta sincronizar con Jira Service Management fuera de la transacción de creación.
 */
@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    public static final String SR_INF_01_CODE = "SR-INF-01";
    public static final String SR_INF_02_CODE = "SR-INF-02";
    public static final String SR_INF_03_CODE = "SR-INF-03";
    public static final String SR_INF_04_CODE = "SR-INF-04";
    public static final String SR_ACC_01_CODE = "SR-ACC-01";
    public static final String SR_ACC_02_CODE = "SR-ACC-02";

    public static final String CATEGORY_INFORMATION_KEY = "INFORMATION";
    public static final String CATEGORY_INFORMATION_LABEL = "Solicitudes de información";
    public static final String CATEGORY_ACCESS_KEY = "ACCESS";
    public static final String CATEGORY_ACCESS_LABEL = "Solicitudes de acceso";

    public static final String SR_INF_01_TITLE = "Consultar el seguimiento completo de un lote";
    public static final String SR_INF_01_CATEGORY_KEY = CATEGORY_INFORMATION_KEY;
    public static final String SR_INF_01_CATEGORY_LABEL = CATEGORY_INFORMATION_LABEL;
    public static final String SR_INF_01_SLA_TARGET = "4 horas hábiles";

    private static final Set<String> ATTACHMENT_ALLOWED_SERVICES = Set.of(
            SR_INF_03_CODE, SR_INF_04_CODE, SR_ACC_01_CODE, SR_ACC_02_CODE
    );

    public record CatalogMetadata(
            String serviceCode,
            String title,
            String categoryKey,
            String categoryLabel,
            String slaTarget,
            boolean requiresApproval
    ) {}

    private static final Map<String, CatalogMetadata> CATALOG_METADATA = Map.of(
            SR_INF_01_CODE, new CatalogMetadata(
                    SR_INF_01_CODE,
                    "Consultar el seguimiento completo de un lote",
                    CATEGORY_INFORMATION_KEY,
                    CATEGORY_INFORMATION_LABEL,
                    "4 horas hábiles",
                    false
            ),
            SR_INF_02_CODE, new CatalogMetadata(
                    SR_INF_02_CODE,
                    "Consultar los documentos y el expediente de un lote",
                    CATEGORY_INFORMATION_KEY,
                    CATEGORY_INFORMATION_LABEL,
                    "4 horas hábiles",
                    false
            ),
            SR_INF_03_CODE, new CatalogMetadata(
                    SR_INF_03_CODE,
                    "Reportar problema en la información de un lote",
                    CATEGORY_INFORMATION_KEY,
                    CATEGORY_INFORMATION_LABEL,
                    "4 horas hábiles para resultado inicial",
                    false
            ),
            SR_INF_04_CODE, new CatalogMetadata(
                    SR_INF_04_CODE,
                    "Solicitar corrección de información de un lote",
                    CATEGORY_INFORMATION_KEY,
                    CATEGORY_INFORMATION_LABEL,
                    "1 día hábil desde aprobación",
                    true
            ),
            SR_ACC_01_CODE, new CatalogMetadata(
                    SR_ACC_01_CODE,
                    "Solicitar la creación o activación de una cuenta",
                    CATEGORY_ACCESS_KEY,
                    CATEGORY_ACCESS_LABEL,
                    "1 día hábil desde aprobación",
                    true
            ),
            SR_ACC_02_CODE, new CatalogMetadata(
                    SR_ACC_02_CODE,
                    "Solicitar cambio o retiro de permisos de acceso",
                    CATEGORY_ACCESS_KEY,
                    CATEGORY_ACCESS_LABEL,
                    "1 día hábil desde aprobación",
                    true
            )
    );

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

    @Autowired
    private FileStorageService fileStorageService;

    public ServiceRequestDTO createServiceRequest(
            CreateServiceRequestDTO req,
            String userEmail,
            HttpServletRequest httpRequest) {
        return createServiceRequest(req, null, userEmail, httpRequest);
    }

    public ServiceRequestDTO createServiceRequest(
            CreateServiceRequestDTO req,
            MultipartFile attachmentFile,
            String userEmail,
            HttpServletRequest httpRequest) {

        FileStorageService.StoredFile storedAttachment = null;
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            String normalizedCode = req != null && req.getServiceCode() != null
                    ? req.getServiceCode().trim().toUpperCase()
                    : "";
            if (!ATTACHMENT_ALLOWED_SERVICES.contains(normalizedCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El servicio " + normalizedCode + " no admite archivos adjuntos.");
            }
            storedAttachment = storeServiceRequestAttachment(attachmentFile);
        }

        final FileStorageService.StoredFile finalStoredAttachment = storedAttachment;
        LocalCreationContext localContext = transactionTemplate.execute(status ->
                saveLocalServiceRequestInTransaction(req, finalStoredAttachment, userEmail, httpRequest));

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
                    .map(serviceRequestSyncService::buildDtoWithComments)
                    .orElse(localContext.dto);
        }
    }

    private LocalCreationContext saveLocalServiceRequestInTransaction(
            CreateServiceRequestDTO req,
            FileStorageService.StoredFile storedAttachment,
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
        CatalogMetadata metadata = CATALOG_METADATA.get(serviceCode);
        if (metadata == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Código de servicio no válido o no soportado en el catálogo: '" + serviceCode + "'.");
        }

        validateAndNormalizeFieldsForService(serviceCode, req);

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
        String requestTypeId = jiraServiceRequestService.resolveRequestTypeId(serviceCode);

        ServiceRequest saved = null;
        int maxRetries = 5;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String correlationId = generateUniqueCorrelationId();
            String code = generateUniqueServiceRequestCode();

            ServiceRequest sr = new ServiceRequest();
            sr.setCode(code);
            sr.setServiceCode(serviceCode);
            sr.setServiceTitle(metadata.title());
            sr.setCategoryKey(metadata.categoryKey());
            sr.setCategoryLabel(metadata.categoryLabel());
            sr.setCorrelationId(correlationId);
            sr.setRequester(requester);
            sr.setRequesterUsername(fullName);
            sr.setRequesterEmail(requester.getEmail());
            sr.setRequesterRole(roleName);
            sr.setSummary(req.getSummary().trim());

            sr.setLotCode(trimOrDefault(req.getLotCode(), ""));
            sr.setRequestedInformation(trimOrDefault(req.getRequestedInformation(), ""));
            sr.setAdditionalDetail(trimOrNull(req.getAdditionalDetail()));

            sr.setDocumentQueryType(trimOrNull(req.getDocumentQueryType()));
            sr.setObservedField(trimOrNull(req.getObservedField()));
            sr.setProblemDescription(trimOrNull(req.getProblemDescription()));
            sr.setFieldToCorrect(trimOrNull(req.getFieldToCorrect()));
            sr.setCurrentValue(trimOrNull(req.getCurrentValue()));
            sr.setProposedValue(trimOrNull(req.getProposedValue()));
            sr.setJustification(trimOrNull(req.getJustification()));
            sr.setWorkerName(trimOrNull(req.getWorkerName()));
            sr.setAffectedUser(trimOrNull(req.getAffectedUser()));
            sr.setInstitutionalEmail(trimOrNull(req.getInstitutionalEmail()));
            sr.setArea(trimOrNull(req.getArea()));
            sr.setRequiredRole(trimOrNull(req.getRequiredRole()));
            sr.setAccessRequestType(trimOrNull(req.getAccessRequestType()));
            sr.setCurrentRoleField(trimOrNull(req.getCurrentRoleField()));
            sr.setRequiredPermissions(trimOrNull(req.getRequiredPermissions()));
            sr.setReason(trimOrNull(req.getReason()));

            sr.setRequiresApproval(metadata.requiresApproval());
            sr.setApprovalStatus(metadata.requiresApproval() ? "PENDING_APPROVAL" : "NOT_REQUIRED");

            if (storedAttachment != null) {
                sr.setAttachmentOriginalName(storedAttachment.originalFileName());
                sr.setAttachmentStoredPath(storedAttachment.relativeStoragePath());
                sr.setAttachmentMimeType(storedAttachment.mimeType());
                sr.setAttachmentSize(storedAttachment.fileSize());
                sr.setAttachmentSha256(storedAttachment.sha256());
                sr.setJiraAttachmentSynced(false);
            }

            sr.setStatus(ServiceRequestStatus.REGISTERED);
            sr.setCreatedAt(now);
            sr.setUpdatedAt(now);
            sr.setSlaTarget(metadata.slaTarget());
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
                "code=%s, serviceCode=%s, lotCode=%s, status=%s, syncStatus=%s, slaTarget=%s, requiresApproval=%s",
                saved.getCode(),
                saved.getServiceCode(),
                saved.getLotCode(),
                saved.getStatus().name(),
                saved.getSyncStatus().name(),
                saved.getSlaTarget(),
                saved.getRequiresApproval()
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

    private void validateAndNormalizeFieldsForService(String serviceCode, CreateServiceRequestDTO req) {
        requireNonBlank(req.getSummary(), "El resumen de la solicitud es obligatorio.");

        try {
            switch (serviceCode) {
                case SR_INF_01_CODE -> {
                    requireNonBlank(req.getLotCode(), "El código del lote es obligatorio.");
                    requireNonBlank(req.getRequestedInformation(), "La información requerida es obligatoria.");
                }
                case SR_INF_02_CODE -> {
                    requireNonBlank(req.getLotCode(), "El código del lote es obligatorio.");
                    requireNonBlank(req.getDocumentQueryType(), "El tipo de consulta documental es obligatorio.");
                    jiraServiceRequestService.resolveDocumentQueryTypeOptionId(req.getDocumentQueryType(), null);
                }
                case SR_INF_03_CODE -> {
                    requireNonBlank(req.getLotCode(), "El código del lote es obligatorio.");
                    requireNonBlank(req.getObservedField(), "El dato observado es obligatorio.");
                    requireNonBlank(req.getProblemDescription(), "La descripción del problema es obligatoria.");
                }
                case SR_INF_04_CODE -> {
                    requireNonBlank(req.getLotCode(), "El código del lote es obligatorio.");
                    requireNonBlank(req.getFieldToCorrect(), "El dato a corregir es obligatorio.");
                    requireNonBlank(req.getCurrentValue(), "La información actual es obligatoria.");
                    requireNonBlank(req.getProposedValue(), "La información propuesta es obligatoria.");
                    requireNonBlank(req.getJustification(), "La justificación es obligatoria.");
                }
                case SR_ACC_01_CODE -> {
                    requireNonBlank(req.getWorkerName(), "El nombre del trabajador es obligatorio.");
                    requireNonBlank(req.getInstitutionalEmail(), "El correo institucional es obligatorio.");
                    requireNonBlank(req.getArea(), "El área es obligatoria.");
                    requireNonBlank(req.getRequiredRole(), "El rol requerido es obligatorio.");
                    requireNonBlank(req.getJustification(), "La justificación es obligatoria.");
                    jiraServiceRequestService.resolveAreaOptionId(req.getArea(), null);
                    jiraServiceRequestService.resolveRole10257OptionId(req.getRequiredRole(), null);
                }
                case SR_ACC_02_CODE -> {
                    requireNonBlank(req.getAffectedUser(), "El usuario afectado es obligatorio.");
                    requireNonBlank(req.getInstitutionalEmail(), "El correo institucional es obligatorio.");
                    requireNonBlank(req.getArea(), "El área es obligatoria.");
                    requireNonBlank(req.getAccessRequestType(), "El tipo de solicitud es obligatorio.");
                    requireNonBlank(req.getCurrentRoleField(), "El rol actual es obligatorio.");
                    requireNonBlank(req.getRequiredPermissions(), "El rol/permisos requeridos es obligatorio.");
                    requireNonBlank(req.getReason(), "El motivo es obligatorio.");
                    jiraServiceRequestService.resolveAreaOptionId(req.getArea(), null);
                    jiraServiceRequestService.resolveAccessRequestTypeOptionId(req.getAccessRequestType(), null);
                    jiraServiceRequestService.resolveRole10257OptionId(req.getCurrentRoleField(), null);
                    jiraServiceRequestService.resolvePermissions10262OptionId(req.getRequiredPermissions(), null);
                }
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Código de servicio no soportado: " + serviceCode);
            }
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String trimOrNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    private String trimOrDefault(String value, String fallback) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : fallback;
    }

    public ServiceRequestDTO uploadAttachment(
            Long id,
            MultipartFile file,
            String userEmail,
            HttpServletRequest httpRequest) {

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
            throw new AccessDeniedException("Acceso denegado: No tiene autorización para adjuntar archivos a esta solicitud.");
        }

        if (!ATTACHMENT_ALLOWED_SERVICES.contains(sr.getServiceCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El servicio " + sr.getServiceCode() + " no admite archivos adjuntos.");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar un archivo válido.");
        }

        FileStorageService.StoredFile stored = storeServiceRequestAttachment(file);

        ServiceRequest updated = transactionTemplate.execute(status -> {
            ServiceRequest entity = serviceRequestRepository.findById(id).orElseThrow();
            entity.setAttachmentOriginalName(stored.originalFileName());
            entity.setAttachmentStoredPath(stored.relativeStoragePath());
            entity.setAttachmentMimeType(stored.mimeType());
            entity.setAttachmentSize(stored.fileSize());
            entity.setAttachmentSha256(stored.sha256());
            entity.setJiraAttachmentSynced(false);
            entity.setUpdatedAt(LocalDateTime.now());
            return serviceRequestRepository.saveAndFlush(entity);
        });

        if (jiraProperties.isEnabled() && updated != null && updated.getJiraIssueKey() != null && !updated.getJiraIssueKey().isBlank()) {
            try {
                Resource attachmentResource = fileStorageService.loadFileAsResource(updated.getAttachmentStoredPath());
                boolean uploaded = jiraServiceRequestService.uploadAttachmentToJiraIssue(
                        updated.getJiraIssueKey(),
                        attachmentResource,
                        updated.getAttachmentOriginalName()
                );
                if (uploaded) {
                    transactionTemplate.executeWithoutResult(status -> {
                        ServiceRequest entity = serviceRequestRepository.findById(id).orElse(null);
                        if (entity != null) {
                            entity.setJiraAttachmentSynced(true);
                            serviceRequestRepository.saveAndFlush(entity);
                        }
                    });
                }
            } catch (Exception ex) {
                log.warn("[ServiceRequestService] Adjunto local guardado para {}, pendiente de envío a Jira ({}).",
                        updated.getCode(), ex.getMessage());
            }
        }

        ServiceRequest refreshed = serviceRequestRepository.findById(id).orElse(updated);
        return serviceRequestSyncService.buildDtoWithComments(refreshed);
    }

    public record AttachmentDownload(Resource resource, String originalName, String mimeType) {}

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(Long id, String userEmail, HttpServletRequest httpRequest) {
        ServiceRequestDTO dto = getServiceRequestById(id, userEmail, httpRequest);
        ServiceRequest sr = serviceRequestRepository.findById(dto.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));

        if (sr.getAttachmentStoredPath() == null || sr.getAttachmentStoredPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La solicitud no posee archivo adjunto.");
        }

        Resource resource = fileStorageService.loadFileAsResource(sr.getAttachmentStoredPath());
        String mimeType = sr.getAttachmentMimeType() != null ? sr.getAttachmentMimeType() : "application/octet-stream";
        String originalName = sr.getAttachmentOriginalName() != null ? sr.getAttachmentOriginalName() : "adjunto";
        return new AttachmentDownload(resource, originalName, mimeType);
    }

    private FileStorageService.StoredFile storeServiceRequestAttachment(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (contentType.contains("pdf") || originalFilename.endsWith(".pdf")) {
            return fileStorageService.storeDocument(file, "service-requests");
        }
        return fileStorageService.storeEvidence(file, "service-requests");
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
                .map(serviceRequestSyncService::buildDtoWithComments)
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

        return serviceRequestSyncService.buildDtoWithComments(sr);
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
                .map(serviceRequestSyncService::buildDtoWithComments)
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
