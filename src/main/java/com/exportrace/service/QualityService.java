package com.exportrace.service;

import com.exportrace.dto.CreateReinspectionRequest;
import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.entity.*;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.QaEvidenceRepository;
import com.exportrace.repository.QualityInspectionRepository;
import com.exportrace.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QualityService {

    private static final Logger log = LoggerFactory.getLogger(QualityService.class);

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private QaEvidenceRepository qaEvidenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LotStateMachineService stateMachineService;

    public QAInspectionDTO getInspectionByLotId(Long lotId) {
        QualityInspection qi = qualityRepository.findByLoteId(lotId).orElse(null);
        return qi != null ? new QAInspectionDTO(qi) : null;
    }

    public List<QAInspectionDTO> getInspectionHistoryByLotId(Long lotId) {
        List<QualityInspection> list = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(lotId);
        return list.stream().map(QAInspectionDTO::new).collect(Collectors.toList());
    }

    public QualityInspection getInspectionEntityById(Long id) {
        return qualityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspección de calidad no encontrada con ID: " + id));
    }

    @Transactional
    public QAInspectionDTO saveInspection(Long lotId, QAInspectionDTO dto, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        // Bloqueo de modificación de QA histórica en lotes certificados o despachados
        LotStatus currentLotStatus = LotStatus.fromString(lot.getEstado());
        if (currentLotStatus == LotStatus.CERTIFIED ||
            currentLotStatus == LotStatus.READY_FOR_DISPATCH ||
            currentLotStatus == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: No se puede modificar la inspección QA de un lote que ya ha sido certificado o despachado (" + currentLotStatus.getEtiqueta() + ").");
        }

        List<QualityInspection> existingList = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(lotId);
        QualityInspection qi;

        if (existingList.isEmpty()) {
            qi = new QualityInspection();
            qi.setNumeroInspeccion(1);
            qi.setFechaCreacion(LocalDateTime.now());
            qi.setCreadoPor(userEmail != null ? userEmail : "qa@exportrace.pe");
        } else if (existingList.size() == 1 && (currentLotStatus == LotStatus.REGISTERED || currentLotStatus == LotStatus.UNDER_QA_INSPECTION)) {
            qi = existingList.get(0);
        } else {
            // If lot was already inspected and transitioned, further inspections must use reinspect endpoint
            qi = existingList.get(existingList.size() - 1);
        }

        qi.setLote(lot);
        qi.setFechaInspeccion(LocalDateTime.now());
        qi.setInspectorNombre(dto.getInspectorName() != null ? dto.getInspectorName() : "Dra. María Elena Quispe");
        qi.setApariencia(dto.getAppearance() != null ? dto.getAppearance() : "EXCELENTE");
        qi.setEvaluacionColor(dto.getColor() != null ? dto.getColor() : "CONFORME");
        qi.setTextura(dto.getTexture() != null ? dto.getTexture() : "FIRM");
        qi.setOlor(dto.getSmell() != null ? dto.getSmell() : "CARACTERISTICO");
        qi.setExamenParasitologico(dto.getParasiteCheck() != null ? dto.getParasiteCheck() : "AUSENCIA");
        qi.setResultadoOrganoleptico(dto.getOrganolepticResult() != null ? dto.getOrganolepticResult() : "CONFORME");
        qi.setObservaciones(dto.getObservations());

        if (dto.getEvidenceUrls() != null && !dto.getEvidenceUrls().isEmpty()) {
            qi.setEvidenciaFotosUrl(String.join(",", dto.getEvidenceUrls()));
        }

        QualityInspection saved = qualityRepository.save(qi);

        // Update lot state based on QA result via canonical state machine
        boolean isConforme = "CONFORME".equalsIgnoreCase(dto.getOrganolepticResult());
        LotStatus newStatus = isConforme ? LotStatus.READY_FOR_CERTIFICATION : LotStatus.OBSERVED;
        stateMachineService.transition(lot, newStatus, userEmail, "QA",
                "Inspección de Calidad #" + saved.getNumeroInspeccion() + " Registrada: " + dto.getOrganolepticResult() + (dto.getObservations() != null ? " - " + dto.getObservations() : ""));

        // Audit Trail
        User user = userRepository.findByEmail(userEmail).orElse(null);
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "qa@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                "QA_INSPECTION_CREATED",
                "QUALITY",
                "QualityInspection",
                String.valueOf(saved.getId()),
                "Inspección #" + saved.getNumeroInspeccion() + " registrada con resultado " + saved.getResultadoOrganoleptico() + " para lote " + lot.getCodigo(),
                null,
                "resultado: " + saved.getResultadoOrganoleptico(),
                "EXITOSO",
                null
        );

        // Auto-emit Real Notifications based on business events
        if (!isConforme) {
            notificationService.broadcastRoleNotification(
                    "PRODUCCION",
                    "Lote " + lot.getCodigo() + " Observado en Calidad",
                    "La inspección técnica sanitaria determinó estado OBSERVADO (" + dto.getOrganolepticResult() + "). Se requiere revisión correctiva en planta.",
                    "QUALITY_OBSERVED",
                    "URGENT",
                    "QUALITY",
                    "LOT",
                    lot.getCodigo(),
                    "/quality/inspect/" + lot.getId()
            );

            notificationService.broadcastRoleNotification(
                    "GERENCIA",
                    "Alerta de Calidad: Lote " + lot.getCodigo() + " No Conforme",
                    "Inspección organoléptica observada por " + qi.getInspectorNombre() + ". Revisión prioritaria requerida.",
                    "QUALITY_OBSERVED",
                    "HIGH",
                    "QUALITY",
                    "LOT",
                    lot.getCodigo(),
                    "/lots/" + lot.getId()
            );
        } else {
            notificationService.broadcastRoleNotification(
                    "LOGISTICA",
                    "Lote " + lot.getCodigo() + " Aprobado para Certificación",
                    "Inspección organoléptica CONFORME. Lote listo para tramitación de Certificado Oficial SANIPES.",
                    "CERTIFICATION_PENDING",
                    "NORMAL",
                    "CERTIFICATION",
                    "LOT",
                    lot.getCodigo(),
                    "/certification"
            );
        }

        return new QAInspectionDTO(saved);
    }

    // ==========================================
    // Reinspecciones QA (CASO 5 / BR-P1-001)
    // ==========================================

    @Transactional
    public QAInspectionDTO createReinspection(Long lotId, CreateReinspectionRequest req, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        // 1. Validar estado compatible: No se puede reinspeccionar lote CERTIFIED, READY_FOR_DISPATCH, DISPATCHED o CANCELLED
        LotStatus currentLotStatus = LotStatus.fromString(lot.getEstado());
        if (currentLotStatus == LotStatus.CERTIFIED ||
            currentLotStatus == LotStatus.READY_FOR_DISPATCH ||
            currentLotStatus == LotStatus.DISPATCHED ||
            currentLotStatus == LotStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: No se puede realizar una reinspección QA en un lote en estado '" + currentLotStatus.getEtiqueta() + "'.");
        }

        // 2. Obtener historial de inspecciones
        List<QualityInspection> inspections = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(lotId);
        if (inspections.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote no cuenta con una inspección inicial previa. Debe registrar primero la inspección #1.");
        }

        // 3. Validar límite máximo: 1 inicial + 2 reinspecciones = 3 máximo
        if (inspections.size() >= 3) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            auditService.logAction(
                    user != null ? user.getId() : null,
                    userEmail != null ? userEmail : "qa@exportrace.pe",
                    user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                    "QA_REINSPECTION_LIMIT_REACHED",
                    "QUALITY",
                    "Lot",
                    String.valueOf(lot.getId()),
                    "LÍMITE ALCANZADO: Intento de 4ta inspección en Lote " + lot.getCodigo() + ". Máximo permitido: 3 inspecciones.",
                    "totalInspecciones: 3",
                    "intento: 4",
                    "REJECTED",
                    null
            );

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "LÍMITE ALCANZADO: El lote " + lot.getCodigo() + " ha alcanzado el límite máximo permitido de 3 inspecciones (1 inicial + 2 reinspecciones). No se permiten más reinspecciones.");
        }

        // 4. Validar que la última inspección permita reinspección (no debe ser ya CONFORME)
        QualityInspection lastInspection = inspections.get(inspections.size() - 1);
        if ("CONFORME".equalsIgnoreCase(lastInspection.getResultadoOrganoleptico())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El lote ya cuenta con un dictamen CONFORME en su última inspección (#" + lastInspection.getNumeroInspeccion() + "). No requiere reinspección.");
        }

        // 5. Validar motivo obligatorio
        if (req.getMotivoReinspeccion() == null || req.getMotivoReinspeccion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo de la reinspección es obligatorio.");
        }

        // 6. Crear nueva entidad de inspección (NUEVA FILA, NUNCA SOBRESCRIBIR LA ANTERIOR)
        int nextSequence = inspections.size() + 1;
        QualityInspection newQi = new QualityInspection();
        newQi.setLote(lot);
        newQi.setNumeroInspeccion(nextSequence);
        newQi.setMotivoReinspeccion(req.getMotivoReinspeccion().trim());
        newQi.setInspectorNombre(req.getInspectorName() != null ? req.getInspectorName() : "Dra. María Elena Quispe");
        newQi.setFechaInspeccion(LocalDateTime.now());
        newQi.setFechaCreacion(LocalDateTime.now());
        newQi.setCreadoPor(userEmail != null ? userEmail : "qa@exportrace.pe");
        newQi.setApariencia(req.getAppearance() != null ? req.getAppearance() : "EXCELENTE");
        newQi.setEvaluacionColor(req.getColor() != null ? req.getColor() : "CONFORME");
        newQi.setTextura(req.getTexture() != null ? req.getTexture() : "FIRM");
        newQi.setOlor(req.getSmell() != null ? req.getSmell() : "CARACTERISTICO");
        newQi.setExamenParasitologico(req.getParasiteCheck() != null ? req.getParasiteCheck() : "AUSENCIA");
        newQi.setResultadoOrganoleptico(req.getResultadoOrganoleptico() != null ? req.getResultadoOrganoleptico() : "CONFORME");
        newQi.setObservaciones(req.getObservations());

        QualityInspection saved = qualityRepository.save(newQi);

        // 7. Transición de estado en la máquina de estados canónica
        boolean isConforme = "CONFORME".equalsIgnoreCase(saved.getResultadoOrganoleptico());
        LotStatus targetStatus = isConforme ? LotStatus.READY_FOR_CERTIFICATION : LotStatus.OBSERVED;

        stateMachineService.transition(lot, targetStatus, userEmail, "QA",
                "Reinspección QA #" + nextSequence + " Registrada (" + saved.getResultadoOrganoleptico() + "). Motivo: " + saved.getMotivoReinspeccion());

        // 8. Registro de Auditoría
        User user = userRepository.findByEmail(userEmail).orElse(null);
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "qa@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                "QA_REINSPECTION_CREATED",
                "QUALITY",
                "QualityInspection",
                String.valueOf(saved.getId()),
                "Reinspección #" + nextSequence + " registrada con resultado " + saved.getResultadoOrganoleptico() + " para lote " + lot.getCodigo() + ". Motivo: " + saved.getMotivoReinspeccion(),
                "inspeccionAnterior: #" + lastInspection.getNumeroInspeccion() + " (" + lastInspection.getResultadoOrganoleptico() + ")",
                "nuevaInspeccion: #" + nextSequence + " (" + saved.getResultadoOrganoleptico() + ")",
                "EXITOSO",
                null
        );

        // 9. Notificación de evento
        if (isConforme) {
            notificationService.broadcastRoleNotification(
                    "LOGISTICA",
                    "Lote " + lot.getCodigo() + " Subsanado y Conforme tras Reinspección #" + nextSequence,
                    "La reinspección #" + nextSequence + " resultó CONFORME. Lote habilitado para certificación.",
                    "QUALITY_REINSPECTED_OK",
                    "NORMAL",
                    "QUALITY",
                    "LOT",
                    lot.getCodigo(),
                    "/lots/" + lot.getId()
            );
        } else {
            notificationService.broadcastRoleNotification(
                    "PRODUCCION",
                    "Lote " + lot.getCodigo() + " No Conforme tras Reinspección #" + nextSequence,
                    "La reinspección #" + nextSequence + " concluyó como " + saved.getResultadoOrganoleptico() + ". Motivo evaluado: " + saved.getMotivoReinspeccion(),
                    "QUALITY_REINSPECTED_OBSERVED",
                    "URGENT",
                    "QUALITY",
                    "LOT",
                    lot.getCodigo(),
                    "/lots/" + lot.getId()
            );
        }

        return new QAInspectionDTO(saved);
    }

    // ==========================================
    // Real Photo Evidence Management (P0-C)
    // ==========================================

    @Transactional
    public QaEvidenceDTO uploadEvidence(Long inspectionId, MultipartFile file, String description, String userEmail) {
        QualityInspection inspection = qualityRepository.findById(inspectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspección de calidad no encontrada con ID: " + inspectionId));

        Lot lot = inspection.getLote();
        if (lot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La inspección no tiene un lote asociado.");
        }

        // Inmutabilidad de evidencias post-certificación o despacho
        LotStatus currentLotStatus = LotStatus.fromString(lot.getEstado());
        if (currentLotStatus == LotStatus.CERTIFIED ||
            currentLotStatus == LotStatus.READY_FOR_DISPATCH ||
            currentLotStatus == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0-C: No se pueden adjuntar nuevas evidencias fotográficas a un lote certificado o despachado (" + currentLotStatus.getEtiqueta() + ").");
        }

        // Store file physically in safe directory with SHA-256 and magic bytes validation
        FileStorageService.StoredFile stored = fileStorageService.storeEvidence(file, "evidence/lot_" + lot.getId());

        QaEvidence evidence = new QaEvidence(
                inspection,
                lot,
                stored.originalFileName(),
                stored.storedFileName(),
                stored.relativeStoragePath(),
                stored.mimeType(),
                stored.fileSize(),
                stored.sha256(),
                description != null ? description : "Evidencia fotográfica organoléptica",
                userEmail != null ? userEmail : "qa@exportrace.pe"
        );

        QaEvidence saved = qaEvidenceRepository.save(evidence);

        User user = userRepository.findByEmail(userEmail).orElse(null);

        // Audit Trail
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "qa@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                "QA_EVIDENCE_UPLOADED",
                "QUALITY",
                "QaEvidence",
                String.valueOf(saved.getId()),
                "Evidencia fotográfica subida para inspección #" + inspection.getNumeroInspeccion() + " del Lote " + lot.getCodigo() + ": " + stored.originalFileName() + " (SHA-256: " + stored.sha256() + ")",
                null,
                "sha256: " + stored.sha256(),
                "EXITOSO",
                null
        );

        return new QaEvidenceDTO(saved);
    }

    public List<QaEvidenceDTO> getEvidencesByInspectionId(Long inspectionId) {
        return qaEvidenceRepository.findByInspectionIdAndActiveTrueOrderByUploadedAtDesc(inspectionId)
                .stream()
                .map(QaEvidenceDTO::new)
                .collect(Collectors.toList());
    }

    public List<QaEvidenceDTO> getEvidencesByLotId(Long lotId) {
        return qaEvidenceRepository.findByLotIdAndActiveTrueOrderByUploadedAtDesc(lotId)
                .stream()
                .map(QaEvidenceDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvidence(Long inspectionId, Long evidenceId, String userEmail) {
        QaEvidence evidence = qaEvidenceRepository.findByIdAndActiveTrue(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidencia fotográfica no encontrada con ID: " + evidenceId));

        if (!evidence.getInspection().getId().equals(inspectionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La evidencia no corresponde a la inspección indicada.");
        }

        Lot lot = evidence.getLot();
        LotStatus currentLotStatus = LotStatus.fromString(lot.getEstado());
        if (currentLotStatus == LotStatus.CERTIFIED ||
            currentLotStatus == LotStatus.READY_FOR_DISPATCH ||
            currentLotStatus == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0-C: No se pueden eliminar evidencias en un lote certificado o despachado (" + currentLotStatus.getEtiqueta() + ").");
        }

        evidence.setActive(false);
        qaEvidenceRepository.save(evidence);

        User user = userRepository.findByEmail(userEmail).orElse(null);
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "qa@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                "QA_EVIDENCE_DEACTIVATED",
                "QUALITY",
                "QaEvidence",
                String.valueOf(evidenceId),
                "Evidencia fotográfica desactivada (Soft-Delete): " + evidence.getOriginalFileName(),
                "active: true",
                "active: false",
                "EXITOSO",
                null
        );
    }

    public Resource loadEvidenceResource(Long evidenceId, boolean verifyIntegrity) {
        QaEvidence evidence = qaEvidenceRepository.findByIdAndActiveTrue(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidencia no encontrada con ID: " + evidenceId));

        Resource resource = fileStorageService.loadFileAsResource(evidence.getStoragePath());

        if (verifyIntegrity && evidence.getSha256() != null) {
            try (InputStream is = resource.getInputStream()) {
                String currentHash = fileStorageService.calculateSha256(is);
                if (!evidence.getSha256().equalsIgnoreCase(currentHash)) {
                    log.error("[QualityService] Tamper detected on evidence {}! Expected SHA256={}, got={}",
                            evidenceId, evidence.getSha256(), currentHash);

                    auditService.logAction(
                            null,
                            "SYSTEM",
                            "SECURITY",
                            "QA_EVIDENCE_INTEGRITY_ERROR",
                            "SECURITY",
                            "QaEvidence",
                            String.valueOf(evidenceId),
                            "Error de integridad: el archivo ha sido modificado externamente (hash SHA-256 no coincide).",
                            "expected: " + evidence.getSha256(),
                            "actual: " + currentHash,
                            "FALLIDO",
                            null
                    );

                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Error de integridad: el archivo ha sido modificado externamente (hash SHA-256 no coincide).");
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                log.error("[QualityService] Error verifying evidence integrity: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error verificando la integridad del archivo.");
            }
        }

        return resource;
    }

    public QaEvidence getEvidenceEntity(Long evidenceId) {
        return qaEvidenceRepository.findByIdAndActiveTrue(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidencia no encontrada con ID: " + evidenceId));
    }
}
