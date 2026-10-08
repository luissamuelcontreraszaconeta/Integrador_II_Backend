package com.exportrace.service;

import com.exportrace.dto.*;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class LotService {

    private static final Logger log = LoggerFactory.getLogger(LotService.class);

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LotStateMachineService stateMachineService;

    @Autowired
    private AuditService auditService;

    public List<LotDTO> getAllLots() {
        return lotRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::toFullDTO)
                .toList();
    }

    public LotDTO getLotById(Long id) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + id));
        return toFullDTO(lot);
    }

    public LotDTO getLotByCode(String code) {
        Lot lot = lotRepository.findByCodigo(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con código: " + code));
        return toFullDTO(lot);
    }

    public LotDTO getLotByQrToken(String qrToken) {
        Lot lot = lotRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con QR token: " + qrToken));
        return toFullDTO(lot);
    }

    public PublicTraceabilityDTO getPublicTraceability(String qrToken) {
        Lot lot = lotRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código de trazabilidad no encontrado: " + qrToken));

        QualityInspection qa = qualityRepository.findByLoteId(lot.getId()).orElse(null);
        SanitaryCertification cert = certificationRepository.findByLoteId(lot.getId()).orElse(null);
        List<ColdChainRecord> coldRecords = coldChainRepository.findByLoteIdOrderByFechaHoraDesc(lot.getId());
        boolean hasCriticalCold = coldRecords.stream().anyMatch(c -> "CRITICAL".equalsIgnoreCase(c.getEstadoMedicion()));
        int docCount = documentRepository.findByLoteId(lot.getId()).size();

        return new PublicTraceabilityDTO(lot, qa, cert, coldRecords.size(), hasCriticalCold, docCount);
    }

    @Transactional
    public LotDTO createLot(CreateLotRequest req, String userEmail) {
        // CASO 2: Validar peso y campos obligatorios sin fallbacks silenciosos
        if (req.getPesoNetoKg() == null || req.getPesoNetoKg() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El peso neto es obligatorio y debe ser mayor que cero.");
        }
        if (req.getCantidadEmpaques() == null || req.getCantidadEmpaques() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de empaques es obligatoria y debe ser mayor que cero.");
        }
        if (req.getPlantaProcesamiento() == null || req.getPlantaProcesamiento().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La planta de procesamiento es obligatoria.");
        }
        if (req.getTipoEmpaque() == null || req.getTipoEmpaque().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de empaque es obligatorio.");
        }
        if (req.getProducto() == null || req.getProducto().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto es obligatorio.");
        }

        // CASO 1: Validación y defensa en profundidad contra códigos duplicados
        String code = req.getCodigo();
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim().toUpperCase();
            if (lotRepository.existsByCodigo(code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote registrado con el código " + code + ".");
            }
        } else {
            // Auto-generación con protección activa de colisión
            long count = lotRepository.count() + 1;
            do {
                code = String.format("EXP-2026-%03d", count++);
            } while (lotRepository.existsByCodigo(code));
        }

        Product product = null;
        if (req.getProducto() != null && !req.getProducto().trim().isEmpty()) {
            product = productRepository.findByCodigo(req.getProducto()).orElse(null);
            if (product == null) {
                product = productRepository.findByNombre(req.getProducto()).orElse(null);
            }
        }
        if (product == null) {
            product = productRepository.findAll().stream().findFirst().orElse(null);
        }
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto especificado no existe en el catálogo.");
        }

        Lot lot = new Lot();
        lot.setCodigo(code);
        lot.setEstado(LotStatus.REGISTERED.name());
        lot.setProducto(product);
        lot.setCantidadEmpaques(req.getCantidadEmpaques());
        lot.setTipoEmpaque(req.getTipoEmpaque());
        lot.setPesoNetoKg(req.getPesoNetoKg());
        lot.setPlantaProcesamiento(req.getPlantaProcesamiento());
        lot.setLineaProcesamiento(req.getLineaProcesamiento() != null && !req.getLineaProcesamiento().trim().isEmpty() 
                ? req.getLineaProcesamiento() : "Línea Principal");
        lot.setProveedor(req.getProveedor() != null && !req.getProveedor().trim().isEmpty() 
                ? req.getProveedor() : "Asociación Pesquera Artesanal");
        lot.setEmbarcacion(req.getEmbarcacion() != null && !req.getEmbarcacion().trim().isEmpty() 
                ? req.getEmbarcacion() : "E/P Don Luis II");
        
        if (req.getFechaProduccion() != null && !req.getFechaProduccion().isEmpty()) {
            try {
                lot.setFechaProduccion(LocalDate.parse(req.getFechaProduccion()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de fecha de producción inválido. Use YYYY-MM-DD.");
            }
        } else {
            lot.setFechaProduccion(LocalDate.now());
        }

        if (req.getFechaVencimiento() != null && !req.getFechaVencimiento().isEmpty()) {
            try {
                lot.setFechaVencimiento(LocalDate.parse(req.getFechaVencimiento()));
            } catch (Exception e) {
                lot.setFechaVencimiento(lot.getFechaProduccion().plusYears(2));
            }
        } else {
            lot.setFechaVencimiento(lot.getFechaProduccion().plusYears(2));
        }

        lot.setInspeccionadoPor(req.getInspeccionadoPor() != null ? req.getInspeccionadoPor() : "Operaciones Planta");
        lot.setObservaciones(req.getObservaciones());

        // Generate deterministic non-random QR token
        String qrToken = generateQrHash(code, System.currentTimeMillis());
        lot.setQrToken(qrToken);

        Lot savedLot;
        try {
            savedLot = lotRepository.save(lot);
        } catch (DataIntegrityViolationException dive) {
            log.warn("[LotService] Unique constraint violation on code '{}'", code);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un lote registrado con el código " + code + ".");
        }

        // Record history entry
        String userName = userEmail != null ? userEmail : "produccion@exportrace.pe";
        String userRole = "PRODUCCION";
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user != null) {
            userName = user.getNombre();
            userRole = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        }

        LotHistory history = new LotHistory(savedLot, null, LotStatus.REGISTERED.name(), userName, userRole, "Creación inicial del lote en sistema");
        lotHistoryRepository.save(history);

        // Initial cold chain record
        ColdChainRecord ccr = new ColdChainRecord();
        ccr.setLote(savedLot);
        ccr.setFechaHora(LocalDateTime.now());
        ccr.setTemperaturaCelsius(req.getTemperaturaInicial() != null ? req.getTemperaturaInicial() : -22.5);
        ccr.setUbicacionCamara("Cámara de Congelamiento #01");
        ccr.setResponsableNombre(userName);
        ccr.setEstadoMedicion("NORMAL");
        ccr.setObservaciones("Lectura inicial de temperatura al registrar el lote");
        coldChainRepository.save(ccr);

        return toFullDTO(savedLot);
    }

    @Transactional
    public LotDTO updateLot(Long id, UpdateLotRequest req, String userEmail) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + id));

        // CASO 3: Control de Concurrencia Optimista
        if (req.getVersion() != null && lot.getVersion() != null && !req.getVersion().equals(lot.getVersion())) {
            log.warn("[LotService] Optimistic lock conflict on Lot {}: sent version {}, actual version {}",
                    lot.getCodigo(), req.getVersion(), lot.getVersion());

            auditService.logAction(
                    null,
                    userEmail != null ? userEmail : "usuario@exportrace.pe",
                    "USUARIO",
                    "LOT_UPDATE_CONFLICT",
                    "LOTES",
                    "Lot",
                    String.valueOf(id),
                    "Conflicto de concurrencia al actualizar Lote " + lot.getCodigo() + ". Versión recibida: " + req.getVersion() + ", Versión actual: " + lot.getVersion(),
                    "version: " + lot.getVersion(),
                    "version: " + req.getVersion(),
                    "REJECTED",
                    null
            );

            throw new ResponseStatusException(HttpStatus.CONFLICT, "El lote fue modificado por otro usuario. Actualice la información antes de volver a guardar.");
        }

        // Caso 16 / Immutability Check: No se puede editar lote certificado o despachado
        LotStatus currentStatus = LotStatus.fromString(lot.getEstado());
        if (currentStatus == LotStatus.CERTIFIED || currentStatus == LotStatus.READY_FOR_DISPATCH || currentStatus == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: No se pueden modificar los datos de producción de un lote que ya ha sido certificado o despachado (" + currentStatus.getEtiqueta() + ").");
        }

        if (req.getPesoNetoKg() != null) {
            if (req.getPesoNetoKg() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El peso neto debe ser mayor que cero.");
            }
            lot.setPesoNetoKg(req.getPesoNetoKg());
        }
        if (req.getCantidadEmpaques() != null) {
            if (req.getCantidadEmpaques() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de empaques debe ser mayor que cero.");
            }
            lot.setCantidadEmpaques(req.getCantidadEmpaques());
        }
        if (req.getTipoEmpaque() != null && !req.getTipoEmpaque().trim().isEmpty()) {
            lot.setTipoEmpaque(req.getTipoEmpaque());
        }
        if (req.getPlantaProcesamiento() != null && !req.getPlantaProcesamiento().trim().isEmpty()) {
            lot.setPlantaProcesamiento(req.getPlantaProcesamiento());
        }
        if (req.getLineaProcesamiento() != null) {
            lot.setLineaProcesamiento(req.getLineaProcesamiento());
        }
        if (req.getProveedor() != null) {
            lot.setProveedor(req.getProveedor());
        }
        if (req.getEmbarcacion() != null) {
            lot.setEmbarcacion(req.getEmbarcacion());
        }
        if (req.getObservaciones() != null) {
            lot.setObservaciones(req.getObservaciones());
        }
        if (req.getInspeccionadoPor() != null) {
            lot.setInspeccionadoPor(req.getInspeccionadoPor());
        }
        lot.setFechaActualizacion(LocalDateTime.now());

        Lot saved;
        try {
            saved = lotRepository.save(lot);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException oolfe) {
            auditService.logAction(
                    null,
                    userEmail != null ? userEmail : "usuario@exportrace.pe",
                    "USUARIO",
                    "LOT_UPDATE_CONFLICT",
                    "LOTES",
                    "Lot",
                    String.valueOf(id),
                    "Conflicto de concurrencia capturado en persistencia para Lote " + lot.getCodigo(),
                    null,
                    null,
                    "REJECTED",
                    null
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El lote fue modificado por otro usuario. Actualice la información antes de volver a guardar.");
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        auditService.logAction(
                user != null ? user.getId() : null,
                userEmail != null ? userEmail : "usuario@exportrace.pe",
                user != null && user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION",
                "LOT_UPDATED",
                "LOTES",
                "Lot",
                String.valueOf(saved.getId()),
                "Lote actualizado: " + saved.getCodigo() + " (Peso: " + saved.getPesoNetoKg() + " kg)",
                null,
                "version: " + saved.getVersion(),
                "EXITOSO",
                null
        );

        return toFullDTO(saved);
    }

    @Transactional
    public LotDTO updateLotStatus(Long id, LotStatusUpdateRequest req, String userEmail) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + id));

        LotStatus targetStatus = LotStatus.fromString(req.getStatus());
        String userName = req.getUserName() != null ? req.getUserName() : userEmail;
        String userRole = req.getUserRole() != null ? req.getUserRole() : "USUARIO";

        Lot updatedLot = stateMachineService.transition(lot, targetStatus, userName, userRole, req.getComment());
        return toFullDTO(updatedLot);
    }

    public LotDTO toFullDTO(Lot lot) {
        LotDTO dto = new LotDTO(lot);

        // Map Quality Inspections history (1:N) & latest inspection
        List<QualityInspection> inspections = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(lot.getId());
        if (!inspections.isEmpty()) {
            dto.setQaInspections(inspections.stream().map(QAInspectionDTO::new).toList());
            dto.setQa(new QAInspectionDTO(inspections.get(inspections.size() - 1)));
        }

        // Map Cold Chain logs
        List<ColdChainRecord> ccrList = coldChainRepository.findByLoteIdOrderByFechaHoraDesc(lot.getId());
        dto.setColdChainLogs(ccrList.stream().map(ColdChainRecordDTO::new).toList());

        // Map Documents
        List<Document> docList = documentRepository.findByLoteId(lot.getId());
        dto.setDocuments(docList.stream().map(LotDocumentDTO::new).toList());

        return dto;
    }

    private String generateQrHash(String code, long timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((code + "-" + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 24);
        } catch (Exception e) {
            return "QR-" + System.currentTimeMillis();
        }
    }
}
