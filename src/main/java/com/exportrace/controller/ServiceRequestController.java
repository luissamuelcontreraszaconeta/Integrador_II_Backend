package com.exportrace.controller;

import com.exportrace.dto.CreateServiceRequestDTO;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.service.ServiceRequestService;
import com.exportrace.service.ServiceRequestSyncService;
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
public class ServiceRequestController {

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ServiceRequestSyncService serviceRequestSyncService;

    @PostMapping("/service-requests")
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(
            @Valid @RequestBody CreateServiceRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ServiceRequestDTO created = serviceRequestService.createServiceRequest(request, userEmail, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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
