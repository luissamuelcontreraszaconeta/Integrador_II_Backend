package com.exportrace.service;

import com.exportrace.dto.*;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class LotService {

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
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    public List<LotDTO> getAllLots() {
        return lotRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(this::toFullDTO)
                .toList();
    }

    public LotDTO getLotById(Long id) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + id));
        return toFullDTO(lot);
    }

    public LotDTO getLotByCode(String code) {
        Lot lot = lotRepository.findByCodigo(code)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con código: " + code));
        return toFullDTO(lot);
    }

    public LotDTO getLotByQrToken(String qrToken) {
        Lot lot = lotRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con QR token: " + qrToken));
        return toFullDTO(lot);
    }

    @Transactional
    public LotDTO createLot(CreateLotRequest req, String userEmail) {
        // Auto-generate code EXP-2026-XXX
        long count = lotRepository.count() + 1;
        String code = String.format("EXP-2026-%03d", count);

        Product product = null;
        if (req.getProducto() != null && !req.getProducto().isEmpty()) {
            product = productRepository.findByCodigo(req.getProducto()).orElse(null);
        }
        if (product == null) {
            product = productRepository.findAll().stream().findFirst().orElse(null);
        }

        Lot lot = new Lot();
        lot.setCodigo(code);
        lot.setEstado("DRAFT");
        lot.setProducto(product);
        lot.setCantidadEmpaques(req.getCantidadEmpaques() != null ? req.getCantidadEmpaques() : 1000);
        lot.setTipoEmpaque(req.getTipoEmpaque() != null ? req.getTipoEmpaque() : "TN");
        lot.setPesoNetoKg(req.getPesoNetoKg() != null ? req.getPesoNetoKg() : 25000.0);
        lot.setPlantaProcesamiento(req.getPlantaProcesamiento() != null ? req.getPlantaProcesamiento() : "Planta Paita #01");
        lot.setLineaProcesamiento(req.getLineaProcesamiento() != null ? req.getLineaProcesamiento() : "Línea 02 - Bloques Exportación");
        lot.setProveedor("Asociación Pesquera Artesanal Paita Norte");
        lot.setEmbarcacion("E/P Don Luis II (CO-18492-PM)");
        
        if (req.getFechaProduccion() != null && !req.getFechaProduccion().isEmpty()) {
            try {
                lot.setFechaProduccion(LocalDate.parse(req.getFechaProduccion()));
            } catch (Exception e) {
                lot.setFechaProduccion(LocalDate.now());
            }
        } else {
            lot.setFechaProduccion(LocalDate.now());
        }

        if (req.getFechaVencimiento() != null && !req.getFechaVencimiento().isEmpty()) {
            try {
                lot.setFechaVencimiento(LocalDate.parse(req.getFechaVencimiento()));
            } catch (Exception e) {
                lot.setFechaVencimiento(LocalDate.now().plusYears(2));
            }
        } else {
            lot.setFechaVencimiento(LocalDate.now().plusYears(2));
        }

        lot.setInspeccionadoPor(req.getInspeccionadoPor() != null ? req.getInspeccionadoPor() : "Renzo Alva");
        lot.setObservaciones(req.getObservaciones());

        // Generate deterministic non-random QR token
        String qrToken = generateQrHash(code, System.currentTimeMillis());
        lot.setQrToken(qrToken);

        Lot savedLot = lotRepository.save(lot);

        // Record history entry
        String userName = userEmail;
        String userRole = "PRODUCCION";
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user != null) {
            userName = user.getNombre();
            userRole = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        }

        LotHistory history = new LotHistory(savedLot, null, "DRAFT", userName, userRole, "Creación inicial del lote en sistema");
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
    public LotDTO updateLotStatus(Long id, LotStatusUpdateRequest req, String userEmail) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + id));

        String oldStatus = lot.getEstado();
        String newStatus = req.getStatus();

        lot.setEstado(newStatus);
        lot.setFechaActualizacion(LocalDateTime.now());
        Lot updatedLot = lotRepository.save(lot);

        // Record history
        String userName = req.getUserName() != null ? req.getUserName() : userEmail;
        String userRole = req.getUserRole() != null ? req.getUserRole() : "USUARIO";
        
        LotHistory history = new LotHistory(updatedLot, oldStatus, newStatus, userName, userRole, req.getComment());
        lotHistoryRepository.save(history);

        return toFullDTO(updatedLot);
    }

    public LotDTO toFullDTO(Lot lot) {
        LotDTO dto = new LotDTO(lot);

        // Map Quality Inspection if present
        qualityRepository.findByLoteId(lot.getId())
                .ifPresent(qi -> dto.setQa(new QAInspectionDTO(qi)));

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
