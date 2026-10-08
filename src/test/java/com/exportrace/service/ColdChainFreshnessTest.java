package com.exportrace.service;

import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ColdChainFreshnessTest {

    @Autowired
    private ColdChainService coldChainService;

    @Autowired
    private LotStateMachineService lotStateMachineService;

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRecordRepository;

    @Autowired
    private QualityInspectionRepository qualityInspectionRepository;

    @Autowired
    private SanitaryCertificationRepository sanitaryCertificationRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Lot testLot;
    private Product frozenProduct;

    @BeforeEach
    void setUp() {
        frozenProduct = new Product();
        frozenProduct.setCodigo("PROD-FRESH-FROZEN");
        frozenProduct.setNombre("Filete de Merluza Congelada");
        frozenProduct.setTipoConservacion("CONGELADO");
        frozenProduct.setTempOptimaMin(-25.0);
        frozenProduct.setTempOptimaMax(-18.0);
        frozenProduct = productRepository.save(frozenProduct);

        testLot = new Lot();
        testLot.setCodigo("EXP-FRESH-" + System.currentTimeMillis());
        testLot.setProducto(frozenProduct);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setPesoNetoKg(15000.0);
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setQrToken("QR-FRESH-" + System.currentTimeMillis());
        testLot = lotRepository.save(testLot);
    }

    private void addConformingQualityInspection() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Ing. Control Calidad");
        qi.setResultadoOrganoleptico("CONFORME");
        qi.setObservaciones("Inspección conforme sin no conformidades");
        qi.setFechaInspeccion(LocalDateTime.now().minusHours(2));
        qualityInspectionRepository.save(qi);
    }

    private void addMandatoryDocument() {
        Document doc = new Document("expediente_sanipes.pdf", "DECLARACION_JURADA", "/app/data/uploads/exp.pdf", "Doc QA", testLot);
        documentRepository.save(doc);
    }

    @Test
    @DisplayName("1. Lectura reciente (1 hora atrás) -> isColdChainFresh = true")
    void testColdChainFresh_WithRecentReading_ReturnsTrue() {
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-20.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setResponsableNombre("QA Termo");
        r.setFechaHora(LocalDateTime.now().minusHours(1));
        coldChainRecordRepository.save(r);

        assertTrue(coldChainService.isColdChainFresh(testLot.getId()));
    }

    @Test
    @DisplayName("2. Lectura en el límite permitido (11 horas 59 min) -> isColdChainFresh = true")
    void testColdChainFresh_WithLimitReading_ReturnsTrue() {
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-21.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setResponsableNombre("QA Termo");
        r.setFechaHora(LocalDateTime.now().minusHours(11).minusMinutes(59));
        coldChainRecordRepository.save(r);

        assertTrue(coldChainService.isColdChainFresh(testLot.getId()));
    }

    @Test
    @DisplayName("3. Lectura vencida (13 horas atrás) -> isColdChainFresh = false")
    void testColdChainFresh_WithExpiredReading_ReturnsFalse() {
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-20.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setResponsableNombre("QA Termo");
        r.setFechaHora(LocalDateTime.now().minusHours(13));
        coldChainRecordRepository.save(r);

        assertFalse(coldChainService.isColdChainFresh(testLot.getId()));
    }

    @Test
    @DisplayName("4. Sin lecturas registradas -> isColdChainFresh = false")
    void testColdChainFresh_WithNoReading_ReturnsFalse() {
        assertFalse(coldChainService.isColdChainFresh(testLot.getId()));
    }

    @Test
    @DisplayName("5. Múltiples lecturas -> toma la más reciente cronológicamente")
    void testColdChainFresh_MultipleReadings_PicksLatest() {
        // Lectura antigua (20h atrás)
        ColdChainRecord rOld = new ColdChainRecord();
        rOld.setLote(testLot);
        rOld.setTemperaturaCelsius(-20.0);
        rOld.setEstadoMedicion("NORMAL");
        rOld.setUbicacionCamara("Cámara 01");
        rOld.setFechaHora(LocalDateTime.now().minusHours(20));
        coldChainRecordRepository.save(rOld);

        // Lectura nueva (2h atrás)
        ColdChainRecord rNew = new ColdChainRecord();
        rNew.setLote(testLot);
        rNew.setTemperaturaCelsius(-22.0);
        rNew.setEstadoMedicion("NORMAL");
        rNew.setUbicacionCamara("Cámara 01");
        rNew.setFechaHora(LocalDateTime.now().minusHours(2));
        coldChainRecordRepository.save(rNew);

        assertTrue(coldChainService.isColdChainFresh(testLot.getId()));
    }

    @Test
    @DisplayName("6. Registro con timestamp futuro (> 5 min) -> Rechazado con HTTP 400")
    void testAddReading_FutureTimestamp_Rejected400() {
        ColdChainRecordDTO dto = new ColdChainRecordDTO();
        dto.setTemperature(-20.0);
        dto.setRecordedAt(LocalDateTime.now().plusHours(2).toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            coldChainService.addTemperatureLog(testLot.getId(), dto, "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("futura"));
    }

    @Test
    @DisplayName("7. Certificación BLOQUEADA si lectura tiene >12 horas -> HTTP 422 y Auditoría COLD_CHAIN_DATA_GAP")
    void testCertificationBlocked_WhenColdChainReadingExpired_Throws422() {
        addConformingQualityInspection();
        addMandatoryDocument();

        // Lectura obsoleta (14h atrás)
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-20.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setFechaHora(LocalDateTime.now().minusHours(14));
        coldChainRecordRepository.save(r);

        testLot.setEstado(LotStatus.UNDER_QA_INSPECTION.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.requestCertification(testLot.getId(), "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("12 horas") || ex.getReason().contains("discontinuas"));

        // Verificar auditoría forense de brecha de datos
        List<AuditLog> audits = auditLogRepository.findByActionOrderByCreatedAtDesc("COLD_CHAIN_DATA_GAP");
        assertTrue(audits.stream().anyMatch(a -> a.getEntityId() != null && a.getEntityId().equals(testLot.getId().toString())));
    }

    @Test
    @DisplayName("8. Certificación BLOQUEADA si no hay registros de temperatura -> HTTP 422")
    void testCertificationBlocked_WhenNoColdChainReadings_Throws422() {
        addConformingQualityInspection();
        addMandatoryDocument();

        testLot.setEstado(LotStatus.UNDER_QA_INSPECTION.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.requestCertification(testLot.getId(), "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("insuficiente o inexistente"));
    }

    @Test
    @DisplayName("9. Despacho BLOQUEADO si la lectura térmica supera 12 horas -> HTTP 422")
    void testDispatchBlocked_WhenColdChainReadingExpired_Throws422() {
        addConformingQualityInspection();
        addMandatoryDocument();

        // Certificado SANIPES APROBADO
        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(testLot);
        cert.setNumeroCertificado("SANIPES-EXP-001");
        cert.setEstado("APROBADO");
        cert.setEntidadEmisora("SENASA/SANIPES");
        cert.setFechaEmision(LocalDateTime.now().minusDays(1));
        sanitaryCertificationRepository.save(cert);

        // Lectura de frío obsoleta (15h atrás)
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-21.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setFechaHora(LocalDateTime.now().minusHours(15));
        coldChainRecordRepository.save(r);

        testLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(testLot);

        Dispatch dispatchReq = new Dispatch();
        dispatchReq.setNumeroContenedor("CONT-12345");
        dispatchReq.setPrecintoSeguridad("SEAL-12345");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            dispatchService.registerDispatch(testLot.getId(), dispatchReq, "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cadena de frío"));
    }

    @Test
    @DisplayName("10. Certificación PERMITIDA cuando la lectura de frío es reciente (<=12h)")
    void testCertificationAllowed_WhenColdChainReadingIsFresh() {
        addConformingQualityInspection();
        addMandatoryDocument();

        // Lectura fresca (2h atrás)
        ColdChainRecord r = new ColdChainRecord();
        r.setLote(testLot);
        r.setTemperaturaCelsius(-21.0);
        r.setEstadoMedicion("NORMAL");
        r.setUbicacionCamara("Cámara 01");
        r.setFechaHora(LocalDateTime.now().minusHours(2));
        coldChainRecordRepository.save(r);

        testLot.setEstado(LotStatus.READY_FOR_CERTIFICATION.name());
        lotRepository.save(testLot);

        assertDoesNotThrow(() -> {
            certificationService.requestCertification(testLot.getId(), "qa@exportrace.pe");
        });
    }
}
