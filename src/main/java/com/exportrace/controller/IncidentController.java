package com.exportrace.controller;

import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.service.IncidentService;
import com.exportrace.service.IncidentSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentSyncService incidentSyncService;

    @PostMapping(value = "/incidents", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncidentDTO> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentDTO created = incidentService.createIncident(request, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/incidents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidentDTO> createIncidentWithAttachment(
            @Valid @RequestPart("request") CreateIncidentRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentDTO created = incidentService.createIncident(request, file, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/incidents/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IncidentDTO> uploadIncidentAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentDTO updated = incidentService.uploadAttachment(id, file, userEmail, httpRequest);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/incidents/{id}/attachment")
    public ResponseEntity<Resource> downloadIncidentAttachment(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        IncidentService.AttachmentDownload download = incidentService.downloadAttachment(id, userEmail, httpRequest);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.originalName() + "\"")
                .body(download.resource());
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
