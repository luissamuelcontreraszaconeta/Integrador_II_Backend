package com.exportrace.controller;

import com.exportrace.dto.UserDTO;
import com.exportrace.entity.User;
import com.exportrace.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> payload) {
        try {
            User user = new User();
            user.setNombre((String) payload.get("nombre"));
            user.setEmail((String) payload.get("email"));
            user.setContrasena((String) payload.get("password"));
            user.setArea((String) payload.get("area"));
            String roleName = payload.get("rol") != null ? (String) payload.get("rol") : "PRODUCCION";

            UserDTO created = userService.createUser(user, roleName);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
