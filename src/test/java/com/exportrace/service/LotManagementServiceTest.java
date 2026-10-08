package com.exportrace.service;

import com.exportrace.dto.CreateLotRequest;
import com.exportrace.dto.LotDTO;
import com.exportrace.dto.UpdateLotRequest;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class LotManagementServiceTest {

    @Autowired
    private LotService lotService;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.findByCodigo("PRD-LGT-TEST").orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("PRD-LGT-TEST");
            p.setNombre("Langostino Test");
            p.setNombreComercial("Langostino Entero Test");
            p.setNombreCientifico("Penaeus vannamei");
            p.setTempOptimaMin(-25.0);
            p.setTempOptimaMax(-18.0);
            return productRepository.save(p);
        });
    }

    // =========================================================================
    // CASO 1: DEFENSA EN PROFUNDIDAD CONTRA LOTE DUPLICADO
    // =========================================================================

    @Test
    @DisplayName("CASO 1.1: Registro exitoso de lote cuando el código no existe")
    void testCreateLot_FirstRegistration_Success() {
        String uniqueCode = "EXP-TEST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CreateLotRequest req = new CreateLotRequest();
        req.setCodigo(uniqueCode);
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(500);
        req.setTipoEmpaque("Cajas Master 10kg");
        req.setPesoNetoKg(5000.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        LotDTO result = lotService.createLot(req, "operaciones@exportrace.pe");

        assertNotNull(result);
        assertEquals(uniqueCode, result.getCode());
        assertEquals("REGISTERED", result.getStatus());
        assertNotNull(result.getVersion());
        assertEquals(0L, result.getVersion());

        // Verify history & initial cold chain were created
        Lot savedLot = lotRepository.findByCodigo(uniqueCode).orElseThrow();
        List<LotHistory> history = lotHistoryRepository.findByLoteIdOrderByFechaCambioDesc(savedLot.getId());
        assertFalse(history.isEmpty());

        List<ColdChainRecord> ccr = coldChainRepository.findByLoteIdOrderByFechaHoraDesc(savedLot.getId());
        assertFalse(ccr.isEmpty());
    }

    @Test
    @DisplayName("CASO 1.2: Rechazo con HTTP 409 cuando se intenta registrar un código de lote ya existente")
    void testCreateLot_DuplicateCode_ThrowsConflict409() {
        String duplicateCode = "EXP-DUP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CreateLotRequest req1 = new CreateLotRequest();
        req1.setCodigo(duplicateCode);
        req1.setProducto("PRD-LGT-TEST");
        req1.setCantidadEmpaques(500);
        req1.setTipoEmpaque("Cajas Master 10kg");
        req1.setPesoNetoKg(5000.0);
        req1.setPlantaProcesamiento("Planta Paita");
        req1.setFechaProduccion(LocalDate.now().toString());

        lotService.createLot(req1, "operaciones@exportrace.pe");

        // Intentar registrar el segundo lote con el mismo código
        CreateLotRequest req2 = new CreateLotRequest();
        req2.setCodigo(duplicateCode);
        req2.setProducto("PRD-LGT-TEST");
        req2.setCantidadEmpaques(300);
        req2.setTipoEmpaque("Cajas Master 10kg");
        req2.setPesoNetoKg(3000.0);
        req2.setPlantaProcesamiento("Planta Paita");
        req2.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req2, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ya existe un lote registrado con el código " + duplicateCode));
    }

    @Test
    @DisplayName("CASO 1.3: Auto-generación de código único sin colisiones")
    void testCreateLot_AutoGenerateUniqueCode() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(200);
        req.setTipoEmpaque("Cajas Master 10kg");
        req.setPesoNetoKg(2000.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        LotDTO result = lotService.createLot(req, "operaciones@exportrace.pe");

        assertNotNull(result);
        assertNotNull(result.getCode());
        assertTrue(result.getCode().startsWith("EXP-"));
    }

    // =========================================================================
    // CASO 2: VALIDACIONES ESTRICTAS DE PESO Y CAMPOS OBLIGATORIOS
    // =========================================================================

    @Test
    @DisplayName("CASO 2.1: Rechazo con HTTP 400 cuando el peso neto es cero")
    void testCreateLot_RejectsZeroWeight() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(100);
        req.setTipoEmpaque("Cajas");
        req.setPesoNetoKg(0.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El peso neto es obligatorio y debe ser mayor que cero"));
    }

    @Test
    @DisplayName("CASO 2.2: Rechazo con HTTP 400 cuando el peso neto es negativo")
    void testCreateLot_RejectsNegativeWeight() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(100);
        req.setTipoEmpaque("Cajas");
        req.setPesoNetoKg(-50.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El peso neto es obligatorio y debe ser mayor que cero"));
    }

    @Test
    @DisplayName("CASO 2.3: Rechazo con HTTP 400 cuando el peso neto es nulo")
    void testCreateLot_RejectsNullWeight() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(100);
        req.setTipoEmpaque("Cajas");
        req.setPesoNetoKg(null);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El peso neto es obligatorio y debe ser mayor que cero"));
    }

    @Test
    @DisplayName("CASO 2.4: Rechazo con HTTP 400 cuando la cantidad de empaques es <= 0")
    void testCreateLot_RejectsInvalidQuantity() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(0);
        req.setTipoEmpaque("Cajas");
        req.setPesoNetoKg(100.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("La cantidad de empaques es obligatoria y debe ser mayor que cero"));
    }

    @Test
    @DisplayName("CASO 2.5: Rechazo con HTTP 400 cuando falta la planta de procesamiento")
    void testCreateLot_RejectsMissingPlanta() {
        CreateLotRequest req = new CreateLotRequest();
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(100);
        req.setTipoEmpaque("Cajas");
        req.setPesoNetoKg(1000.0);
        req.setPlantaProcesamiento("   ");
        req.setFechaProduccion(LocalDate.now().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.createLot(req, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("La planta de procesamiento es obligatoria"));
    }

    // =========================================================================
    // CASO 3: CONTROL DE CONCURRENCIA OPTIMISTA EN EDICIÓN
    // =========================================================================

    @Test
    @DisplayName("CASO 3.1: Actualización exitosa cuando las versiones coinciden")
    void testUpdateLot_MatchingVersion_Success() {
        String code = "EXP-UPD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CreateLotRequest req = new CreateLotRequest();
        req.setCodigo(code);
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(500);
        req.setTipoEmpaque("Cajas Master 10kg");
        req.setPesoNetoKg(5000.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        LotDTO created = lotService.createLot(req, "operaciones@exportrace.pe");
        Long lotId = Long.parseLong(created.getId());
        Long initialVersion = created.getVersion();

        UpdateLotRequest updateReq = new UpdateLotRequest();
        updateReq.setVersion(initialVersion);
        updateReq.setPesoNetoKg(6000.0);
        updateReq.setCantidadEmpaques(600);
        updateReq.setPlantaProcesamiento("Planta Paita Modificada");

        LotDTO updated = lotService.updateLot(lotId, updateReq, "operaciones@exportrace.pe");

        assertNotNull(updated);
        assertEquals(600.0, updated.getProduction().getQuantity());
        assertEquals("Planta Paita Modificada", updated.getProduction().getPortOfOrigin());
        assertNotNull(updated.getVersion());
    }

    @Test
    @DisplayName("CASO 3.2: Conflicto de concurrencia (HTTP 409) cuando la versión enviada no coincide")
    void testUpdateLot_VersionMismatch_ThrowsConflict409AndLogsAudit() {
        String code = "EXP-CONF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CreateLotRequest req = new CreateLotRequest();
        req.setCodigo(code);
        req.setProducto("PRD-LGT-TEST");
        req.setCantidadEmpaques(500);
        req.setTipoEmpaque("Cajas Master 10kg");
        req.setPesoNetoKg(5000.0);
        req.setPlantaProcesamiento("Planta Paita");
        req.setFechaProduccion(LocalDate.now().toString());

        LotDTO created = lotService.createLot(req, "operaciones@exportrace.pe");
        Long lotId = Long.parseLong(created.getId());

        // Update 1 modifies the lot and bumps version
        UpdateLotRequest updateReq1 = new UpdateLotRequest();
        updateReq1.setVersion(created.getVersion());
        updateReq1.setPesoNetoKg(5500.0);
        updateReq1.setCantidadEmpaques(550);
        lotService.updateLot(lotId, updateReq1, "usuario_a@exportrace.pe");

        // Update 2 by another user sending stale initial version
        UpdateLotRequest updateReq2 = new UpdateLotRequest();
        updateReq2.setVersion(created.getVersion()); // Stale version
        updateReq2.setPesoNetoKg(5800.0);
        updateReq2.setCantidadEmpaques(580);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.updateLot(lotId, updateReq2, "usuario_b@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("El lote fue modificado por otro usuario"));

        // Verify audit log registered LOT_UPDATE_CONFLICT
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundConflictLog = auditLogs.stream()
                .anyMatch(l -> "LOT_UPDATE_CONFLICT".equals(l.getAction()) && l.getDescription() != null && l.getDescription().contains("Conflicto de concurrencia"));
        assertTrue(foundConflictLog, "Debe registrarse el log de auditoría LOT_UPDATE_CONFLICT");
    }

    @Test
    @DisplayName("CASO 3.3: Inmutabilidad - No permite editar lotes CERTIFICADOS o DESPACHADOS (HTTP 409)")
    void testUpdateLot_CertifiedLot_ThrowsConflict409() {
        Lot lot = new Lot();
        lot.setCodigo("EXP-CERT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        lot.setEstado(LotStatus.CERTIFIED.name());
        lot.setQrToken("QR-CERT-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setProducto(testProduct);
        lot.setCantidadEmpaques(1000);
        lot.setTipoEmpaque("Cajas");
        lot.setPesoNetoKg(10000.0);
        lot.setPlantaProcesamiento("Planta Paita");
        lot.setFechaProduccion(LocalDate.now());
        lot.setFechaVencimiento(LocalDate.now().plusYears(2));
        lot.setVersion(0L);
        lot = lotRepository.save(lot);

        UpdateLotRequest updateReq = new UpdateLotRequest();
        updateReq.setVersion(0L);
        updateReq.setPesoNetoKg(12000.0);

        final Long targetLotId = lot.getId();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            lotService.updateLot(targetLotId, updateReq, "operaciones@exportrace.pe");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0: No se pueden modificar los datos de producción de un lote que ya ha sido certificado"));
    }
}
