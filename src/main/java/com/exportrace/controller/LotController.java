package com.exportrace.controller;

import com.exportrace.dto.CreateLotRequest;
import com.exportrace.dto.LotDTO;
import com.exportrace.dto.LotStatusUpdateRequest;
import com.exportrace.dto.UpdateLotRequest;
import com.exportrace.entity.LotHistory;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.service.LotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
public class LotController {

    @Autowired
    private LotService lotService;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('LOTS_VIEW') or hasAnyRole('SUPERADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<List<LotDTO>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOTS_VIEW') or hasAnyRole('SUPERADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<LotDTO> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getLotById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('LOTS_VIEW') or hasAnyRole('SUPERADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<LotDTO> getLotByCode(@PathVariable String code) {
        return ResponseEntity.ok(lotService.getLotByCode(code));
    }

    @GetMapping("/qr/{token}")
    public ResponseEntity<LotDTO> getLotByQrToken(@PathVariable String token) {
        return ResponseEntity.ok(lotService.getLotByQrToken(token));
    }

    @PostMapping
    @PreAuthorize("(hasAuthority('LOTS_CREATE') or hasAnyRole('PRODUCCION', 'ADMINISTRADOR', 'SUPERADMIN')) and !hasAnyRole('QA', 'LOGISTICA', 'GERENCIA')")
    public ResponseEntity<LotDTO> createLot(@Valid @RequestBody CreateLotRequest request, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "produccion@exportrace.pe";
        LotDTO created = lotService.createLot(request, email);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("(hasAuthority('LOTS_UPDATE') or hasAnyRole('PRODUCCION', 'ADMINISTRADOR', 'SUPERADMIN')) and !hasAnyRole('QA', 'LOGISTICA', 'GERENCIA')")
    public ResponseEntity<LotDTO> updateLot(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLotRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "produccion@exportrace.pe";
        LotDTO updated = lotService.updateLot(id, request, email);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('LOTS_UPDATE') or hasAnyRole('PRODUCCION', 'QA', 'LOGISTICA', 'ADMINISTRADOR', 'SUPERADMIN')")
    public ResponseEntity<LotDTO> updateLotStatus(
            @PathVariable Long id,
            @RequestBody LotStatusUpdateRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "admin@exportrace.pe";
        LotDTO updated = lotService.updateLotStatus(id, request, email);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('LOTS_VIEW') or hasAnyRole('SUPERADMIN', 'ADMINISTRADOR')")
    public ResponseEntity<List<LotHistory>> getLotHistory(@PathVariable Long id) {
        return ResponseEntity.ok(lotHistoryRepository.findByLoteIdOrderByFechaCambioDesc(id));
    }
}
