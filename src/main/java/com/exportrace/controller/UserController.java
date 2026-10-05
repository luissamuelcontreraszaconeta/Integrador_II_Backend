package com.exportrace.controller;

import com.exportrace.dto.CreateUserRequest;
import com.exportrace.dto.UserAdminDTO;
import com.exportrace.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USERS_VIEW') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UserAdminDTO>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(userService.getAllUsers(search, role, status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USERS_CREATE') or hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest payload,
                                        Authentication auth,
                                        HttpServletRequest request) {
        try {
            String adminEmail = auth != null ? auth.getName() : "admin@exportrace.pe";
            UserAdminDTO created = userService.createUser(payload, adminEmail, request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
