package com.exportrace.service;

import com.exportrace.dto.AuthResponse;
import com.exportrace.dto.LoginRequest;
import com.exportrace.dto.UserDTO;
import com.exportrace.entity.User;
import com.exportrace.repository.UserRepository;
import com.exportrace.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            auditService.logAction(
                    null,
                    email,
                    "ANONIMO",
                    "LOGIN_FAILED",
                    "AUTENTICACION",
                    "Auth",
                    email,
                    "Intento de inicio de sesión fallido: usuario no encontrado (" + email + ")",
                    null,
                    null,
                    "FALLIDO",
                    httpRequest
            );
            throw new RuntimeException("Credenciales inválidas: correo o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logAction(
                    user.getId(),
                    user.getNombre(),
                    user.getRole() != null ? user.getRole().getNombre() : "UNKNOWN",
                    "LOGIN_FAILED",
                    "AUTENTICACION",
                    "User",
                    String.valueOf(user.getId()),
                    "Intento de inicio de sesión fallido: contraseña incorrecta",
                    null,
                    null,
                    "FALLIDO",
                    httpRequest
            );
            throw new RuntimeException("Credenciales inválidas: correo o contraseña incorrectos");
        }

        if (Boolean.FALSE.equals(user.getActivo())) {
            auditService.logAction(
                    user.getId(),
                    user.getNombre(),
                    user.getRole() != null ? user.getRole().getNombre() : "UNKNOWN",
                    "LOGIN_BLOCKED",
                    "AUTENTICACION",
                    "User",
                    String.valueOf(user.getId()),
                    "Inicio de sesión bloqueado: cuenta desactivada",
                    null,
                    null,
                    "DENEGADO",
                    httpRequest
            );
            throw new RuntimeException("La cuenta se encuentra desactivada. Comuníquese con el Administrador.");
        }

        user.setUltimoAcceso(LocalDateTime.now());
        userRepository.save(user);

        String roleName = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        String token = jwtUtil.generateToken(user.getEmail(), roleName);

        auditService.logAction(
                user.getId(),
                user.getNombre(),
                roleName,
                "LOGIN_SUCCESS",
                "AUTENTICACION",
                "User",
                String.valueOf(user.getId()),
                "Inicio de sesión exitoso en la plataforma",
                null,
                "Rol: " + roleName,
                "EXITOSO",
                httpRequest
        );

        UserDTO userDTO = new UserDTO(user);
        return new AuthResponse(token, userDTO);
    }
}
