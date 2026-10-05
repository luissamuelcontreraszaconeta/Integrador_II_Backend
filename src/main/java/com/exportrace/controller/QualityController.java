package com.exportrace.controller;

import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.QaEvidenceDTO;
import com.exportrace.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ResponseEntity<QAInspectionDTO> getInspectionByLotId(@PathVariable Long lotId) {
        QAInspectionDTO dto = qualityService.getInspectionByLotId(lotId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<QAInspectionDTO> saveInspection(
            @PathVariable Long lotId,
            @RequestBody QAInspectionDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        QAInspectionDTO saved = qualityService.saveInspection(lotId, dto, email);
        return ResponseEntity.ok(saved);
    }

    // ==========================================
    // Real Evidence Endpoints
    // ==========================================

    @PostMapping(value = "/inspections/{inspectionId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<List<QaEvidenceDTO>> getEvidencesByInspection(@PathVariable Long inspectionId) {
        List<QaEvidenceDTO> list = qualityService.getEvidencesByInspectionId(inspectionId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/lots/{lotId}/evidence")
    public ResponseEntity<List<QaEvidenceDTO>> getEvidencesByLot(@PathVariable Long lotId) {
        List<QaEvidenceDTO> list = qualityService.getEvidencesByLotId(lotId);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/inspections/{inspectionId}/evidence/{evidenceId}")
    public ResponseEntity<Map<String, Object>> deleteEvidence(
            @PathVariable Long inspectionId,
            @PathVariable Long evidenceId,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        qualityService.deleteEvidence(inspectionId, evidenceId, email);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Evidencia fotográfica eliminada con éxito");
        return ResponseEntity.ok(resp);
    }
}
