package com.exportrace;

import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import com.exportrace.service.FileStorageService;
import com.exportrace.service.QualityService;
import com.exportrace.service.impl.PersistentFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QaEvidenceServiceTest {

    @Autowired
    private QualityService qualityService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private QualityInspectionRepository inspectionRepository;

    @Autowired
    private QaEvidenceRepository evidenceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @TempDir
    Path tempUploadDir;

    private Lot testLot;
    private QualityInspection testInspection;

    // Standard valid JPEG header bytes (FF D8 FF E0 ...)
    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
            (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08,
            (byte) 0xFF, (byte) 0xD9
    };

    // Standard valid PNG header bytes (89 50 4E 47 0D 0A 1A 0A ...)
    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    @BeforeEach
    public void setUp() {
        // Point file storage service to temp directory for clean test isolation
        PersistentFileStorageService persistentStorage = new PersistentFileStorageService(tempUploadDir.toString());
        ReflectionTestUtils.setField(qualityService, "fileStorageService", persistentStorage);

        Product product = new Product("PROD-TEST-POTA", "Pota Congelada", "Dosidicus gigas", "Test Product");
        product.setTipoConservacion("CONGELADO");
        product = productRepository.save(product);

        testLot = new Lot();
        testLot.setCodigo("LOT-EVID-TEST-" + System.currentTimeMillis());
        testLot.setProducto(product);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setQrToken(UUID.randomUUID().toString());
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setFechaVencimiento(LocalDate.now().plusMonths(12));
        testLot = lotRepository.save(testLot);

        testInspection = new QualityInspection();
        testInspection.setLote(testLot);
        testInspection.setInspectorNombre("Dra. María Elena Quispe");
        testInspection.setResultadoOrganoleptico("CONFORME");
        testInspection = inspectionRepository.save(testInspection);
    }

    @Test
    @DisplayName("EV-01: Carga exitosa de evidencia fotográfica con metadatos y cálculo SHA-256")
    public void testUploadEvidence_Success_ValidImageAndChecksum() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "muestra_organoleptica.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        QaEvidenceDTO result = qualityService.uploadEvidence(
                testInspection.getId(),
                file,
                "Corte transversal de muestra",
                "qa@exportrace.pe"
        );

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("muestra_organoleptica.jpg", result.getOriginalFileName());
        assertEquals("image/jpeg", result.getMimeType());
        assertEquals((long) VALID_JPEG_BYTES.length, result.getFileSize());
        assertNotNull(result.getSha256());
        assertEquals(64, result.getSha256().length()); // Valid SHA-256 hex length
        assertTrue(result.getActive());

        // Verify entity in repository
        QaEvidence savedEntity = evidenceRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedEntity);
        assertEquals(result.getSha256(), savedEntity.getSha256());
        assertTrue(savedEntity.getStoragePath().contains("evidence/lot_" + testLot.getId()));
    }

    @Test
    @DisplayName("EV-A: Mismo nombre original almacenado con UUID diferentes sin colisión ni sobrescritura")
    public void testUploadEvidence_SameOriginalFileName_StoredWithDifferentUUIDs() {
        MockMultipartFile file1 = new MockMultipartFile("file", "foto.jpg", "image/jpeg", VALID_JPEG_BYTES);
        MockMultipartFile file2 = new MockMultipartFile("file", "foto.jpg", "image/jpeg", VALID_JPEG_BYTES);

        QaEvidenceDTO ev1 = qualityService.uploadEvidence(testInspection.getId(), file1, "Foto 1", "qa@exportrace.pe");
        QaEvidenceDTO ev2 = qualityService.uploadEvidence(testInspection.getId(), file2, "Foto 2", "qa@exportrace.pe");

        assertNotEquals(ev1.getId(), ev2.getId());

        QaEvidence ent1 = evidenceRepository.findById(ev1.getId()).orElseThrow();
        QaEvidence ent2 = evidenceRepository.findById(ev2.getId()).orElseThrow();

        // Stored physical file names must be completely different UUIDs
        assertNotEquals(ent1.getStoredFileName(), ent2.getStoredFileName());
        assertNotEquals(ent1.getStoragePath(), ent2.getStoragePath());

        // Both original names preserved
        assertEquals("foto.jpg", ent1.getOriginalFileName());
        assertEquals("foto.jpg", ent2.getOriginalFileName());
    }

    @Test
    @DisplayName("EV-B: Sanitización de nombres con Path Traversal (../../etc/passwd.jpg)")
    public void testUploadEvidence_PathTraversalFilename_SanitizedSafely() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        QaEvidenceDTO result = qualityService.uploadEvidence(testInspection.getId(), file, "Test Traversal", "qa@exportrace.pe");
        assertNotNull(result);

        QaEvidence saved = evidenceRepository.findById(result.getId()).orElseThrow();
        assertFalse(saved.getStoragePath().contains(".."));
        assertFalse(saved.getStoredFileName().contains(".."));
    }

    @Test
    @DisplayName("EV-04: Rechazo inmediato de archivo vacío (0 bytes)")
    public void testUploadEvidence_RejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.uploadEvidence(testInspection.getId(), emptyFile, "Vacio", "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("EV-02: Rechazo de extensiones no permitidas (.exe, .sh, .php)")
    public void testUploadEvidence_RejectsInvalidExtension() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "virus.exe", "image/jpeg", VALID_JPEG_BYTES);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.uploadEvidence(testInspection.getId(), exeFile, "Executable", "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("EV-03: Rechazo por falsificación de extensión y Magic Bytes no correspondientes")
    public void testUploadEvidence_RejectsSpoofedMimeType_MagicBytesMismatch() {
        // Plain text file pretending to be a JPG
        byte[] textBytes = "This is a plain text file disguised as a JPEG image".getBytes();
        MockMultipartFile spoofedFile = new MockMultipartFile("file", "malware.jpg", "image/jpeg", textBytes);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.uploadEvidence(testInspection.getId(), spoofedFile, "Spoofed", "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no corresponde a una imagen válida"));
    }

    @Test
    @DisplayName("EV-C: Rechazo de archivo de imagen con encabezado corrupto (< 4 bytes)")
    public void testUploadEvidence_RejectsCorruptHeader() {
        byte[] corruptBytes = new byte[]{(byte) 0xFF, (byte) 0xD8}; // only 2 bytes
        MockMultipartFile corruptFile = new MockMultipartFile("file", "corrupt.jpg", "image/jpeg", corruptBytes);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.uploadEvidence(testInspection.getId(), corruptFile, "Corrupt", "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("EV-F / EV-10: Inmutabilidad de evidencias post-certificación (HTTP 409 Conflict)")
    public void testEvidenceImmutability_BlockedWhenLotCertifiedOrDispatched() {
        // First upload evidence when lot is REGISTERED
        MockMultipartFile file = new MockMultipartFile("file", "foto_valida.jpg", "image/jpeg", VALID_JPEG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "Foto Pre-Cert", "qa@exportrace.pe");

        // Transition lot to CERTIFIED
        testLot.setEstado(LotStatus.CERTIFIED.name());
        testLot = lotRepository.save(testLot);

        // 1. Attempt to upload new evidence on certified lot -> 409 CONFLICT
        MockMultipartFile newFile = new MockMultipartFile("file", "foto_nueva.jpg", "image/jpeg", VALID_JPEG_BYTES);
        ResponseStatusException uploadEx = assertThrows(ResponseStatusException.class, () ->
                qualityService.uploadEvidence(testInspection.getId(), newFile, "Nueva foto", "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.CONFLICT, uploadEx.getStatusCode());
        assertTrue(uploadEx.getReason().contains("BLOQUEO P0-C"));

        // 2. Attempt to delete existing evidence on certified lot -> 409 CONFLICT
        ResponseStatusException deleteEx = assertThrows(ResponseStatusException.class, () ->
                qualityService.deleteEvidence(testInspection.getId(), ev.getId(), "qa@exportrace.pe")
        );
        assertEquals(HttpStatus.CONFLICT, deleteEx.getStatusCode());
        assertTrue(deleteEx.getReason().contains("BLOQUEO P0-C"));
    }

    @Test
    @DisplayName("EV-11: Desactivación lógica (Soft-Delete) y registro en pista de auditoría")
    public void testDeleteEvidence_SoftDelete_SuccessInActiveLot() {
        MockMultipartFile file = new MockMultipartFile("file", "borrar.jpg", "image/jpeg", VALID_JPEG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "Por borrar", "qa@exportrace.pe");

        // Delete evidence
        qualityService.deleteEvidence(testInspection.getId(), ev.getId(), "qa@exportrace.pe");

        // Check in DB that active is false
        QaEvidence entity = evidenceRepository.findById(ev.getId()).orElseThrow();
        assertFalse(entity.getActive());

        // Should not be returned in active list
        List<QaEvidenceDTO> activeList = qualityService.getEvidencesByInspectionId(testInspection.getId());
        assertTrue(activeList.stream().noneMatch(e -> e.getId().equals(ev.getId())));

        // Verify audit log
        List<AuditLog> logs = auditLogRepository.findByActionOrderByCreatedAtDesc("QA_EVIDENCE_DEACTIVATED");
        assertFalse(logs.isEmpty());
        assertEquals("EXITOSO", logs.get(0).getResult());
    }

    @Test
    @DisplayName("EV-07: Carga y verificación de recurso binario por streaming protegido")
    public void testLoadEvidenceResource_SuccessAndVerification() {
        MockMultipartFile file = new MockMultipartFile("file", "streaming_test.png", "image/png", VALID_PNG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "PNG Test", "qa@exportrace.pe");

        Resource resource = qualityService.loadEvidenceResource(ev.getId(), true);
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    @DisplayName("EV-G / EV-12: Detección de adulteración externa del archivo físico (SHA-256 Mismatch -> 409)")
    public void testLoadEvidenceResource_TamperedFile_DetectsIntegrityFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "original.jpg", "image/jpeg", VALID_JPEG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "Original", "qa@exportrace.pe");

        QaEvidence entity = evidenceRepository.findById(ev.getId()).orElseThrow();
        Path physicalFile = tempUploadDir.resolve(entity.getStoragePath());

        // Tamper with file on disk by appending arbitrary bytes
        Files.write(physicalFile, new byte[]{0x00, 0x01, 0x02, 0x03}, java.nio.file.StandardOpenOption.APPEND);

        // Attempt to load with integrity verification -> throws CONFLICT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.loadEvidenceResource(ev.getId(), true)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Error de integridad"));

        // Check that audit log recorded the integrity failure
        List<AuditLog> auditLogs = auditLogRepository.findByActionOrderByCreatedAtDesc("QA_EVIDENCE_INTEGRITY_ERROR");
        assertFalse(auditLogs.isEmpty());
        assertEquals("FALLIDO", auditLogs.get(0).getResult());
    }

    @Test
    @DisplayName("EV-D: Metadata presente pero archivo físico eliminado -> Error controlado 404")
    public void testLoadEvidenceResource_MissingPhysicalFile_ControlledError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "disappearing.jpg", "image/jpeg", VALID_JPEG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "Disappearing", "qa@exportrace.pe");

        QaEvidence entity = evidenceRepository.findById(ev.getId()).orElseThrow();
        Path physicalFile = tempUploadDir.resolve(entity.getStoragePath());

        // Physically delete file behind the scenes
        Files.deleteIfExists(physicalFile);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                qualityService.loadEvidenceResource(ev.getId(), false)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("EV-H / OM-05: Verificación de persistencia entre reinicios del servicio de almacenamiento")
    public void testStorageServiceRestart_EvidencePersistsAcrossServiceReinitialization() throws Exception {
        // Step 1: Upload evidence using initial service
        MockMultipartFile file = new MockMultipartFile("file", "persistence_sample.jpg", "image/jpeg", VALID_JPEG_BYTES);
        QaEvidenceDTO ev = qualityService.uploadEvidence(testInspection.getId(), file, "Persistencia OM-05", "qa@exportrace.pe");

        String expectedSha256 = ev.getSha256();
        Long evidenceId = ev.getId();

        // Step 2: Simulate complete server / container restart by destroying and recreating the storage service
        PersistentFileStorageService restartedStorageService = new PersistentFileStorageService(tempUploadDir.toString());
        ReflectionTestUtils.setField(qualityService, "fileStorageService", restartedStorageService);

        // Step 3: Retrieve evidence from DB and disk via newly initialized service
        Resource reloadedResource = qualityService.loadEvidenceResource(evidenceId, true);
        assertNotNull(reloadedResource);
        assertTrue(reloadedResource.exists());

        // Step 4: Verify SHA-256 checksum after reinitialization
        try (InputStream is = reloadedResource.getInputStream()) {
            String recalculatedSha256 = restartedStorageService.calculateSha256(is);
            assertEquals(expectedSha256, recalculatedSha256);
        }
    }
}
