package com.exportrace.controller;

import com.exportrace.dto.CreateServiceRequestDTO;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.service.ServiceRequestService;
import com.exportrace.service.ServiceRequestSyncService;
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
public class ServiceRequestController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ServiceRequestSyncService serviceRequestSyncService;

    @PostMapping(value = "/service-requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestDTO created = serviceRequestService.createServiceRequest(request, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/service-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceRequestDTO> createServiceRequestWithAttachment(
            @Valid @RequestPart("request") CreateServiceRequestDTO request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestDTO created = serviceRequestService.createServiceRequest(request, file, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/service-requests/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ServiceRequestDTO> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestDTO updated = serviceRequestService.uploadAttachment(id, file, userEmail, httpRequest);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/service-requests/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestService.AttachmentDownload download =
                serviceRequestService.downloadAttachment(id, userEmail, httpRequest);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.originalName() + "\"")
                .body(download.resource());
    }

    @PostMapping("/service-requests/{id}/refresh-from-jira")
    public ResponseEntity<ServiceRequestDTO> refreshServiceRequestFromJira(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestDTO refreshed = serviceRequestSyncService.refreshFromJira(id, userEmail, httpRequest);
        return ResponseEntity.ok(refreshed);
    }

    @GetMapping("/service-requests/my")
    public ResponseEntity<List<ServiceRequestDTO>> getMyServiceRequests(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(serviceRequestService.getMyServiceRequests(userEmail));
    }

    @GetMapping("/service-requests/{id}")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(serviceRequestService.getServiceRequestById(id, userEmail, httpRequest));
    }

    @GetMapping("/admin/service-requests")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequestsForAdmin(Authentication authentication) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(serviceRequestService.getAllServiceRequestsForAdmin(userEmail));
    }

    @PostMapping("/admin/service-requests/{id}/retry-sync")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<ServiceRequestDTO> retryServiceRequestSync(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(serviceRequestSyncService.retrySyncForAdmin(id, userEmail, httpRequest));
    }
}
