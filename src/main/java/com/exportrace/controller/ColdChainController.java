package com.exportrace.controller;

import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.service.ColdChainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cold-chain")
public class ColdChainController {

    @Autowired
    private ColdChainService coldChainService;

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<ColdChainRecordDTO>> getLogsByLotId(@PathVariable Long lotId) {
        return ResponseEntity.ok(coldChainService.getLogsByLotId(lotId));
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<ColdChainRecordDTO> addLog(
            @PathVariable Long lotId,
            @RequestBody ColdChainRecordDTO dto,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "qa@exportrace.pe";
        ColdChainRecordDTO saved = coldChainService.addTemperatureLog(lotId, dto, email);
        return ResponseEntity.ok(saved);
    }
}
