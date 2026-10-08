package com.exportrace.service;

import com.exportrace.dto.ColdChainIncidentDTO;
import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.dto.ResolveIncidentRequestDTO;
import com.exportrace.dto.ThermalProfileDTO;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ColdChainService {

    @Value("${coldchain.max-reading-age-hours:12}")
    private int maxReadingAgeHours = 12;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private ColdChainIncidentRepository coldChainIncidentRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private QualityInspectionRepository qualityInspectionRepository;

    @Autowired
    private SanitaryCertificationRepository sanitaryCertificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    public int getMaxReadingAgeHours() {
        return maxReadingAgeHours;
    }

    public void setMaxReadingAgeHours(int maxReadingAgeHours) {
        this.maxReadingAgeHours = maxReadingAgeHours;
    }

    public boolean isColdChainFresh(Long lotId) {
        List<ColdChainRecord> records = coldChainRepository.findByLoteIdOrderByFechaHoraDesc(lotId);
        if (records == null || records.isEmpty()) {
            return false;
        }
        ColdChainRecord latest = records.get(0);
        if (latest.getFechaHora() == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxReadingAgeHours);
        return !latest.getFechaHora().isBefore(cutoff);
    }

    public List<ColdChainRecordDTO> getLogsByLotId(Long lotId) {
        return coldChainRepository.findByLoteIdOrderByFechaHoraDesc(lotId).stream()
                .map(ColdChainRecordDTO::new)
                .toList();
    }

    public List<ColdChainIncidentDTO> getIncidentsByLotId(Long lotId) {
        return coldChainIncidentRepository.findByLoteIdOrderByFechaCreacionDesc(lotId).stream()
                .map(ColdChainIncidentDTO::new)
                .toList();
    }

    public ThermalProfileDTO getThermalProfile(Long lotId) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));
        return new ThermalProfileDTO(lot.getProducto());
    }

    /**
     * Evalúa el estado de temperatura según el perfil del producto (RF-28, BR-P0-002)
     */
    public String evaluateTemperatureStatus(Product product, Double temp) {
        if (temp == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La temperatura no puede ser nula.");
        }

        // Validación de valores físicamente improbables (-80°C a +60°C)
        if (temp < -80.0 || temp > 60.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lectura de temperatura físicamente imposible (" + temp + "°C). Rango admitido: -80.0°C a +60.0°C. Verifique sensor.");
        }

        boolean isRefrigerado = product != null && product.isRefrigerado();

        if (isRefrigerado) {
            // Producto Refrigerado / Fresco (≤ 4.0°C)
            if (temp >= 0.0 && temp <= 4.0) {
                return "NORMAL";
            } else if (temp > 4.0) {
                return "CRITICAL"; // Desviación por ruptura de cadena de frío
            } else {
                // temp < 0.0: Congelamiento indebido de pescado fresco
                return temp >= -1.0 ? "NORMAL" : "CRITICAL";
            }
        } else {
            // Producto Congelado (Norma SANIPES / Codex: ≤ -18.0°C)
            if (temp <= -18.0) {
                return "NORMAL";
            } else if (temp <= -15.0) {
                return "WARNING"; // Fluctuación térmica breve controlada
            } else {
                return "CRITICAL"; // Desviación crítica de frío (> -15.0°C)
            }
        }
    }

    @Transactional
    public ColdChainRecordDTO addTemperatureLog(Long lotId, ColdChainRecordDTO dto, String userEmail, String userRole) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        if (dto.getTemperature() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor de temperatura es obligatorio.");
        }

        // 1. Evaluar estado térmico dinámico según producto
        String status = evaluateTemperatureStatus(lot.getProducto(), dto.getTemperature());

        // 2. Determinar y validar timestamp de lectura
        LocalDateTime recordTime = LocalDateTime.now();
        if (dto.getRecordedAt() != null && !dto.getRecordedAt().isBlank()) {
            try {
                LocalDateTime parsedTime = LocalDateTime.parse(dto.getRecordedAt());
                if (parsedTime.isAfter(LocalDateTime.now().plusMinutes(5))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha/hora del registro no puede ser futura.");
                }
                recordTime = parsedTime;
            } catch (DateTimeParseException e) {
                // If parsing standard ISO string fails, default to now or validate
            }
        }

        // 3. Guardar registro inmutable de lectura
        ColdChainRecord record = new ColdChainRecord();
        record.setLote(lot);
        record.setFechaHora(recordTime);
        record.setTemperaturaCelsius(dto.getTemperature());
        record.setUbicacionCamara(dto.getLocation() != null && !dto.getLocation().isBlank() ? dto.getLocation() : "Cámara Frigorífica #01");
        record.setResponsableNombre(dto.getResponsible() != null && !dto.getResponsible().isBlank() ? dto.getResponsible() : (userEmail != null ? userEmail : "Técnico Frigorífico QA"));
        record.setEstadoMedicion(status);
        record.setObservaciones(dto.getObservations());

        ColdChainRecord savedRecord = coldChainRepository.save(record);

        // 3. Gestión de Incidencias y Máquina de Estados ante desviación CRITICAL
        if ("CRITICAL".equalsIgnoreCase(status)) {
            handleCriticalTemperature(lot, savedRecord, userEmail, userRole);
        }

        return new ColdChainRecordDTO(savedRecord);
    }

    // Sobrecarga para compatibilidad
    @Transactional
    public ColdChainRecordDTO addTemperatureLog(Long lotId, ColdChainRecordDTO dto, String userEmail) {
        return addTemperatureLog(lotId, dto, userEmail, "QA");
    }

    private void handleCriticalTemperature(Lot lot, ColdChainRecord record, String userEmail, String userRole) {
        Product product = lot.getProducto();
        Double limit = (product != null && product.isRefrigerado()) ? 4.0 : -15.0;

        // Verificar si ya existe una incidencia activa o en revisión para este lote (evitar duplicados masivos)
        List<ColdChainIncident> activeIncidents = coldChainIncidentRepository.findByLoteIdAndEstadoIn(lot.getId(), List.of("ACTIVE", "UNDER_REVIEW"));
        ColdChainIncident incident;
        if (activeIncidents.isEmpty()) {
            incident = new ColdChainIncident(
                    lot,
                    record,
                    record.getTemperaturaCelsius(),
                    limit,
                    product != null ? product.getTipoConservacion() : "CONGELADO"
            );
            incident.setObservaciones("Desviación térmica crítica detectada en " + record.getUbicacionCamara() + ": " + record.getTemperaturaCelsius() + "°C.");
            coldChainIncidentRepository.save(incident);

            // Notificación a QA
            notificationService.createNotification(
                    "QA",
                    "ALERTA CRÍTICA CADENA DE FRÍO",
                    "El lote " + lot.getCodigo() + " registró " + record.getTemperaturaCelsius() + "°C (Límite: " + limit + "°C) en " + record.getUbicacionCamara() + ". Requiere intervención técnica.",
                    "HIGH"
            );
        } else {
            incident = activeIncidents.get(0);
            incident.setTemperaturaLeida(record.getTemperaturaCelsius());
            incident.setRecord(record);
            coldChainIncidentRepository.save(incident);
        }

        // Auditoría forense inmutable
        auditService.logAction(
                "COLD_CHAIN_CRITICAL",
                "FRIO",
                "Lot",
                lot.getId().toString(),
                "Alerta crítica de temperatura: " + record.getTemperaturaCelsius() + "°C en lote " + lot.getCodigo(),
                userEmail != null ? userEmail : "SISTEMA_FRIGORIFICO",
                userRole != null ? userRole : "SENSOR"
        );

        // Integración con Máquina de Estados:
        LotStatus currentStatus = LotStatus.fromString(lot.getEstado());
        if (currentStatus == LotStatus.READY_FOR_CERTIFICATION) {
            lot.setEstado(LotStatus.OBSERVED.name());
            lot.setFechaActualizacion(LocalDateTime.now());
            lotRepository.save(lot);
        } else if (currentStatus == LotStatus.CERTIFIED || currentStatus == LotStatus.READY_FOR_DISPATCH) {
            // Regla Decisión 6 / BR-P1-013: Bloqueo preventivo post-certificación sin revocar certificado externo
            lot.setEstado(LotStatus.OBSERVED.name());
            lot.setFechaActualizacion(LocalDateTime.now());
            lotRepository.save(lot);

            notificationService.createNotification(
                    "LOGISTICA",
                    "DESPACHO BLOQUEADO POR ALERTA TÉRMICA",
                    "El lote certificado " + lot.getCodigo() + " sufrió una fluctuación térmica crítica (" + record.getTemperaturaCelsius() + "°C). Despacho bloqueado preventivamente.",
                    "HIGH"
            );
        }
    }

    @Transactional
    public ColdChainIncidentDTO reviewIncident(Long incidentId, String userEmail, String userRole, String notes) {
        ColdChainIncident incident = coldChainIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada con ID: " + incidentId));

        if (!"ACTIVE".equalsIgnoreCase(incident.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La incidencia ya se encuentra en estado: " + incident.getEstado());
        }

        incident.setEstado("UNDER_REVIEW");
        incident.setRevisadoPor(userEmail != null ? userEmail : "QA");
        incident.setFechaRevision(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            incident.setObservaciones((incident.getObservaciones() != null ? incident.getObservaciones() + " | " : "") + notes);
        }

        ColdChainIncident saved = coldChainIncidentRepository.save(incident);

        auditService.logAction(
                "COLD_CHAIN_INCIDENT_REVIEW",
                "FRIO",
                "ColdChainIncident",
                incidentId.toString(),
                "Incidencia de frío puesta en revisión para lote " + incident.getLote().getCodigo(),
                userEmail != null ? userEmail : "QA",
                userRole != null ? userRole : "QA"
        );

        return new ColdChainIncidentDTO(saved);
    }

    @Transactional
    public ColdChainIncidentDTO resolveIncident(Long incidentId, ResolveIncidentRequestDTO req, String userEmail, String userRole) {
        ColdChainIncident incident = coldChainIncidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia no encontrada con ID: " + incidentId));

        if ("RESOLVED".equalsIgnoreCase(incident.getEstado())) {
            return new ColdChainIncidentDTO(incident); // Idempotente
        }

        if (req.getTechnicalJustification() == null || req.getTechnicalJustification().trim().length() < 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La justificación técnica de subsanación debe tener al menos 15 caracteres descriptivos.");
        }

        incident.setEstado("RESOLVED");
        incident.setResueltoPor(userEmail != null ? userEmail : "QA_LEAD");
        incident.setFechaResolucion(LocalDateTime.now());
        incident.setJustificacionTecnica(req.getTechnicalJustification().trim());
        incident.setAccionesTomadas(req.getActionsTaken() != null ? req.getActionsTaken() : "Re-estabilización térmica en túnel y verificación con termómetro patrón.");
        if (req.getEvidenceUrl() != null) {
            incident.setEvidenciaUrl(req.getEvidenceUrl());
        }
        if (req.getObservations() != null) {
            incident.setObservaciones(req.getObservations());
        }

        ColdChainIncident saved = coldChainIncidentRepository.save(incident);

        auditService.logAction(
                "COLD_CHAIN_INCIDENT_RESOLVED",
                "FRIO",
                "ColdChainIncident",
                incidentId.toString(),
                "Incidencia de frío resuelta técnicamente: " + req.getTechnicalJustification(),
                userEmail != null ? userEmail : "QA",
                userRole != null ? userRole : "QA"
        );

        // Evaluar rehabilitación del lote si ya no quedan incidencias activas
        Lot lot = incident.getLote();
        long remainingActive = coldChainIncidentRepository.countByLoteIdAndEstadoIn(lot.getId(), List.of("ACTIVE", "UNDER_REVIEW"));
        
        if (remainingActive == 0) {
            // Verificar si el lote tiene QA Conforme
            QualityInspection qi = qualityInspectionRepository.findByLoteId(lot.getId()).orElse(null);
            SanitaryCertification cert = sanitaryCertificationRepository.findByLoteId(lot.getId()).orElse(null);

            if (cert != null && "APROBADO".equalsIgnoreCase(cert.getEstado())) {
                // Post-certificación: Rehabilitar para despacho
                lot.setEstado(LotStatus.READY_FOR_DISPATCH.name());
                lot.setFechaActualizacion(LocalDateTime.now());
                lotRepository.save(lot);

                notificationService.createNotification(
                        "LOGISTICA",
                        "LOTE REHABILITADO PARA DESPACHO",
                        "El lote " + lot.getCodigo() + " completó la subsanación técnica de frío y está nuevamente habilitado para despacho.",
                        "INFO"
                );
            } else if (qi != null && "CONFORME".equalsIgnoreCase(qi.getResultadoOrganoleptico())) {
                // Pre-certificación: Rehabilitar para certificación
                lot.setEstado(LotStatus.READY_FOR_CERTIFICATION.name());
                lot.setFechaActualizacion(LocalDateTime.now());
                lotRepository.save(lot);

                notificationService.createNotification(
                        "LOGISTICA",
                        "LOTE APTO PARA CERTIFICACIÓN",
                        "El lote " + lot.getCodigo() + " resolvió la observación térmica y queda apto para certificación SANIPES.",
                        "INFO"
                );
            }
        }

        return new ColdChainIncidentDTO(saved);
    }
}
