package com.exportrace.controller;

import com.exportrace.dto.CreateReinspectionRequest;
import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.entity.QaEvidence;
import com.exportrace.service.QualityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    @Autowired
    private QualityService qualityService;

    @GetMapping("/lot/{lotId}")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN', 'LOGISTICA', 'PRODUCCION', 'GERENCIA')")
    public ResponseEntity<QAInspectionDTO> getInspectionByLotId(@PathVariable Long lotId) {
        QAInspectionDTO dto = qualityService.getInspectionByLotId(lotId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping({"/lot/{lotId}/history", "/lots/{lotId}/inspections"})
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN', 'LOGISTICA', 'PRODUCCION', 'GERENCIA')")
    public ResponseEntity<List<QAInspectionDTO>> getInspectionHistoryByLotId(@PathVariable Long lotId) {
        List<QAInspectionDTO> history = qualityService.getInspectionHistoryByLotId(lotId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/lot/{lotId}")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<QAInspectionDTO> saveInspection(
            @PathVariable Long lotId,
            @RequestBody QAInspectionDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        QAInspectionDTO saved = qualityService.saveInspection(lotId, dto, email);
        return ResponseEntity.ok(saved);
    }

    @PostMapping({"/lot/{lotId}/reinspect", "/lots/{lotId}/reinspections"})
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<QAInspectionDTO> createReinspection(
            @PathVariable Long lotId,
            @Valid @RequestBody CreateReinspectionRequest req,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        QAInspectionDTO saved = qualityService.createReinspection(lotId, req, email);
        return ResponseEntity.ok(saved);
    }

    // ==========================================
    // Real Evidence Endpoints (P0-C)
    // ==========================================

    @PostMapping(value = "/inspections/{inspectionId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<QaEvidenceDTO> uploadEvidence(
            @PathVariable Long inspectionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        QaEvidenceDTO created = qualityService.uploadEvidence(inspectionId, file, description, email);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/inspections/{inspectionId}/evidence")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN', 'LOGISTICA', 'PRODUCCION', 'GERENCIA')")
    public ResponseEntity<List<QaEvidenceDTO>> getEvidencesByInspection(@PathVariable Long inspectionId) {
        List<QaEvidenceDTO> list = qualityService.getEvidencesByInspectionId(inspectionId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/lots/{lotId}/evidence")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN', 'LOGISTICA', 'PRODUCCION', 'GERENCIA')")
    public ResponseEntity<List<QaEvidenceDTO>> getEvidencesByLot(@PathVariable Long lotId) {
        List<QaEvidenceDTO> list = qualityService.getEvidencesByLotId(lotId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN', 'LOGISTICA', 'PRODUCCION', 'GERENCIA')")
    public ResponseEntity<Resource> streamEvidence(
            @PathVariable Long evidenceId,
            @RequestParam(value = "verify", defaultValue = "false") boolean verify) {
        QaEvidence entity = qualityService.getEvidenceEntity(evidenceId);
        Resource resource = qualityService.loadEvidenceResource(evidenceId, verify);

        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (entity.getMimeType() != null) {
            try {
                mediaType = MediaType.parseMediaType(entity.getMimeType());
            } catch (Exception ignored) {}
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entity.getOriginalFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }

    @GetMapping("/evidence/{evidenceId}/verify")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> verifyEvidenceIntegrity(@PathVariable Long evidenceId) {
        QaEvidence entity = qualityService.getEvidenceEntity(evidenceId);
        qualityService.loadEvidenceResource(evidenceId, true);

        Map<String, Object> resp = new HashMap<>();
        resp.put("evidenceId", evidenceId);
        resp.put("originalFileName", entity.getOriginalFileName());
        resp.put("sha256", entity.getSha256());
        resp.put("status", "INTEGRITY_VERIFIED");
        resp.put("message", "El archivo físico coincide exactamente con el hash criptográfico SHA-256.");
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/inspections/{inspectionId}/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> deleteEvidence(
            @PathVariable Long inspectionId,
            @PathVariable Long evidenceId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        qualityService.deleteEvidence(inspectionId, evidenceId, email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Evidencia fotográfica desactivada con éxito.");
        resp.put("active", false);
        return ResponseEntity.ok(resp);
    }
}
