package com.exportrace.service;

import com.exportrace.dto.QAInspectionDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class LotStateMachineServiceTest {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private QualityInspectionRepository qualityInspectionRepository;

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private LotStateMachineService stateMachineService;

    @Autowired
    private CertificationService certificationService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private QualityService qualityService;

    private Lot testLot;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.findAll().stream().findFirst().orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("TEST_PROD");
            p.setNombre("Pota Test");
            return productRepository.save(p);
        });

        testLot = new Lot();
        testLot.setCodigo("EXP-TEST-" + System.currentTimeMillis());
        testLot.setProducto(testProduct);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setPesoNetoKg(20000.0);
        testLot.setQrToken("TOKEN-TEST-" + System.currentTimeMillis());
        testLot.setFechaProduccion(LocalDate.now());
        testLot = lotRepository.save(testLot);
    }

    private void addConformingDoc() {
        Document doc = new Document("DJ_Test.pdf", "DECLARACION_JURADA", "https://s3.amazonaws.com/dj.pdf", "Tester", testLot);
        documentRepository.save(doc);
    }

    private void addConformingCold() {
        ColdChainRecord ccr = new ColdChainRecord();
        ccr.setLote(testLot);
        ccr.setFechaHora(LocalDateTime.now());
        ccr.setTemperaturaCelsius(-22.0);
        ccr.setUbicacionCamara("Camara 1");
        ccr.setResponsableNombre("QA Tester");
        ccr.setEstadoMedicion("NORMAL");
        coldChainRepository.save(ccr);
    }

    @Test
    @DisplayName("CASO 1: REGISTRADO intenta ir directamente a DESPACHADO -> BLOQUEADO (422)")
    void testCaso1_RegistradoDirectoADespachado_Bloqueado() {
        Dispatch dispReq = new Dispatch();
        dispReq.setNumeroContenedor("MSCU-123456-7");
        dispReq.setPrecintoSeguridad("SEAL-999");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            dispatchService.registerDispatch(testLot.getId(), dispReq, "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0") && ex.getReason().contains("Registrado"));
    }

    @Test
    @DisplayName("CASO 2: Lote sin QA intenta certificarse -> BLOQUEADO (422)")
    void testCaso2_LoteSinQA_IntentaCertificarse_Bloqueado() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.requestCertification(testLot.getId(), "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no cuenta con ninguna inspección de calidad"));
    }

    @Test
    @DisplayName("CASO 3: QA = OBSERVADO intenta certificarse -> BLOQUEADO (422)")
    void testCaso3_QAObservado_IntentaCertificarse_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("OBSERVADO");
        qualityInspectionRepository.save(qi);

        testLot.setEstado(LotStatus.OBSERVED.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.requestCertification(testLot.getId(), "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("OBSERVADO") && ex.getReason().contains("dictamen CONFORME"));
    }

    @Test
    @DisplayName("CASO 4: QA = NO_CONFORME intenta avanzar -> BLOQUEADO (422)")
    void testCaso4_QANoConforme_IntentaAvanzar_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("NO_CONFORME");
        qualityInspectionRepository.save(qi);

        testLot.setEstado(LotStatus.OBSERVED.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stateMachineService.transition(testLot, LotStatus.READY_FOR_CERTIFICATION, "qa@exportrace.pe", "QA", "Avanzar");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("NO_CONFORME") && ex.getReason().contains("dictamen CONFORME"));
    }

    @Test
    @DisplayName("CASO 5: QA conforme pero cadena de frío crítica -> BLOQUEADO (422)")
    void testCaso5_QAConforme_FrioCritico_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        ColdChainRecord criticalRecord = new ColdChainRecord();
        criticalRecord.setLote(testLot);
        criticalRecord.setFechaHora(LocalDateTime.now());
        criticalRecord.setTemperaturaCelsius(-8.5); // Crítico
        criticalRecord.setUbicacionCamara("Camara 1");
        criticalRecord.setResponsableNombre("Sensor T-01");
        criticalRecord.setEstadoMedicion("CRITICAL");
        coldChainRepository.save(criticalRecord);

        addConformingDoc();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stateMachineService.validateCanBeReadyForCertification(testLot);
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("desviación crítica de temperatura"));
    }

    @Test
    @DisplayName("CASO 6: QA conforme + frío conforme pero documentación incompleta -> NO LISTO PARA CERTIFICACIÓN (422)")
    void testCaso6_QAConforme_FrioConforme_SinDocumentos_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        addConformingCold();
        // No agregamos documentos a testLot

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stateMachineService.validateCanBeReadyForCertification(testLot);
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("documentación obligatoria"));
    }

    @Test
    @DisplayName("CASO 7: QA conforme + frío conforme + documentación completa -> permitir avanzar a certificación")
    void testCaso7_QAConforme_FrioConforme_DocsCompletos_Permitido() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        addConformingCold();
        addConformingDoc();

        assertDoesNotThrow(() -> {
            stateMachineService.validateCanBeReadyForCertification(testLot);
        });

        SanitaryCertification cert = certificationService.requestCertification(testLot.getId(), "logistica@exportrace.pe");
        assertNotNull(cert);
        assertEquals("SOLICITADO", cert.getEstado());

        Lot updatedLot = lotRepository.findById(testLot.getId()).orElseThrow();
        assertEquals(LotStatus.IN_CERTIFICATION.name(), updatedLot.getEstado());
    }

    @Test
    @DisplayName("CASO 8: Certificación en evaluación intenta marcarse como despacho listo -> BLOQUEADO (422)")
    void testCaso8_CertificacionEnEvaluacion_IntentaDespachoListo_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(testLot);
        cert.setNumeroCertificado("SANIPES-EXP-001");
        cert.setEstado("SOLICITADO"); // En evaluación
        certificationRepository.save(cert);

        testLot.setEstado(LotStatus.IN_CERTIFICATION.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            certificationService.enableForDispatch(testLot.getId(), "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("debe estar CERTIFICADO") || ex.getReason().contains("SOLICITADO"));
    }

    @Test
    @DisplayName("CASO 9: Certificación rechazada intenta despacharse -> BLOQUEADO (422)")
    void testCaso9_CertificacionRechazada_IntentaDespacharse_Bloqueado() {
        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(testLot);
        cert.setNumeroCertificado("SANIPES-EXP-002");
        cert.setEstado("RECHAZADO");
        certificationRepository.save(cert);

        testLot.setEstado(LotStatus.OBSERVED.name());
        lotRepository.save(testLot);

        Dispatch dispReq = new Dispatch();
        dispReq.setNumeroContenedor("MSCU-123456-7");
        dispReq.setPrecintoSeguridad("SEAL-999");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            dispatchService.registerDispatch(testLot.getId(), dispReq, "logistica@exportrace.pe");
        });

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0") && (ex.getReason().contains("Certificado Sanitario") || ex.getReason().contains("Observado")));
    }

    @Test
    @DisplayName("CASO 10: Certificado válido + QA conforme + frío conforme + documentos completos -> permitir transición hacia LISTO_PARA_DESPACHO")
    void testCaso10_CertificadoValido_PermitirListoParaDespacho() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        addConformingCold();
        addConformingDoc();

        SanitaryCertification cert = new SanitaryCertification();
        cert.setLote(testLot);
        cert.setNumeroCertificado("CS-2026-994821");
        cert.setEstado("APROBADO");
        certificationRepository.save(cert);

        testLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(testLot);

        Lot dispatchReadyLot = certificationService.enableForDispatch(testLot.getId(), "logistica@exportrace.pe");
        assertNotNull(dispatchReadyLot);
        assertEquals(LotStatus.READY_FOR_DISPATCH.name(), dispatchReadyLot.getEstado());
    }

    @Test
    @DisplayName("CASO 11: Lote DESPACHADO intenta volver a Producción -> BLOQUEADO (409)")
    void testCaso11_LoteDespachado_IntentaVolverAProduccion_Bloqueado() {
        testLot.setEstado(LotStatus.DISPATCHED.name());
        lotRepository.save(testLot);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stateMachineService.transition(testLot, LotStatus.REGISTERED, "admin@exportrace.pe", "ADMIN", "Revertir a produccion");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("DESPACHADO") && ex.getReason().contains("estado terminal inmutable"));
    }

    @Test
    @DisplayName("CASO 12: Lote certificado intenta modificar una inspección QA histórica -> BLOQUEADO (409)")
    void testCaso12_LoteCertificado_IntentaModificarQAHistorica_Bloqueado() {
        QualityInspection qi = new QualityInspection();
        qi.setLote(testLot);
        qi.setInspectorNombre("Dra. Quispe");
        qi.setResultadoOrganoleptico("CONFORME");
        qualityInspectionRepository.save(qi);

        testLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(testLot);

        QAInspectionDTO editReq = new QAInspectionDTO();
        editReq.setOrganolepticResult("OBSERVADO");
        editReq.setObservations("Intento de modificación no autorizada de inspección histórica");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.saveInspection(testLot.getId(), editReq, "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0") && ex.getReason().contains("inspección QA"));
    }
}
