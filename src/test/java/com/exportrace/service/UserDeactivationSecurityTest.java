package com.exportrace.service;

import com.exportrace.dto.AuthResponse;
import com.exportrace.dto.LoginRequest;
import com.exportrace.dto.RefreshTokenRequest;
import com.exportrace.dto.UpdateUserRequest;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import com.exportrace.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserDeactivationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private User adminUser;
    private String rawPassword = "SecurePassword2026!";

    @BeforeEach
    void setUp() {
        Role roleQa = roleRepository.findByNombre("QA").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("QA");
            r.setDescripcion("Inspector QA");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleAdmin = roleRepository.findByNombre("ADMINISTRADOR").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("ADMINISTRADOR");
            r.setDescripcion("Administrador General");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        adminUser = new User();
        adminUser.setNombre("Admin");
        adminUser.setApellido("General");
        adminUser.setEmail("admin." + System.currentTimeMillis() + "@exportrace.pe");
        adminUser.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
        adminUser.setRole(roleAdmin);
        adminUser.setActivo(true);
        adminUser = userRepository.save(adminUser);

        testUser = new User();
        testUser.setNombre("Carlos");
        testUser.setApellido("Inspector");
        testUser.setEmail("carlos.inspector." + System.currentTimeMillis() + "@exportrace.pe");
        testUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        testUser.setRole(roleQa);
        testUser.setActivo(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("CASO 18.1: Usuario activo inicia sesión y token JWT funciona correctamente")
    void testUserActive_LoginAndRequest_Success() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testUser.getEmail());
        loginReq.setPassword(rawPassword);

        AuthResponse authRes = authService.login(loginReq, request);
        assertNotNull(authRes);
        assertNotNull(authRes.getToken());

        // Request with valid active token
        mockMvc.perform(get("/api/lots")
                        .header("Authorization", "Bearer " + authRes.getToken()))
                .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("CASO 18.2: Usuario desactivado -> Token JWT activo es RECHAZADO INMEDIATAMENTE (401/403)")
    void testUserDeactivated_JwtRejectedImmediately() throws Exception {
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testUser.getEmail());
        loginReq.setPassword(rawPassword);

        AuthResponse authRes = authService.login(loginReq, httpReq);
        String jwtToken = authRes.getToken();

        // Desactivar usuario administrativamente
        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setActivo(false);
        userService.updateUser(testUser.getId(), updateReq, adminUser.getEmail(), httpReq);

        // Petición subsecuente con el JWT anterior DEBE ser denegada inmediatamente
        mockMvc.perform(get("/api/lots")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403, "Status should be 401 or 403 but was " + status);
                });
    }

    @Test
    @DisplayName("CASO 18.3: Usuario desactivado -> Intento de refresh token es RECHAZADO")
    void testUserDeactivated_RefreshTokenRejected() {
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testUser.getEmail());
        loginReq.setPassword(rawPassword);

        AuthResponse authRes = authService.login(loginReq, httpReq);

        UpdateUserRequest updateReq = new UpdateUserRequest();
        updateReq.setActivo(false);
        userService.updateUser(testUser.getId(), updateReq, adminUser.getEmail(), httpReq);

        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(authRes.getRefreshToken());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.refresh(refreshReq, httpReq);
        });

        assertTrue(ex.getMessage().contains("SESSION_REVOKED") || ex.getMessage().contains("desactivada"));
    }

    @Test
    @DisplayName("CASO 18.4: Usuario desactivado -> Intento de re-login es BLOQUEADO")
    void testUserDeactivated_ReLoginBlocked() {
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        testUser.setActivo(false);
        userRepository.save(testUser);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testUser.getEmail());
        loginReq.setPassword(rawPassword);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.login(loginReq, httpReq);
        });

        assertTrue(ex.getMessage().contains("desactivada"));
    }

    @Test
    @DisplayName("CASO 18.5: Re-activación del usuario NO restaura tokens de sesiones previamente revocadas")
    void testUserReactivated_DoesNotRestoreRevokedTokens() {
        MockHttpServletRequest httpReq = new MockHttpServletRequest();
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(testUser.getEmail());
        loginReq.setPassword(rawPassword);

        AuthResponse authRes = authService.login(loginReq, httpReq);
        String oldRefreshToken = authRes.getRefreshToken();

        // 1. Desactivar
        UpdateUserRequest deactReq = new UpdateUserRequest();
        deactReq.setActivo(false);
        userService.updateUser(testUser.getId(), deactReq, adminUser.getEmail(), httpReq);

        // 2. Reactivar usuario
        UpdateUserRequest reactReq = new UpdateUserRequest();
        reactReq.setActivo(true);
        userService.updateUser(testUser.getId(), reactReq, adminUser.getEmail(), httpReq);

        // 3. El token de refresco antiguo debe seguir REVOCADO
        RefreshTokenRequest refreshReq = new RefreshTokenRequest();
        refreshReq.setRefreshToken(oldRefreshToken);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            authService.refresh(refreshReq, httpReq);
        });

        assertTrue(ex.getMessage().contains("SESSION_REVOKED"));
    }
}
