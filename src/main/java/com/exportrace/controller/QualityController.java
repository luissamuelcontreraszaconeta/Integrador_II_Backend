package com.exportrace.controller;

import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.service.QualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
}
