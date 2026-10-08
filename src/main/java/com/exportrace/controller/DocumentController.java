package com.exportrace.controller;

import com.exportrace.dto.LotDocumentDTO;
import com.exportrace.entity.Document;
import com.exportrace.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<LotDocumentDTO>> getDocumentsByLotId(@PathVariable Long lotId) {
        return ResponseEntity.ok(documentService.getDocumentsByLotId(lotId));
    }

    @GetMapping("/lot/{lotId}/active")
    public ResponseEntity<List<LotDocumentDTO>> getActiveDocumentsByLotId(@PathVariable Long lotId) {
        return ResponseEntity.ok(documentService.getActiveDocumentsByLotId(lotId));
    }

    @PostMapping(value = "/lot/{lotId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LotDocumentDTO> uploadDocumentFile(
            @PathVariable Long lotId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tipo", required = false, defaultValue = "DECLARACION_JURADA") String tipo,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "sistema@exportrace.pe";
        String role = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "PRODUCCION";
        return ResponseEntity.ok(documentService.uploadDocumentFile(lotId, file, tipo, email, role));
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<LotDocumentDTO> uploadDocumentMetadata(
            @PathVariable Long lotId,
            @RequestBody Document docReq,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "sistema@exportrace.pe";
        String role = authentication != null && !authentication.getAuthorities().isEmpty()
                ? authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
                : "PRODUCCION";
        return ResponseEntity.ok(documentService.uploadDocumentMetadata(lotId, docReq, email, role));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String fileName) {
        Resource resource = documentService.downloadDocumentFile(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
