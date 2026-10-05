package com.exportrace.controller;

import com.exportrace.dto.PublicTraceabilityDTO;
import com.exportrace.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/traceability")
public class PublicTraceabilityController {

    @Autowired
    private LotService lotService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicTraceabilityDTO> getPublicTraceabilityByToken(@PathVariable String token) {
        if (token == null || token.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(PublicTraceabilityDTO.notFound());
        }

        try {
            PublicTraceabilityDTO dto = lotService.getPublicTraceability(token.trim());
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(PublicTraceabilityDTO.notFound());
        }
    }
}
