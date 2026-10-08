package com.exportrace.controller;

import com.exportrace.dto.ColdChainIncidentDTO;
import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.dto.ResolveIncidentRequestDTO;
import com.exportrace.dto.ThermalProfileDTO;
import com.exportrace.service.ColdChainService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cold-chain")
public class ColdChainController {

    @Autowired
    private ColdChainService coldChainService;

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<ColdChainRecordDTO>> getLogsByLotId(@PathVariable Long lotId) {
        return ResponseEntity.ok(coldChainService.getLogsByLotId(lotId));
    }

    @GetMapping("/lot/{lotId}/profile")
    public ResponseEntity<ThermalProfileDTO> getThermalProfile(@PathVariable Long lotId) {
        return ResponseEntity.ok(coldChainService.getThermalProfile(lotId));
    }

    @GetMapping("/lot/{lotId}/incidents")
    public ResponseEntity<List<ColdChainIncidentDTO>> getIncidentsByLotId(@PathVariable Long lotId) {
        return ResponseEntity.ok(coldChainService.getIncidentsByLotId(lotId));
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<ColdChainRecordDTO> addLog(
            @PathVariable Long lotId,
            @RequestBody ColdChainRecordDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        String role = authentication != null && authentication.getAuthorities().stream().findFirst().isPresent()
                ? authentication.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "")
                : "QA";
        ColdChainRecordDTO saved = coldChainService.addTemperatureLog(lotId, dto, email, role);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/incidents/{incidentId}/review")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<ColdChainIncidentDTO> reviewIncident(
            @PathVariable Long incidentId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        String role = authentication != null && authentication.getAuthorities().stream().findFirst().isPresent()
                ? authentication.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "")
                : "QA";
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(coldChainService.reviewIncident(incidentId, email, role, notes));
    }

    @PostMapping("/incidents/{incidentId}/resolve")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<ColdChainIncidentDTO> resolveIncident(
            @PathVariable Long incidentId,
            @Valid @RequestBody ResolveIncidentRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        String role = authentication != null && authentication.getAuthorities().stream().findFirst().isPresent()
                ? authentication.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "")
                : "QA";
        return ResponseEntity.ok(coldChainService.resolveIncident(incidentId, request, email, role));
    }

    @PostMapping("/lot/{lotId}/resolve-alert")
    @PreAuthorize("hasAnyRole('QA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<?> resolveLatestAlert(
            @PathVariable Long lotId,
            @Valid @RequestBody ResolveIncidentRequestDTO request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        String role = authentication != null && authentication.getAuthorities().stream().findFirst().isPresent()
                ? authentication.getAuthorities().stream().findFirst().get().getAuthority().replace("ROLE_", "")
                : "QA";
        
        List<ColdChainIncidentDTO> incidents = coldChainService.getIncidentsByLotId(lotId);
        ColdChainIncidentDTO active = incidents.stream()
                .filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus()) || "UNDER_REVIEW".equalsIgnoreCase(i.getStatus()))
                .findFirst()
                .orElse(null);

        if (active == null) {
            return ResponseEntity.ok(Map.of("message", "No existen incidencias activas de cadena de frío para este lote."));
        }

        ColdChainIncidentDTO resolved = coldChainService.resolveIncident(Long.valueOf(active.getId()), request, email, role);
        return ResponseEntity.ok(resolved);
    }
}
