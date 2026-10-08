package com.exportrace.service;

import com.exportrace.dto.ColdChainIncidentDTO;
import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.dto.ResolveIncidentRequestDTO;
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
public class ColdChainServiceTest {

    @Autowired
    private ColdChainService coldChainService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private QualityInspectionRepository qualityInspectionRepository;

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private ColdChainIncidentRepository incidentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LotStateMachineService stateMachineService;

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private DispatchService dispatchService;

    private Product frozenProduct;
    private Product refrigeratedProduct;
    private Lot frozenLot;
    private Lot refrigeratedLot;

    @BeforeEach
    void setUp() {
        // 1. Producto Congelado
        frozenProduct = new Product();
        frozenProduct.setCodigo("PROD-POTA-CONG");
        frozenProduct.setNombre("Pota Congelada");
        frozenProduct.setTipoConservacion("CONGELADO");
        frozenProduct.setTempOptimaMin(-25.0);
        frozenProduct.setTempOptimaMax(-18.0);
        frozenProduct.setTempWarningMax(-15.0);
        frozenProduct.setTempCriticaMax(-15.0);
        frozenProduct = productRepository.save(frozenProduct);

        // 2. Producto Refrigerado / Fresco
        refrigeratedProduct = new Product();
        refrigeratedProduct.setCodigo("PROD-PESC-FRESCO");
        refrigeratedProduct.setNombre("Pescado Fresco Refrigerado");
        refrigeratedProduct.setTipoConservacion("REFRIGERADO");
        refrigeratedProduct.setTempOptimaMin(0.0);
        refrigeratedProduct.setTempOptimaMax(4.0);
        refrigeratedProduct.setTempWarningMax(4.0);
        refrigeratedProduct.setTempCriticaMax(4.0);
        refrigeratedProduct = productRepository.save(refrigeratedProduct);

        // 3. Lotes de prueba
        frozenLot = new Lot();
        frozenLot.setCodigo("EXP-CONG-" + System.currentTimeMillis());
        frozenLot.setProducto(frozenProduct);
        frozenLot.setEstado(LotStatus.REGISTERED.name());
        frozenLot.setPesoNetoKg(15000.0);
        frozenLot.setQrToken("QR-CONG-" + System.currentTimeMillis());
        frozenLot.setFechaProduccion(LocalDate.now());
        frozenLot = lotRepository.save(frozenLot);

        refrigeratedLot = new Lot();
        refrigeratedLot.setCodigo("EXP-REFRIG-" + System.currentTimeMillis());
        refrigeratedLot.setProducto(refrigeratedProduct);
        refrigeratedLot.setEstado(LotStatus.REGISTERED.name());
        refrigeratedLot.setPesoNetoKg(5000.0);
        refrigeratedLot.setQrToken("QR-REFRIG-" + System.currentTimeMillis());
        refrigeratedLot.setFechaProduccion(LocalDate.now());
        refrigeratedLot = lotRepository.save(refrigeratedLot);
    }

    private void addConformingDoc(Lot lot) {
        Document doc = new Document("DJ_Test.pdf", "DECLARACION_JURADA", "https://render.com/dj.pdf", "Tester", lot);
        documentRepository.save(doc);
    }

    private void addConformingQA(Lot lot) {
        QualityInspection qi = new QualityInspection();
        qi.setLote(lot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);
    }

    @Test
    @DisplayName("CF-01 a CF-07: Evaluación de rangos térmicos para producto CONGELADO")
    void testFrozenProductTemperatureRanges() {
        assertEquals("NORMAL", coldChainService.evaluateTemperatureStatus(frozenProduct, -25.0)); // CF-01
        assertEquals("NORMAL", coldChainService.evaluateTemperatureStatus(frozenProduct, -20.0)); // CF-02
        assertEquals("NORMAL", coldChainService.evaluateTemperatureStatus(frozenProduct, -18.0)); // CF-03
        assertEquals("WARNING", coldChainService.evaluateTemperatureStatus(frozenProduct, -17.0)); // CF-04
        assertEquals("WARNING", coldChainService.evaluateTemperatureStatus(frozenProduct, -15.0)); // CF-05 (límite)
        assertEquals("CRITICAL", coldChainService.evaluateTemperatureStatus(frozenProduct, -14.0)); // CF-06
        assertEquals("CRITICAL", coldChainService.evaluateTemperatureStatus(frozenProduct, -5.0)); // CF-07
    }

    @Test
    @DisplayName("Evaluación de rangos térmicos para producto REFRIGERADO (Fresco)")
    void testRefrigeratedProductTemperatureRanges() {
        assertEquals("NORMAL", coldChainService.evaluateTemperatureStatus(refrigeratedProduct, 2.0));
        assertEquals("NORMAL", coldChainService.evaluateTemperatureStatus(refrigeratedProduct, 4.0));
        assertEquals("CRITICAL", coldChainService.evaluateTemperatureStatus(refrigeratedProduct, 5.0)); // Desviación
        assertEquals("CRITICAL", coldChainService.evaluateTemperatureStatus(refrigeratedProduct, -3.0)); // Congelamiento indebido
    }

    @Test
    @DisplayName("Validación física: Rechazar temperaturas inverosímiles (-150°C y +150°C)")
    void testPhysicallyImprobableTemperaturesRejected() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () -> {
            coldChainService.evaluateTemperatureStatus(frozenProduct, 150.0);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());
        assertTrue(ex1.getReason().contains("físicamente imposible"));

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> {
            coldChainService.evaluateTemperatureStatus(frozenProduct, -150.0);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
        assertTrue(ex2.getReason().contains("físicamente imposible"));
    }

    @Test
    @DisplayName("Ciclo de Incidencia: CRITICAL crea ACTIVE -> Retorno a normalidad NO borra incidencia")
    void testCriticalCreatesActiveIncidentAndNormalDoesNotDeleteIt() {
        ColdChainRecordDTO critReq = new ColdChainRecordDTO();
        critReq.setTemperature(-10.0); // Crítico para congelado
        critReq.setLocation("Cámara 01");
        critReq.setResponsible("Sensor T1");

        ColdChainRecordDTO saved = coldChainService.addTemperatureLog(frozenLot.getId(), critReq, "qa@exportrace.pe", "QA");
        assertEquals("CRITICAL", saved.getStatus());

        List<ColdChainIncidentDTO> incidents = coldChainService.getIncidentsByLotId(frozenLot.getId());
        assertEquals(1, incidents.size());
        assertEquals("ACTIVE", incidents.get(0).getStatus());

        // Nueva lectura en rango NORMAL (-20°C)
        ColdChainRecordDTO normReq = new ColdChainRecordDTO();
        normReq.setTemperature(-20.0);
        normReq.setLocation("Cámara 01");
        coldChainService.addTemperatureLog(frozenLot.getId(), normReq, "qa@exportrace.pe", "QA");

        // La incidencia DEBE CONTINUAR ACTIVA
        List<ColdChainIncidentDTO> incidentsAfter = coldChainService.getIncidentsByLotId(frozenLot.getId());
        assertEquals(1, incidentsAfter.size());
        assertEquals("ACTIVE", incidentsAfter.get(0).getStatus());
    }

    @Test
    @DisplayName("Transición de Incidencia: ACTIVE -> UNDER_REVIEW -> RESOLVED por QA")
    void testIncidentLifecycleTransitions() {
        ColdChainRecordDTO critReq = new ColdChainRecordDTO();
        critReq.setTemperature(-8.0);
        coldChainService.addTemperatureLog(frozenLot.getId(), critReq, "sensor@exportrace.pe", "SENSOR");

        ColdChainIncidentDTO incident = coldChainService.getIncidentsByLotId(frozenLot.getId()).get(0);
        Long incidentId = Long.valueOf(incident.getId());

        // 1. QA pone en revisión
        ColdChainIncidentDTO underReview = coldChainService.reviewIncident(incidentId, "qa@exportrace.pe", "QA", "Iniciando verificación técnica de termostato");
        assertEquals("UNDER_REVIEW", underReview.getStatus());
        assertEquals("qa@exportrace.pe", underReview.getReviewedBy());

        // 2. QA resuelve la incidencia con justificación técnica
        ResolveIncidentRequestDTO resolveReq = new ResolveIncidentRequestDTO();
        resolveReq.setTechnicalJustification("Se recalibró el compresor N°2 y se constató temperatura estabilizada a -22°C por 4 horas.");
        resolveReq.setActionsTaken("Mantenimiento de evaporador y verificación con termómetro patrón certificado.");

        ColdChainIncidentDTO resolved = coldChainService.resolveIncident(incidentId, resolveReq, "qa_lead@exportrace.pe", "QA");
        assertEquals("RESOLVED", resolved.getStatus());
        assertEquals("qa_lead@exportrace.pe", resolved.getResolvedBy());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    @DisplayName("Bloqueo de Certificación: Lote con incidencia térmica activa intenta certificarse -> 422")
    void testLotWithActiveIncidentCannotBeCertified() {
        addConformingQA(frozenLot);
        addConformingDoc(frozenLot);

        // Desviación crítica de frío
        ColdChainRecordDTO critReq = new ColdChainRecordDTO();
        critReq.setTemperature(-11.0);
        coldChainService.addTemperatureLog(frozenLot.getId(), critReq, "qa@exportrace.pe", "QA");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.requestCertification(frozenLot.getId(), "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("desviación crítica de temperatura"));
    }

    @Test
    @DisplayName("BR-P1-013 / Decisión 6: Lote CERTIFICADO que sufre desviación crítica bloquea Despacho pero conserva Certificado")
    void testCertifiedLotReceivesCriticalTemperature_BlocksDispatchAndPreservesCert() {
        addConformingQA(frozenLot);
        addConformingDoc(frozenLot);

        // Lectura normal inicial
        ColdChainRecordDTO normReq = new ColdChainRecordDTO();
        normReq.setTemperature(-22.0);
        coldChainService.addTemperatureLog(frozenLot.getId(), normReq, "qa@exportrace.pe", "QA");

        // Certificación aprobada
        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(frozenLot);
        cert.setNumeroCertificado("CS-SANIPES-2026-991");
        cert.setEstado("APROBADO");
        certificationRepository.save(cert);

        frozenLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(frozenLot);

        // Ocurre desviación crítica en cámara antes del embarque
        ColdChainRecordDTO postCertCrit = new ColdChainRecordDTO();
        postCertCrit.setTemperature(-8.0); // Ruptura de frío
        coldChainService.addTemperatureLog(frozenLot.getId(), postCertCrit, "sensor@exportrace.pe", "SENSOR");

        // Lote pasa a OBSERVADO y despacho queda bloqueado
        Lot updatedLot = lotRepository.findById(frozenLot.getId()).orElseThrow();
        assertEquals(LotStatus.OBSERVED.name(), updatedLot.getEstado());

        // El certificado SANIPES se conserva en BD (NO se revoca unilateralmente)
        SanitaryCertification preservedCert = certificationRepository.findByLoteId(frozenLot.getId()).orElseThrow();
        assertEquals("APROBADO", preservedCert.getEstado());
        assertEquals("CS-SANIPES-2026-991", preservedCert.getNumeroCertificado());

        // Intento de habilitar despacho o despachar arroja 422
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stateMachineService.validateCanBeReadyForDispatch(updatedLot);
        });
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @DisplayName("Rehabilitación: Tras resolución de incidencia y estabilización de frío, se rehabilita despacho")
    void testResolveIncidentRehabilitatesLotForDispatch() {
        addConformingQA(frozenLot);
        addConformingDoc(frozenLot);

        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(frozenLot);
        cert.setNumeroCertificado("CS-SANIPES-2026-888");
        cert.setEstado("APROBADO");
        certificationRepository.save(cert);

        frozenLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(frozenLot);

        // Desviación
        ColdChainRecordDTO critReq = new ColdChainRecordDTO();
        critReq.setTemperature(-9.0);
        coldChainService.addTemperatureLog(frozenLot.getId(), critReq, "sensor@exportrace.pe", "SENSOR");

        ColdChainIncident incident = incidentRepository.findAll().get(0);

        // Estabilización de frío (-21°C)
        ColdChainRecordDTO okReq = new ColdChainRecordDTO();
        okReq.setTemperature(-21.0);
        coldChainService.addTemperatureLog(frozenLot.getId(), okReq, "qa@exportrace.pe", "QA");

        // QA Resuelve técnicamente
        ResolveIncidentRequestDTO resolveReq = new ResolveIncidentRequestDTO();
        resolveReq.setTechnicalJustification("Subsanación técnica completada: verificación organoléptica sin descongelamiento superficial.");
        resolveReq.setActionsTaken("Traslado a túnel de choque y reinspección organoléptica conforme.");
        coldChainService.resolveIncident(incident.getId(), resolveReq, "qa_lead@exportrace.pe", "QA");

        // Lote queda rehabilitado en READY_FOR_DISPATCH
        Lot rehabilitatedLot = lotRepository.findById(frozenLot.getId()).orElseThrow();
        assertEquals(LotStatus.READY_FOR_DISPATCH.name(), rehabilitatedLot.getEstado());

        // Ahora sí se permite despacho
        assertDoesNotThrow(() -> {
            stateMachineService.validateCanBeDispatched(rehabilitatedLot);
        });
    }
}
