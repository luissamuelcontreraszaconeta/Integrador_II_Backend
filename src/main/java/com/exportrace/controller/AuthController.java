package com.exportrace.controller;

import com.exportrace.dto.AuthResponse;
import com.exportrace.dto.LoginRequest;
import com.exportrace.dto.RefreshTokenRequest;
import com.exportrace.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        try {
            AuthResponse response = authService.login(request, httpRequest);

            // Also attach HttpOnly, Secure, SameSite=None cookie for refresh token if supported
            attachRefreshTokenCookie(httpResponse, response.getRefreshToken());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                     @CookieValue(name = "exportrace_refresh_token", required = false) String cookieRefreshToken,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        try {
            String refreshToken = (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank())
                    ? request.getRefreshToken()
                    : cookieRefreshToken;

            if (refreshToken == null || refreshToken.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token de refresco no provisto."));
            }

            AuthResponse response = authService.refresh(new RefreshTokenRequest(refreshToken), httpRequest);

            // Re-attach cookie if token refreshed
            attachRefreshTokenCookie(httpResponse, response.getRefreshToken());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String error = e.getMessage() != null ? e.getMessage() : "Error al renovar sesión";
            if (error.startsWith("SESSION_IDLE_EXPIRED") || error.startsWith("SESSION_ABSOLUTE_EXPIRED") || error.startsWith("SESSION_REVOKED")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "SESSION_EXPIRED",
                        "message", error
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", error));
        }
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<?> keepAlive(HttpServletRequest httpRequest) {
        try {
            authService.keepAlive(httpRequest);
            return ResponseEntity.ok(Map.of("status", "OK", "message", "Sesión extendida correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    @CookieValue(name = "exportrace_refresh_token", required = false) String cookieRefreshToken,
                                    @RequestParam(name = "sessionId", required = false) String sessionId,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        try {
            String refreshToken = (request != null && request.getRefreshToken() != null)
                    ? request.getRefreshToken()
                    : cookieRefreshToken;

            authService.logout(refreshToken, sessionId, httpRequest);

            // Clear cookie
            clearRefreshTokenCookie(httpResponse);

            return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Logout procesado."));
        }
    }

    private void attachRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (response == null || refreshToken == null) return;
        // Cookie header with SameSite=None; Secure; HttpOnly
        String cookieHeader = String.format("exportrace_refresh_token=%s; Path=/api/auth; Max-Age=2592000; HttpOnly; Secure; SameSite=None", refreshToken);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        if (response == null) return;
        String cookieHeader = "exportrace_refresh_token=; Path=/api/auth; Max-Age=0; HttpOnly; Secure; SameSite=None";
        response.addHeader("Set-Cookie", cookieHeader);
    }
}
