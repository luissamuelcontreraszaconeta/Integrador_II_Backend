package com.exportrace.service;

import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LotStateMachineService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private QualityInspectionRepository qualityInspectionRepository;

    @Autowired
    private SanitaryCertificationRepository sanitaryCertificationRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRecordRepository;

    @Autowired
    private ColdChainIncidentRepository coldChainIncidentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${coldchain.max-reading-age-hours:12}")
    private int maxReadingAgeHours = 12;

    /**
     * Valida y ejecuta la transición de estado de un lote según las reglas canónicas T-01 a T-08.
     */
    public Lot transition(Lot lot, LotStatus targetStatus, String userEmail, String userRole, String reason) {
        LotStatus currentStatus = LotStatus.fromString(lot.getEstado());

        if (currentStatus == targetStatus) {
            return lot; // Idempotente
        }

        // Caso 11: Estado terminal inmutable DISPATCHED no puede revertirse a REGISTERED o Producción
        if (currentStatus == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " ya ha sido DESPACHADO (estado terminal inmutable). No puede volver a etapas previas.");
        }
        if (currentStatus == LotStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " se encuentra ANULADO y no admite transiciones.");
        }
        if (currentStatus == LotStatus.REJECTED && targetStatus != LotStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " fue RECHAZADO por no conformidad técnica.");
        }

        switch (targetStatus) {
            case REGISTERED:
                if (currentStatus != LotStatus.REGISTERED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "BLOQUEO P0: No se permite revertir un lote en curso (" + currentStatus.getEtiqueta() + ") al estado inicial REGISTRADO.");
                }
                break;

            case UNDER_QA_INSPECTION:
                validateCanStartQA(currentStatus, lot);
                break;

            case READY_FOR_CERTIFICATION:
                validateCanBeReadyForCertification(lot);
                break;

            case OBSERVED:
                // Permite transicionar a OBSERVADO ante fallas o desviaciones
                break;

            case REJECTED:
                // Rechazo definitivo
                break;

            case IN_CERTIFICATION:
                validateCanStartCertification(lot);
                break;

            case CERTIFIED:
                validateCanBeCertified(lot);
                break;

            case READY_FOR_DISPATCH:
                validateCanBeReadyForDispatch(lot);
                break;

            case DISPATCHED:
                // Caso 1: REGISTRADO u otros estados no pueden ir directo a DESPACHADO
                validateCanBeDispatched(lot);
                break;

            case CANCELLED:
                validateCanCancel(currentStatus, lot);
                break;
        }

        // Aplicar transición
        String oldStatusStr = lot.getEstado();
        lot.setEstado(targetStatus.name());
        lot.setFechaActualizacion(LocalDateTime.now());
        Lot savedLot = lotRepository.save(lot);

        // Registro de Auditoría en Historial Inmutable
        LotHistory history = new LotHistory(
                savedLot,
                oldStatusStr,
                targetStatus.name(),
                userEmail != null ? userEmail : "SYSTEM",
                userRole != null ? userRole : "SYSTEM",
                reason != null ? reason : "Transición de estado a " + targetStatus.getEtiqueta()
        );
        lotHistoryRepository.save(history);

        return savedLot;
    }

    private void validateCanStartQA(LotStatus current, Lot lot) {
        if (current != LotStatus.REGISTERED && current != LotStatus.OBSERVED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar inspección QA desde el estado actual: " + current.getEtiqueta());
        }
    }

    public void validateCanBeReadyForCertification(Lot lot) {
        // Caso 2: Lote sin QA
        QualityInspection qi = qualityInspectionRepository.findByLoteId(lot.getId()).orElse(null);
        if (qi == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " no cuenta con ninguna inspección de calidad registrada.");
        }

        // Casos 3 y 4: QA OBSERVADO o NO_CONFORME
        if (!"CONFORME".equalsIgnoreCase(qi.getResultadoOrganoleptico())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación porque el lote tiene una inspección QA con resultado '" + qi.getResultadoOrganoleptico() + "'. Se requiere dictamen CONFORME.");
        }

        // Caso 5: Cadena de frío con desviación crítica o incidencias activas
        List<ColdChainIncident> activeIncidents = coldChainIncidentRepository.findByLoteIdAndEstadoIn(lot.getId(), List.of("ACTIVE", "UNDER_REVIEW"));
        if (!activeIncidents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación porque existe una desviación crítica de temperatura en cadena de frío sin subsanar.");
        }

        List<ColdChainRecord> coldRecords = coldChainRecordRepository.findByLoteIdOrderByFechaHoraDesc(lot.getId());
        if (coldRecords == null || coldRecords.isEmpty()) {
            auditService.logAction(
                    "COLD_CHAIN_DATA_GAP",
                    "FRIO",
                    "Lot",
                    lot.getId().toString(),
                    "Bloqueo de certificación: Lote " + lot.getCodigo() + " no cuenta con lecturas de cadena de frío.",
                    "SYSTEM",
                    "SYSTEM"
            );
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación debido a información térmica insuficiente o inexistente en cadena de frío.");
        }

        // Caso 8: Verificación de frescura térmica (máximo 12 horas de antigüedad)
        ColdChainRecord latestCold = coldRecords.get(0);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxReadingAgeHours);
        if (latestCold.getFechaHora() == null || latestCold.getFechaHora().isBefore(cutoff)) {
            auditService.logAction(
                    "COLD_CHAIN_DATA_GAP",
                    "FRIO",
                    "Lot",
                    lot.getId().toString(),
                    "Bloqueo de certificación: Última lectura térmica de lote " + lot.getCodigo() + " excede el límite de " + maxReadingAgeHours + " horas.",
                    "SYSTEM",
                    "SYSTEM"
            );
            notificationService.createNotification(
                    "QA",
                    "ALERTA BRECHA DE DATOS TÉRMICOS",
                    "El lote " + lot.getCodigo() + " tiene lecturas de frío obsoletas (> " + maxReadingAgeHours + "h). Se requiere registro térmico actualizado antes de certificar.",
                    "HIGH"
            );
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación debido a lecturas térmicas discontinuas u obsoletas (última lectura excede " + maxReadingAgeHours + " horas).");
        }

        boolean hasUnresolvedCriticalCold = coldRecords.stream().anyMatch(c -> "CRITICAL".equalsIgnoreCase(c.getEstadoMedicion()))
                && coldChainIncidentRepository.findByLoteIdOrderByFechaCreacionDesc(lot.getId()).stream().noneMatch(i -> "RESOLVED".equalsIgnoreCase(i.getEstado()));
        if (hasUnresolvedCriticalCold) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación porque existe una desviación crítica de temperatura en cadena de frío.");
        }

        // Caso 6: Documentación obligatoria incompleta
        List<Document> docs = documentRepository.findByLoteId(lot.getId());
        if (docs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede iniciar certificación porque la documentación obligatoria del expediente digital está incompleta.");
        }
    }

    public void validateCanStartCertification(Lot lot) {
        LotStatus current = LotStatus.fromString(lot.getEstado());
        if (current == LotStatus.REGISTERED || current == LotStatus.OBSERVED || current == LotStatus.REJECTED) {
            validateCanBeReadyForCertification(lot);
        } else if (current != LotStatus.READY_FOR_CERTIFICATION && current != LotStatus.IN_CERTIFICATION) {
            validateCanBeReadyForCertification(lot);
        }
    }

    public void validateCanBeCertified(Lot lot) {
        validateCanStartCertification(lot);
    }

    public void validateCanBeReadyForDispatch(Lot lot) {
        LotStatus current = LotStatus.fromString(lot.getEstado());
        // Caso 8: Trámite en evaluación o no certificado no puede marcarse para despacho
        if (current != LotStatus.CERTIFIED && current != LotStatus.READY_FOR_DISPATCH) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " debe estar CERTIFICADO antes de pasar a preparación de despacho. Estado actual: " + current.getEtiqueta());
        }

        SanitaryCertification cert = sanitaryCertificationRepository.findByLoteId(lot.getId()).orElse(null);
        if (cert == null || !"APROBADO".equalsIgnoreCase(cert.getEstado())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede habilitar despacho. El trámite SANIPES se encuentra en estado: " + (cert != null ? cert.getEstado() : "NO INICIADO"));
        }

        // BR-P1-013 / Decisión 6: Bloqueo si hay incidencias térmicas activas
        List<ColdChainIncident> activeIncidents = coldChainIncidentRepository.findByLoteIdAndEstadoIn(lot.getId(), List.of("ACTIVE", "UNDER_REVIEW"));
        if (!activeIncidents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0 / P1-013: Despacho denegado. El lote presenta una incidencia crítica de temperatura en cadena de frío pendiente de resolución técnica.");
        }

        List<ColdChainRecord> coldRecords = coldChainRecordRepository.findByLoteIdOrderByFechaHoraDesc(lot.getId());
        if (coldRecords == null || coldRecords.isEmpty() || coldRecords.get(0).getFechaHora() == null || coldRecords.get(0).getFechaHora().isBefore(LocalDateTime.now().minusHours(maxReadingAgeHours))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: Despacho denegado por inconsistencia en cadena de frío: información térmica insuficiente o discontinua (última lectura excede " + maxReadingAgeHours + " horas).");
        }
    }

    public void validateCanBeDispatched(Lot lot) {
        LotStatus current = LotStatus.fromString(lot.getEstado());
        // Caso 1: REGISTRADO directo a DESPACHADO
        if (current != LotStatus.READY_FOR_DISPATCH && current != LotStatus.CERTIFIED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: No se puede autorizar el despacho directo de un lote en estado: " + current.getEtiqueta() + ". Debe estar habilitado para despacho.");
        }

        // Caso 9: Certificación no aprobada o rechazada
        SanitaryCertification cert = sanitaryCertificationRepository.findByLoteId(lot.getId()).orElse(null);
        if (cert == null || !"APROBADO".equalsIgnoreCase(cert.getEstado())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: Despacho denegado. Se requiere Certificado Sanitario SANIPES en estado APROBADO.");
        }

        // BR-P1-013 / Decisión 6: Bloqueo si hay incidencias térmicas activas
        List<ColdChainIncident> activeIncidents = coldChainIncidentRepository.findByLoteIdAndEstadoIn(lot.getId(), List.of("ACTIVE", "UNDER_REVIEW"));
        if (!activeIncidents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0 / P1-013: Despacho denegado. El lote presenta una incidencia crítica de temperatura en cadena de frío pendiente de resolución técnica.");
        }

        List<ColdChainRecord> coldRecords = coldChainRecordRepository.findByLoteIdOrderByFechaHoraDesc(lot.getId());
        if (coldRecords == null || coldRecords.isEmpty() || coldRecords.get(0).getFechaHora() == null || coldRecords.get(0).getFechaHora().isBefore(LocalDateTime.now().minusHours(maxReadingAgeHours))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "BLOQUEO P0: Despacho denegado por inconsistencia en cadena de frío: información térmica insuficiente o discontinua (última lectura excede " + maxReadingAgeHours + " horas).");
        }
    }

    private void validateCanCancel(LotStatus current, Lot lot) {
        if (current == LotStatus.DISPATCHED || current == LotStatus.CERTIFIED || current == LotStatus.IN_CERTIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: No se puede anular un lote en proceso avanzado de certificación o despacho (" + current.getEtiqueta() + ").");
        }
    }
}
