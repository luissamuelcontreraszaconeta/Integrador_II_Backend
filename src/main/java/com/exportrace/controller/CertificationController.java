package com.exportrace.controller;

import com.exportrace.entity.SanitaryCertification;
import com.exportrace.service.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certifications")
public class CertificationController {

    @Autowired
    private CertificationService certificationService;

    @GetMapping
    public ResponseEntity<List<SanitaryCertification>> getAllCertifications() {
        return ResponseEntity.ok(certificationService.getAllCertifications());
    }

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<SanitaryCertification> getCertificationByLotId(@PathVariable Long lotId) {
        SanitaryCertification cert = certificationService.getCertificationByLotId(lotId);
        if (cert == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(cert);
    }

    @PostMapping("/lot/{lotId}/request")
    public ResponseEntity<SanitaryCertification> requestCertification(@PathVariable Long lotId, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "logistica@exportrace.pe";
        SanitaryCertification cert = certificationService.requestCertification(lotId, email);
        return ResponseEntity.ok(cert);
    }

    @PostMapping("/lot/{lotId}/approve")
    public ResponseEntity<SanitaryCertification> approveCertification(
            @PathVariable Long lotId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "logistica@exportrace.pe";
        String certNum = body != null ? body.get("certNumber") : null;
        SanitaryCertification cert = certificationService.approveCertification(lotId, certNum, email);
        return ResponseEntity.ok(cert);
    }
}
