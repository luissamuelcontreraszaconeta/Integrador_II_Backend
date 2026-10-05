package com.exportrace.service;

import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.entity.Lot;
import com.exportrace.entity.LotHistory;
import com.exportrace.entity.QaEvidence;
import com.exportrace.entity.QualityInspection;
import com.exportrace.entity.User;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.QaEvidenceRepository;
import com.exportrace.repository.QualityInspectionRepository;
import com.exportrace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QualityService {

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

    public QAInspectionDTO getInspectionByLotId(Long lotId) {
        QualityInspection qi = qualityRepository.findByLoteId(lotId).orElse(null);
        return qi != null ? new QAInspectionDTO(qi) : null;
    }

    public QualityInspection getInspectionEntityById(Long id) {
        return qualityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inspección de calidad no encontrada con ID: " + id));
    }

    @Transactional
    public QAInspectionDTO saveInspection(Long lotId, QAInspectionDTO dto, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        QualityInspection qi = qualityRepository.findByLoteId(lotId)
                .orElse(new QualityInspection());

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

        // Update lot state based on QA result
        String oldStatus = lot.getEstado();
        boolean isConforme = "CONFORME".equalsIgnoreCase(dto.getOrganolepticResult());
        String newStatus = isConforme ? "READY_FOR_CERTIFICATION" : "OBSERVED";

        lot.setEstado(newStatus);
        lot.setFechaActualizacion(LocalDateTime.now());
        lotRepository.save(lot);

        // Record history
        LotHistory history = new LotHistory(lot, oldStatus, newStatus, qi.getInspectorNombre(), "QA",
                "Inspección de Calidad Registrada: " + dto.getOrganolepticResult() + (dto.getObservations() != null ? " - " + dto.getObservations() : ""));
        lotHistoryRepository.save(history);

        // Auto-emit Real Notifications based on business events
        if (!isConforme) {
            // Quality Observed Alert for Produccion and Gerencia
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
            // Ready for Certification Alert for Logistica
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
    // Real Photo Evidence Management
    // ==========================================

    @Transactional
    public QaEvidenceDTO uploadEvidence(Long inspectionId, MultipartFile file, String description, String userEmail) {
        QualityInspection inspection = qualityRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspección de calidad no encontrada con ID: " + inspectionId));

        Lot lot = inspection.getLote();
        if (lot == null) {
            throw new RuntimeException("La inspección no tiene un lote asociado.");
        }

        // Store file physically in safe directory
        String storedPath = fileStorageService.storeFile(file, "evidence/lot_" + lot.getId());
        String fileUrl = "/uploads/" + storedPath;

        QaEvidence evidence = new QaEvidence(
                inspection,
                lot,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence.jpg",
                fileUrl,
                file.getContentType() != null ? file.getContentType() : "image/jpeg",
                file.getSize(),
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
                "Evidencia fotográfica cargada para Lote: " + lot.getCodigo() + " (Archivo: " + saved.getFileName() + ", Tamaño: " + (saved.getFileSize() / 1024) + " KB)",
                null,
                fileUrl,
                "EXITOSO",
                null
        );

        return new QaEvidenceDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<QaEvidenceDTO> getEvidencesByInspectionId(Long inspectionId) {
        List<QaEvidence> list = qaEvidenceRepository.findByInspectionIdOrderByUploadedAtDesc(inspectionId);
        return list.stream().map(QaEvidenceDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QaEvidenceDTO> getEvidencesByLotId(Long lotId) {
        List<QaEvidence> list = qaEvidenceRepository.findByLotIdOrderByUploadedAtDesc(lotId);
        return list.stream().map(QaEvidenceDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void deleteEvidence(Long inspectionId, Long evidenceId, String userEmail) {
        QaEvidence evidence = qaEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidencia no encontrada con ID: " + evidenceId));

        if (!evidence.getInspection().getId().equals(inspectionId)) {
            throw new IllegalArgumentException("La evidencia no pertenece a la inspección especificada.");
        }

        String fileName = evidence.getFileName();
        String fileUrl = evidence.getFileUrl();
        Long lotId = evidence.getLot() != null ? evidence.getLot().getId() : null;

        qaEvidenceRepository.delete(evidence);

        // Delete physical file if possible
        if (fileUrl != null && fileUrl.startsWith("/uploads/")) {
            String relativePath = fileUrl.replace("/uploads/", "");
            fileStorageService.deleteFile(relativePath, "");
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);

        // Audit Trail
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "qa@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "QA",
                "QA_EVIDENCE_DELETED",
                "QUALITY",
                "QaEvidence",
                String.valueOf(evidenceId),
                "Evidencia fotográfica eliminada (Archivo: " + fileName + ", Lote ID: " + lotId + ")",
                fileUrl,
                null,
                "EXITOSO",
                null
        );
    }
}
