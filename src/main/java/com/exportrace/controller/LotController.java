package com.exportrace.controller;

import com.exportrace.dto.CreateLotRequest;
import com.exportrace.dto.LotDTO;
import com.exportrace.dto.LotStatusUpdateRequest;
import com.exportrace.entity.LotHistory;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.service.LotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<LotDTO>> getAllLots() {
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LotDTO> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getLotById(id));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<LotDTO> getLotByCode(@PathVariable String code) {
        return ResponseEntity.ok(lotService.getLotByCode(code));
    }

    @GetMapping("/qr/{token}")
    public ResponseEntity<LotDTO> getLotByQrToken(@PathVariable String token) {
        return ResponseEntity.ok(lotService.getLotByQrToken(token));
    }

    @PostMapping
    public ResponseEntity<LotDTO> createLot(@RequestBody CreateLotRequest request, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "produccion@exportrace.pe";
        LotDTO created = lotService.createLot(request, email);
        return ResponseEntity.ok(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LotDTO> updateLotStatus(
            @PathVariable Long id,
            @RequestBody LotStatusUpdateRequest request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "admin@exportrace.pe";
        LotDTO updated = lotService.updateLotStatus(id, request, email);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<LotHistory>> getLotHistory(@PathVariable Long id) {
        return ResponseEntity.ok(lotHistoryRepository.findByLoteIdOrderByFechaCambioDesc(id));
    }
}
