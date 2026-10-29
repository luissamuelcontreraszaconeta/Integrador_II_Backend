package com.exportrace.controller;

import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.service.IncidentService;
import com.exportrace.service.IncidentSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentSyncService incidentSyncService;

    @PostMapping("/incidents")
    public ResponseEntity<IncidentDTO> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentDTO created = incidentService.createIncident(request, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/incidents/my")
    public ResponseEntity<List<IncidentDTO>> getMyIncidents(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(incidentService.getMyIncidents(userEmail));
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<IncidentDTO> getIncidentById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(incidentService.getIncidentById(id, userEmail, httpRequest));
    }

    @GetMapping("/admin/incidents")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<List<IncidentDTO>> getAllIncidentsForAdmin(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(incidentService.getAllIncidentsForAdmin(userEmail));
    }

    @PostMapping("/admin/incidents/{id}/retry-sync")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<IncidentDTO> retryIncidentSync(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(incidentSyncService.retrySyncForAdmin(id, userEmail, httpRequest));
    }
}
