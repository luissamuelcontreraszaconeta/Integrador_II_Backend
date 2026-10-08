package com.exportrace.service;

import com.exportrace.dto.CreateReinspectionRequest;
import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QualityReinspectionServiceTest {

    @Autowired
    private QualityService qualityService;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private QaEvidenceRepository qaEvidenceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    private Product testProduct;
    private Lot testLot;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.findByCodigo("PRD-LGT-QA").orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("PRD-LGT-QA");
            p.setNombre("Langostino QA Test");
            p.setNombreComercial("Langostino QA");
            p.setNombreCientifico("Penaeus vannamei");
            p.setTempOptimaMin(-25.0);
            p.setTempOptimaMax(-18.0);
            return productRepository.save(p);
        });

        testLot = new Lot();
        testLot.setCodigo("EXP-QA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setQrToken("QR-QA-" + UUID.randomUUID().toString().substring(0, 8));
        testLot.setProducto(testProduct);
        testLot.setCantidadEmpaques(500);
        testLot.setTipoEmpaque("Cajas Master 10kg");
        testLot.setPesoNetoKg(5000.0);
        testLot.setPlantaProcesamiento("Planta Paita");
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setFechaVencimiento(LocalDate.now().plusYears(2));
        testLot.setVersion(0L);
        testLot = lotRepository.save(testLot);

        // Required digital documents for Ready-for-certification gate
        Document dj = new Document("DJ_Origen.pdf", "DECLARACION_JURADA", "https://exportrace.pe/docs/dj.pdf", "QA Tester", testLot);
        documentRepository.save(dj);

        Document tp = new Document("Ticket_Pesaje.pdf", "TICKET_PESAJE", "https://exportrace.pe/docs/tp.pdf", "QA Tester", testLot);
        documentRepository.save(tp);

        // Required cold chain record
        ColdChainRecord ccr = new ColdChainRecord();
        ccr.setLote(testLot);
        ccr.setFechaHora(LocalDateTime.now());
        ccr.setTemperaturaCelsius(-22.0);
        ccr.setUbicacionCamara("Cámara #01");
        ccr.setResponsableNombre("QA Tester");
        ccr.setEstadoMedicion("NORMAL");
        coldChainRepository.save(ccr);
    }

    // =========================================================================
    // TC-QA-01: Primera inspección OBSERVADO se guarda como inspección #1
    // =========================================================================
    @Test
    @DisplayName("TC-QA-01: Primera inspección OBSERVADO se guarda con secuencia #1 y transiciona lote a OBSERVED")
    void testTC_QA_01_FirstInspection_SavedAsSequence1_Observed() {
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setInspectorName("Dr. Carlos Mendoza");
        initialDto.setOrganolepticResult("OBSERVADO");
        initialDto.setAppearance("REGULAR");
        initialDto.setColor("LIGERA_DESCOLORACION");
        initialDto.setTexture("BLANDA");
        initialDto.setSmell("NEUTRO");
        initialDto.setParasiteCheck("AUSENCIA");
        initialDto.setObservations("Ligera descoloración en muestra #04");

        QAInspectionDTO saved = qualityService.saveInspection(testLot.getId(), initialDto, "inspector1@exportrace.pe");

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getInspectionNumber());
        assertEquals("OBSERVADO", saved.getOrganolepticResult());
        assertEquals("Dr. Carlos Mendoza", saved.getInspectorName());

        // Verify lot state transition to OBSERVED
        Lot updatedLot = lotRepository.findById(testLot.getId()).orElseThrow();
        assertEquals(LotStatus.OBSERVED.name(), updatedLot.getEstado());

        // Verify database records
        List<QualityInspection> inspections = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(testLot.getId());
        assertEquals(1, inspections.size());
        assertEquals(1, inspections.get(0).getNumeroInspeccion());
    }

    // =========================================================================
    // TC-QA-02: Reinspección después de OBSERVADO crea inspección #2 e inspección #1 intacta
    // =========================================================================
    @Test
    @DisplayName("TC-QA-02: Reinspección crea nueva fila con secuencia #2 y mantiene intacta la inspección #1")
    void testTC_QA_02_ReinspectionAfterObserved_CreatesSequence2_PreservesSequence1() {
        // 1. Initial inspection #1 OBSERVADO
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setInspectorName("Dr. Carlos Mendoza");
        initialDto.setOrganolepticResult("OBSERVADO");
        initialDto.setObservations("Defecto leve en textura");
        QAInspectionDTO insp1 = qualityService.saveInspection(testLot.getId(), initialDto, "inspector1@exportrace.pe");

        // 2. Reinspection #2
        CreateReinspectionRequest reReq = new CreateReinspectionRequest();
        reReq.setMotivoReinspeccion("Subsanación de defecto leve tras recirculación y acondicionamiento");
        reReq.setResultadoOrganoleptico("OBSERVADO");
        reReq.setInspectorName("Dra. María Elena Quispe");
        reReq.setAppearance("BUENA");
        reReq.setTexture("FIRM");
        reReq.setObservations("Mejora notable pero requiere 2da evaluación de lote");

        QAInspectionDTO insp2 = qualityService.createReinspection(testLot.getId(), reReq, "inspector2@exportrace.pe");

        assertNotNull(insp2);
        assertNotEquals(insp1.getId(), insp2.getId(), "Debe ser una entidad distinta con nuevo ID");
        assertEquals(2, insp2.getInspectionNumber(), "La reinspección debe tener secuencia #2");
        assertEquals("Dra. María Elena Quispe", insp2.getInspectorName());
        assertEquals("Subsanación de defecto leve tras recirculación y acondicionamiento", insp2.getReinspectionReason());

        // 3. Verify that Inspection #1 remains completely intact in database
        List<QualityInspection> history = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(testLot.getId());
        assertEquals(2, history.size());

        QualityInspection row1 = history.get(0);
        assertEquals(1, row1.getNumeroInspeccion());
        assertEquals("OBSERVADO", row1.getResultadoOrganoleptico());
        assertEquals("Dr. Carlos Mendoza", row1.getInspectorNombre());
        assertEquals("Defecto leve en textura", row1.getObservaciones());

        QualityInspection row2 = history.get(1);
        assertEquals(2, row2.getNumeroInspeccion());
        assertEquals("OBSERVADO", row2.getResultadoOrganoleptico());
        assertEquals("Dra. María Elena Quispe", row2.getInspectorNombre());
        assertEquals("Subsanación de defecto leve tras recirculación y acondicionamiento", row2.getMotivoReinspeccion());
    }

    // =========================================================================
    // TC-QA-03: Inspección #2 = CONFORME -> historial contiene #1 y #2 y lote queda READY_FOR_CERTIFICATION
    // =========================================================================
    @Test
    @DisplayName("TC-QA-03: Reinspección #2 CONFORME habilita lote para READY_FOR_CERTIFICATION conservando #1")
    void testTC_QA_03_ReinspectionConforme_EnablesReadyForCertification() {
        // Initial inspection #1
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setInspectorName("Dr. Carlos Mendoza");
        initialDto.setOrganolepticResult("OBSERVADO");
        initialDto.setObservations("Color irregular");
        qualityService.saveInspection(testLot.getId(), initialDto, "inspector1@exportrace.pe");

        // Reinspection #2 CONFORME
        CreateReinspectionRequest reReq = new CreateReinspectionRequest();
        reReq.setMotivoReinspeccion("Reclasificación de lote tras selección organoléptica");
        reReq.setResultadoOrganoleptico("CONFORME");
        reReq.setInspectorName("Dra. Rosa Torres");
        reReq.setAppearance("EXCELENTE");
        reReq.setColor("CONFORME");
        reReq.setTexture("FIRM");
        reReq.setSmell("FRESCO");
        reReq.setParasiteCheck("AUSENCIA");
        reReq.setObservations("Lote 100% conforme para exportación");

        QAInspectionDTO insp2 = qualityService.createReinspection(testLot.getId(), reReq, "inspector2@exportrace.pe");

        assertEquals(2, insp2.getInspectionNumber());
        assertEquals("CONFORME", insp2.getOrganolepticResult());

        // Verify lot status transitioned to READY_FOR_CERTIFICATION
        Lot lot = lotRepository.findById(testLot.getId()).orElseThrow();
        assertEquals(LotStatus.READY_FOR_CERTIFICATION.name(), lot.getEstado());

        // Verify history endpoint / service
        List<QAInspectionDTO> history = qualityService.getInspectionHistoryByLotId(testLot.getId());
        assertEquals(2, history.size());
        assertEquals("OBSERVADO", history.get(0).getOrganolepticResult());
        assertEquals("CONFORME", history.get(1).getOrganolepticResult());
    }

    // =========================================================================
    // TC-QA-04: Segunda reinspección crea inspección #3
    // =========================================================================
    @Test
    @DisplayName("TC-QA-04: Segunda reinspección permitida crea inspección con secuencia #3")
    void testTC_QA_04_SecondReinspection_CreatesSequence3() {
        // #1
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("OBSERVADO");
        qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        // #2
        CreateReinspectionRequest req2 = new CreateReinspectionRequest();
        req2.setMotivoReinspeccion("Primer reproceso térmico");
        req2.setResultadoOrganoleptico("OBSERVADO");
        qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");

        // #3
        CreateReinspectionRequest req3 = new CreateReinspectionRequest();
        req3.setMotivoReinspeccion("Segundo tratamiento de enfriamiento y estabilización");
        req3.setResultadoOrganoleptico("CONFORME");
        req3.setInspectorName("Dr. Supervisor QA");
        QAInspectionDTO insp3 = qualityService.createReinspection(testLot.getId(), req3, "qa3@exportrace.pe");

        assertEquals(3, insp3.getInspectionNumber());
        assertEquals("CONFORME", insp3.getOrganolepticResult());

        List<QualityInspection> all = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(testLot.getId());
        assertEquals(3, all.size());
        assertEquals(1, all.get(0).getNumeroInspeccion());
        assertEquals(2, all.get(1).getNumeroInspeccion());
        assertEquals(3, all.get(2).getNumeroInspeccion());
    }

    // =========================================================================
    // TC-QA-05: Intentar cuarta inspección (superar máximo de 3) -> BLOQUEADO (HTTP 409)
    // =========================================================================
    @Test
    @DisplayName("TC-QA-05: Intento de 4ta inspección es BLOQUEADO con HTTP 409 y auditoría de límite alcanzado")
    void testTC_QA_05_FourthInspection_BlockedWith409AndAudited() {
        // #1
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("OBSERVADO");
        qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        // #2
        CreateReinspectionRequest req2 = new CreateReinspectionRequest("Motivo 2", "OBSERVADO", "QA 2");
        qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");

        // #3
        CreateReinspectionRequest req3 = new CreateReinspectionRequest("Motivo 3", "OBSERVADO", "QA 3");
        qualityService.createReinspection(testLot.getId(), req3, "qa3@exportrace.pe");

        // #4 Intento no permitido
        CreateReinspectionRequest req4 = new CreateReinspectionRequest("Motivo 4", "CONFORME", "QA 4");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.createReinspection(testLot.getId(), req4, "qa4@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("LÍMITE ALCANZADO"), "Debe indicar que se alcanzó el límite máximo");
        assertTrue(ex.getReason().contains("3 inspecciones"));

        // Verify audit log registered QA_REINSPECTION_LIMIT_REACHED
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundLimitLog = auditLogs.stream()
                .anyMatch(l -> "QA_REINSPECTION_LIMIT_REACHED".equals(l.getAction()));
        assertTrue(foundLimitLog, "Debe registrarse el log de auditoría QA_REINSPECTION_LIMIT_REACHED");
    }

    // =========================================================================
    // TC-QA-06: Reinspección sin motivo -> HTTP 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("TC-QA-06: Reinspección sin motivo obligatorio retorna HTTP 400 Bad Request")
    void testTC_QA_06_ReinspectionMissingReason_ThrowsBadRequest400() {
        // #1
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("OBSERVADO");
        qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        // #2 Blank reason
        CreateReinspectionRequest req2 = new CreateReinspectionRequest("   ", "CONFORME", "QA 2");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El motivo de la reinspección es obligatorio"));
    }

    // =========================================================================
    // TC-QA-07: Reinspección cuando lote ya es CONFORME -> HTTP 409 Conflict
    // =========================================================================
    @Test
    @DisplayName("TC-QA-07: Intento de reinspeccionar un lote con dictamen ya CONFORME retorna HTTP 409 Conflict")
    void testTC_QA_07_ReinspectAlreadyConforme_ThrowsConflict409() {
        // #1 CONFORME
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("CONFORME");
        qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        // #2 Attempt reinspect
        CreateReinspectionRequest req2 = new CreateReinspectionRequest("Reinspección innecesaria", "CONFORME", "QA 2");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ya cuenta con un dictamen CONFORME"));
    }

    // =========================================================================
    // TC-QA-08: Lote CERTIFIED intenta reinspección -> HTTP 409 Conflict
    // =========================================================================
    @Test
    @DisplayName("TC-QA-08: Lote en estado CERTIFIED bloquea cualquier intento de reinspección (HTTP 409)")
    void testTC_QA_08_CertifiedLot_ReinspectionBlockedWith409() {
        testLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(testLot);

        CreateReinspectionRequest req = new CreateReinspectionRequest("Intento en certificado", "CONFORME", "QA 1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.createReinspection(testLot.getId(), req, "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0"));
    }

    // =========================================================================
    // TC-QA-09: Lote DISPATCHED intenta reinspección -> HTTP 409 Conflict
    // =========================================================================
    @Test
    @DisplayName("TC-QA-09: Lote en estado DISPATCHED bloquea cualquier intento de reinspección (HTTP 409)")
    void testTC_QA_09_DispatchedLot_ReinspectionBlockedWith409() {
        testLot.setEstado(LotStatus.DISPATCHED.name());
        lotRepository.save(testLot);

        CreateReinspectionRequest req = new CreateReinspectionRequest("Intento en despachado", "CONFORME", "QA 1");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            qualityService.createReinspection(testLot.getId(), req, "qa@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0"));
    }

    // =========================================================================
    // TC-QA-10: Evidencias de inspección #1 y #2 permanecen separadas
    // =========================================================================
    @Test
    @DisplayName("TC-QA-10: Las evidencias fotográficas quedan asociadas a su inspección específica y no se mezclan")
    void testTC_QA_10_EvidencesSeparatedByInspection() {
        // #1
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("OBSERVADO");
        QAInspectionDTO insp1 = qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        // Upload evidence to Inspection #1
        byte[] imgBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00};
        MockMultipartFile file1 = new MockMultipartFile("file", "foto_insp1.jpg", "image/jpeg", imgBytes);
        QaEvidenceDTO ev1 = qualityService.uploadEvidence(insp1.getId(), file1, "Foto defecto inicial #1", "qa1@exportrace.pe");

        // #2
        CreateReinspectionRequest req2 = new CreateReinspectionRequest("Subsanado", "CONFORME", "QA 2");
        QAInspectionDTO insp2 = qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");

        // Upload evidence to Inspection #2
        MockMultipartFile file2 = new MockMultipartFile("file", "foto_insp2.jpg", "image/jpeg", imgBytes);
        QaEvidenceDTO ev2 = qualityService.uploadEvidence(insp2.getId(), file2, "Foto conformidad #2", "qa2@exportrace.pe");

        // Query evidences by inspection
        List<QaEvidenceDTO> evList1 = qualityService.getEvidencesByInspectionId(insp1.getId());
        List<QaEvidenceDTO> evList2 = qualityService.getEvidencesByInspectionId(insp2.getId());

        assertEquals(1, evList1.size());
        assertEquals(ev1.getId(), evList1.get(0).getId());
        assertEquals("foto_insp1.jpg", evList1.get(0).getOriginalFileName());

        assertEquals(1, evList2.size());
        assertEquals(ev2.getId(), evList2.get(0).getId());
        assertEquals("foto_insp2.jpg", evList2.get(0).getOriginalFileName());
    }

    // =========================================================================
    // TC-QA-11: Restricción de unicidad impide duplicar la misma secuencia
    // =========================================================================
    @Test
    @DisplayName("TC-QA-11: Restricción de unicidad garantiza que cada inspección tenga secuencia única")
    void testTC_QA_11_UniqueSequenceConstraint_PreventsDuplicateSequences() {
        QAInspectionDTO initialDto = new QAInspectionDTO();
        initialDto.setOrganolepticResult("OBSERVADO");
        qualityService.saveInspection(testLot.getId(), initialDto, "qa1@exportrace.pe");

        CreateReinspectionRequest req2 = new CreateReinspectionRequest("Motivo 2", "OBSERVADO", "QA 2");
        qualityService.createReinspection(testLot.getId(), req2, "qa2@exportrace.pe");

        // Verify count is 2 and sequences are distinct
        List<QualityInspection> list = qualityRepository.findByLoteIdOrderByNumeroInspeccionAsc(testLot.getId());
        assertEquals(2, list.size());
        assertNotEquals(list.get(0).getNumeroInspeccion(), list.get(1).getNumeroInspeccion());
    }
}
