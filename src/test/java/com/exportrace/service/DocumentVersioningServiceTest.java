package com.exportrace.service;

import com.exportrace.dto.LotDocumentDTO;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DocumentVersioningServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Lot testLot;
    private Product testProduct;

    // Helper: Valid minimal PDF bytes (%PDF-1.4 ...)
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\n%âãÏÓ\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\nxref\n0 2\n0000000000 65535 f \n0000000015 00000 n \ntrailer<</Size 2/Root 1 0 R>>\nstartxref\n68\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);

    // Helper: Spoofed Executable bytes (MZ Header)
    private static final byte[] SPOOFED_EXE_BYTES = new byte[] { 0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00 };

    // Helper: Plain text pretending to be PDF
    private static final byte[] PLAIN_TEXT_BYTES = "Hola, este es un archivo de texto plano".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setCodigo("PROD-DOC-TEST");
        testProduct.setNombre("Langostino Entero Congelado");
        testProduct.setTipoConservacion("CONGELADO");
        testProduct = productRepository.save(testProduct);

        testLot = new Lot();
        testLot.setCodigo("EXP-DOC-" + System.currentTimeMillis());
        testLot.setProducto(testProduct);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setPesoNetoKg(10000.0);
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setQrToken("QR-DOC-" + System.currentTimeMillis());
        testLot = lotRepository.save(testLot);
    }

    @Test
    @DisplayName("CASO 10.1: Primer documento subido -> Version = 1, active = true")
    void testDocumentUpload_FirstVersion_SetsVersion1() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "declaracion_jurada_v1.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        LotDocumentDTO result = documentService.uploadDocumentFile(
                testLot.getId(),
                file,
                "DECLARACION_JURADA",
                "produccion@exportrace.pe",
                "PRODUCCION"
        );

        assertNotNull(result);
        assertEquals(1, result.getVersion());
        assertTrue(result.getActive());
        assertEquals("DECLARACION_JURADA", result.getType());
        assertNotNull(result.getSha256());

        List<Document> docs = documentRepository.findByLoteIdAndTipoOrderByVersionDesc(testLot.getId(), "DECLARACION_JURADA");
        assertEquals(1, docs.size());
        assertEquals(1, docs.get(0).getVersion());
        assertTrue(docs.get(0).getActive());
    }

    @Test
    @DisplayName("CASO 10.2: Segunda versión del mismo tipo -> Version = 2, conserva historial completo")
    void testDocumentUpload_SecondVersion_SetsVersion2_AndPreservesHistory() {
        MockMultipartFile file1 = new MockMultipartFile("file", "dj_v1.pdf", "application/pdf", VALID_PDF_BYTES);
        documentService.uploadDocumentFile(testLot.getId(), file1, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");

        MockMultipartFile file2 = new MockMultipartFile("file", "dj_v2.pdf", "application/pdf", VALID_PDF_BYTES);
        LotDocumentDTO result2 = documentService.uploadDocumentFile(testLot.getId(), file2, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");

        assertNotNull(result2);
        assertEquals(2, result2.getVersion());
        assertTrue(result2.getActive());

        // Verificar que AMBAS versiones existen en la BD (historial inmutable)
        List<Document> docs = documentRepository.findByLoteIdAndTipoOrderByVersionDesc(testLot.getId(), "DECLARACION_JURADA");
        assertEquals(2, docs.size());
        assertEquals(2, docs.get(0).getVersion());
        assertTrue(docs.get(0).getActive()); // Más reciente activo
        assertEquals(1, docs.get(1).getVersion());
        assertFalse(docs.get(1).getActive()); // Versión previa archivada
    }

    @Test
    @DisplayName("CASO 10.3: Distintos tipos de documento mantienen secuencias de versión independientes")
    void testDocumentUpload_DifferentTypes_IndependentSequences() {
        MockMultipartFile djFile1 = new MockMultipartFile("file", "dj_1.pdf", "application/pdf", VALID_PDF_BYTES);
        documentService.uploadDocumentFile(testLot.getId(), djFile1, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");

        MockMultipartFile djFile2 = new MockMultipartFile("file", "dj_2.pdf", "application/pdf", VALID_PDF_BYTES);
        documentService.uploadDocumentFile(testLot.getId(), djFile2, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");

        MockMultipartFile certFile = new MockMultipartFile("file", "cert_origen.pdf", "application/pdf", VALID_PDF_BYTES);
        LotDocumentDTO certResult = documentService.uploadDocumentFile(testLot.getId(), certFile, "CERTIFICADO_ORIGEN", "qa@exportrace.pe", "QA");

        assertEquals(1, certResult.getVersion()); // CERTIFICADO_ORIGEN inicia en v1
        List<Document> djDocs = documentRepository.findByLoteIdAndTipoOrderByVersionDesc(testLot.getId(), "DECLARACION_JURADA");
        assertEquals(2, djDocs.size());
        assertEquals(2, djDocs.get(0).getVersion());
    }

    @Test
    @DisplayName("CASO 10.4: Lote CERTIFICADO bloquea subida de documentos -> HTTP 409 Conflict")
    void testDocumentUpload_CertifiedLot_Blocked409() {
        testLot.setEstado(LotStatus.CERTIFIED.name());
        lotRepository.save(testLot);

        MockMultipartFile file = new MockMultipartFile("file", "dj.pdf", "application/pdf", VALID_PDF_BYTES);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), file, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0") && ex.getReason().contains("inmutabilidad"));
    }

    @Test
    @DisplayName("CASO 10.5: Lote DESPACHADO bloquea subida de documentos -> HTTP 409 Conflict")
    void testDocumentUpload_DispatchedLot_Blocked409() {
        testLot.setEstado(LotStatus.DISPATCHED.name());
        lotRepository.save(testLot);

        MockMultipartFile file = new MockMultipartFile("file", "dj.pdf", "application/pdf", VALID_PDF_BYTES);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), file, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("BLOQUEO P0"));
    }

    @Test
    @DisplayName("CASO 11.1: Archivo con Magic Bytes PDF válidos (%PDF-) -> Aceptado y calcula SHA-256")
    void testDocumentUpload_ValidPdfMagicBytes_Accepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "informe_ensayo.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        LotDocumentDTO dto = documentService.uploadDocumentFile(testLot.getId(), file, "INFORME_ENSAYO", "qa@exportrace.pe", "QA");

        assertNotNull(dto);
        assertNotNull(dto.getSha256());
        assertEquals(64, dto.getSha256().length()); // SHA-256 hex string length
    }

    @Test
    @DisplayName("CASO 11.2: Archivo ejecutable camuflado como PDF (MZ Header) -> Rechazado con HTTP 400")
    void testDocumentUpload_SpoofedExecutableAsPdf_Rejected400() {
        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "malware_disguised.pdf",
                "application/pdf",
                SPOOFED_EXE_BYTES
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), spoofedFile, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("PDF válido") || ex.getReason().contains("mágica"));
    }

    @Test
    @DisplayName("CASO 11.3: Archivo de texto plano con extensión .pdf -> Rechazado con HTTP 400")
    void testDocumentUpload_PlainTextAsPdf_Rejected400() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "falso_pdf.pdf",
                "application/pdf",
                PLAIN_TEXT_BYTES
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), textFile, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("PDF válido") || ex.getReason().contains("mágica"));
    }

    @Test
    @DisplayName("CASO 11.4: Archivo vacío (0 bytes) -> Rechazado con HTTP 400")
    void testDocumentUpload_EmptyFile_Rejected400() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "vacio.pdf",
                "application/pdf",
                new byte[0]
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), emptyFile, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("vacío"));
    }

    @Test
    @DisplayName("CASO 11.5: Extensión no permitida (.exe / .docx) -> Rechazado con HTTP 400")
    void testDocumentUpload_InvalidExtension_Rejected400() {
        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "documento.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                VALID_PDF_BYTES
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            documentService.uploadDocumentFile(testLot.getId(), docxFile, "DECLARACION_JURADA", "qa@exportrace.pe", "QA");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains(".pdf"));
    }

    @Test
    @DisplayName("CASO 10/11.6: Auditoría forense inmutable generada al subir documento")
    void testDocumentUpload_EmitsAuditLog() {
        MockMultipartFile file = new MockMultipartFile("file", "audit_test.pdf", "application/pdf", VALID_PDF_BYTES);
        LotDocumentDTO dto = documentService.uploadDocumentFile(testLot.getId(), file, "INFORME_ENSAYO", "auditor@exportrace.pe", "QA");

        List<AuditLog> audits = auditLogRepository.findByActionOrderByCreatedAtDesc("DOCUMENT_UPLOADED");
        assertTrue(audits.stream().anyMatch(a -> a.getEntityId() != null && a.getEntityId().equals(dto.getId())));
    }
}
