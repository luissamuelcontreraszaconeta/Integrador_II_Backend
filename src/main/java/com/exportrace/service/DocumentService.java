package com.exportrace.service;

import com.exportrace.dto.LotDocumentDTO;
import com.exportrace.entity.Document;
import com.exportrace.entity.Lot;
import com.exportrace.entity.LotStatus;
import com.exportrace.repository.DocumentRepository;
import com.exportrace.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AuditService auditService;

    public List<LotDocumentDTO> getDocumentsByLotId(Long lotId) {
        return documentRepository.findByLoteIdOrderByFechaSubidaDesc(lotId).stream()
                .map(LotDocumentDTO::new)
                .toList();
    }

    public List<LotDocumentDTO> getActiveDocumentsByLotId(Long lotId) {
        return documentRepository.findByLoteIdAndActiveTrue(lotId).stream()
                .map(LotDocumentDTO::new)
                .toList();
    }

    public Document getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado con ID: " + documentId));
    }

    /**
     * Carga y versionamiento seguro de documentos PDF con validación de magic bytes (RF-27, BR-P0-003, CASO 10, CASO 11)
     */
    @Transactional
    public LotDocumentDTO uploadDocumentFile(Long lotId, MultipartFile file, String tipo, String userEmail, String userRole) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        validateLotStateForDocumentUpload(lot);

        String docType = tipo != null && !tipo.isBlank() ? tipo.trim().toUpperCase() : "DECLARACION_JURADA";

        // 1. Guardar archivo físico validando Magic Bytes (%PDF-) y calculando SHA-256
        FileStorageService.StoredFile storedFile = fileStorageService.storeDocument(file, "documents/lot_" + lotId);

        // 2. Gestionar versionamiento inmutable
        List<Document> existingVersions = documentRepository.findByLoteIdAndTipoOrderByVersionDesc(lotId, docType);
        int nextVersion = 1;
        if (!existingVersions.isEmpty()) {
            nextVersion = existingVersions.get(0).getVersion() + 1;
            // Archivar versiones previas sin borrarlas
            for (Document prev : existingVersions) {
                prev.setActive(false);
            }
            documentRepository.saveAll(existingVersions);
        }

        // 3. Crear entidad Document
        Document doc = new Document();
        doc.setLote(lot);
        doc.setNombre(storedFile.originalFileName());
        doc.setTipo(docType);
        doc.setVersion(nextVersion);
        doc.setSha256(storedFile.sha256());
        doc.setMimeType("application/pdf");
        doc.setFileSize(storedFile.fileSize());
        doc.setFilePath(storedFile.relativeStoragePath());
        doc.setUrl("/api/documents/download/" + storedFile.storedFileName());
        doc.setActive(true);
        doc.setSubidoPor(userEmail != null ? userEmail : "sistema@exportrace.pe");
        doc.setFechaSubida(LocalDateTime.now());

        Document savedDoc = documentRepository.save(doc);

        // 4. Auditoría forense inmutable
        String auditAction = nextVersion > 1 ? "DOCUMENT_VERSION_CREATED" : "DOCUMENT_UPLOADED";
        auditService.logAction(
                auditAction,
                "DOCUMENTACION",
                "Document",
                savedDoc.getId().toString(),
                "Documento " + docType + " v" + nextVersion + " cargado para lote " + lot.getCodigo() + " (SHA-256: " + storedFile.sha256() + ")",
                userEmail != null ? userEmail : "SYSTEM",
                userRole != null ? userRole : "PRODUCCION"
        );

        return new LotDocumentDTO(savedDoc);
    }

    /**
     * Carga de metadata de documento (compatibilidad REST y fixtures)
     */
    @Transactional
    public LotDocumentDTO uploadDocumentMetadata(Long lotId, Document docReq, String userEmail, String userRole) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        validateLotStateForDocumentUpload(lot);

        String docType = docReq.getTipo() != null && !docReq.getTipo().isBlank() ? docReq.getTipo().trim().toUpperCase() : "DECLARACION_JURADA";

        List<Document> existingVersions = documentRepository.findByLoteIdAndTipoOrderByVersionDesc(lotId, docType);
        int nextVersion = 1;
        if (!existingVersions.isEmpty()) {
            nextVersion = existingVersions.get(0).getVersion() + 1;
            for (Document prev : existingVersions) {
                prev.setActive(false);
            }
            documentRepository.saveAll(existingVersions);
        }

        Document doc = new Document();
        doc.setLote(lot);
        doc.setNombre(docReq.getNombre() != null ? docReq.getNombre() : "documento_" + docType.toLowerCase() + ".pdf");
        doc.setTipo(docType);
        doc.setVersion(nextVersion);
        doc.setUrl(docReq.getUrl() != null ? docReq.getUrl() : "/documents/" + doc.getNombre());
        doc.setSha256(docReq.getSha256() != null ? docReq.getSha256() : "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        doc.setMimeType("application/pdf");
        doc.setFileSize(docReq.getFileSize() != null ? docReq.getFileSize() : 2500000L);
        doc.setActive(true);
        doc.setSubidoPor(userEmail != null ? userEmail : (docReq.getSubidoPor() != null ? docReq.getSubidoPor() : "sistema@exportrace.pe"));
        doc.setFechaSubida(LocalDateTime.now());

        Document savedDoc = documentRepository.save(doc);

        String auditAction = nextVersion > 1 ? "DOCUMENT_VERSION_CREATED" : "DOCUMENT_UPLOADED";
        auditService.logAction(
                auditAction,
                "DOCUMENTACION",
                "Document",
                savedDoc.getId().toString(),
                "Metadata documento " + docType + " v" + nextVersion + " registrada para lote " + lot.getCodigo(),
                userEmail != null ? userEmail : "SYSTEM",
                userRole != null ? userRole : "PRODUCCION"
        );

        return new LotDocumentDTO(savedDoc);
    }

    public Resource downloadDocumentFile(String storedFileName) {
        return fileStorageService.loadFileAsResource(storedFileName, "documents");
    }

    private void validateLotStateForDocumentUpload(Lot lot) {
        LotStatus current = LotStatus.fromString(lot.getEstado());
        if (current == LotStatus.CERTIFIED || current == LotStatus.READY_FOR_DISPATCH || current == LotStatus.DISPATCHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: No se pueden adjuntar ni modificar documentos para un lote en estado " + current.getEtiqueta() + ". El expediente está bloqueado para garantizar inmutabilidad.");
        }
        if (current == LotStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "BLOQUEO P0: El lote " + lot.getCodigo() + " se encuentra ANULADO.");
        }
    }
}
