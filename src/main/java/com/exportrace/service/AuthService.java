package com.exportrace.service;

import com.exportrace.dto.AuthResponse;
import com.exportrace.dto.LoginRequest;
import com.exportrace.dto.UserDTO;
import com.exportrace.entity.User;
import com.exportrace.repository.UserRepository;
import com.exportrace.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas: correo no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getContrasena())) {
            throw new RuntimeException("Credenciales inválidas: contraseña incorrecta");
        }

        if (!"ACTIVO".equalsIgnoreCase(user.getEstado())) {
            throw new RuntimeException("El usuario se encuentra inactivo");
        }

        String roleName = user.getRole() != null ? user.getRole().getNombre() : "PRODUCCION";
        String token = jwtUtil.generateToken(user.getEmail(), roleName);

        UserDTO userDTO = new UserDTO(user);
        return new AuthResponse(token, userDTO);
    }
}
