package com.exportrace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Filtro de seguridad dedicado exclusivamente para GET /actuator/prometheus.
 *
 * Reglas de seguridad:
 * 1. Requiere cabecera Authorization: Bearer <MONITORING_TOKEN>.
 * 2. MONITORING_TOKEN se lee desde la variable de entorno (monitoring.token / MONITORING_TOKEN).
 * 3. Valida el token en tiempo constante (MessageDigest.isEqual) para prevenir ataques de timing.
 * 4. Nunca registra el token en logs ni lo incluye en respuestas de error.
 * 5. Otorga únicamente la autoridad SCOPE_PROMETHEUS_READ, válida exclusivamente para /actuator/prometheus.
 */
@Component
public class MonitoringTokenAuthFilter extends OncePerRequestFilter {

    public static final String PROMETHEUS_READ_AUTHORITY = "SCOPE_PROMETHEUS_READ";
    private static final String PROMETHEUS_PATH = "/actuator/prometheus";

    @Value("${monitoring.token:}")
    private String configuredMonitoringToken;

    /**
     * Permite configurar el token en memoria únicamente dentro de suites de pruebas automatizadas.
     */
    public void setConfiguredMonitoringTokenForTesting(String token) {
        this.configuredMonitoringToken = token;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = extractNormalizedPath(request);
        return !PROMETHEUS_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String expectedToken = resolveExpectedToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            writeUnauthorizedResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "El monitoreo Prometheus no cuenta con MONITORING_TOKEN configurado.");
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Autenticación Bearer requerida para consultar métricas de monitoreo.");
            return;
        }

        String providedToken = authHeader.substring(7).trim();
        if (providedToken.isEmpty() || !constantTimeEquals(providedToken, expectedToken.trim())) {
            writeUnauthorizedResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "Token de monitoreo inválido.");
            return;
        }

        UsernamePasswordAuthenticationToken monitoringAuth = new UsernamePasswordAuthenticationToken(
                "prometheus-monitor",
                null,
                List.of(new SimpleGrantedAuthority(PROMETHEUS_READ_AUTHORITY))
        );
        SecurityContextHolder.getContext().setAuthentication(monitoringAuth);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveExpectedToken() {
        if (configuredMonitoringToken != null && !configuredMonitoringToken.isBlank()) {
            return configuredMonitoringToken;
        }
        String envVal = System.getenv("MONITORING_TOKEN");
        return (envVal != null && !envVal.isBlank()) ? envVal : null;
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    private String extractNormalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isEmpty()) {
            return "";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, int status, String safeMessage) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = String.format(
                "{\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                status,
                status == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Forbidden",
                safeMessage
        );
        response.getWriter().write(json);
    }
}
