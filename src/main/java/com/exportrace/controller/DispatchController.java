package com.exportrace.controller;

import com.exportrace.entity.Dispatch;
import com.exportrace.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatches")
public class DispatchController {

    @Autowired
    private DispatchService dispatchService;

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<Dispatch> getDispatchByLotId(@PathVariable Long lotId) {
        Dispatch dispatch = dispatchService.getDispatchByLotId(lotId);
        if (dispatch == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dispatch);
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<Dispatch> registerDispatch(
            @PathVariable Long lotId,
            @RequestBody Dispatch request,
            Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "logistica@exportrace.pe";
        Dispatch dispatch = dispatchService.registerDispatch(lotId, request, email);
        return ResponseEntity.ok(dispatch);
    }
}
