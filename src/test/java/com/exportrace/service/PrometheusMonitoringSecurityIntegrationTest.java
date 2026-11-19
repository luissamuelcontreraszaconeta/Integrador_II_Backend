package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.config.MonitoringTokenAuthFilter;
import com.exportrace.entity.Role;
import com.exportrace.entity.User;
import com.exportrace.repository.RoleRepository;
import com.exportrace.repository.UserRepository;
import com.exportrace.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite de pruebas automatizadas para Monitoreo Prometheus Seguro (MON-SEC-01..MON-SEC-07).
 */
@SpringBootTest(properties = {
        "management.endpoints.web.exposure.include=health,prometheus",
        "management.endpoint.prometheus.enabled=true",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
public class PrometheusMonitoringSecurityIntegrationTest {

    private static final String TEST_MONITORING_TOKEN = "mon_tok_exportrace_secret_prometheus_2026_x99";
    private static final String TEST_JIRA_SECRET = "ATATT3xFfGF0_JIRA_SECRET_NEVER_EXPOSE_IN_METRICS";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitoringTokenAuthFilter monitoringTokenAuthFilter;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String userJwtToken;

    @BeforeEach
    void setUp() {
        monitoringTokenAuthFilter.setConfiguredMonitoringTokenForTesting(TEST_MONITORING_TOKEN);
        jiraProperties.setApiToken(TEST_JIRA_SECRET);

        Role adminRole = roleRepository.findByNombre("ADMINISTRADOR").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("ADMINISTRADOR");
            r.setDescripcion("Administrador");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        long suffix = System.nanoTime();
        User adminUser = new User();
        adminUser.setNombre("Monitor");
        adminUser.setApellido("Tester");
        adminUser.setEmail("monitor.test." + suffix + "@exportrace.pe");
        adminUser.setPasswordHash(passwordEncoder.encode("SecretPass123!"));
        adminUser.setRole(adminRole);
        adminUser.setActivo(true);
        adminUser = userRepository.save(adminUser);

        userJwtToken = jwtUtil.generateAccessToken(adminUser, "sess-mon-" + suffix);
    }

    @AfterEach
    void tearDown() {
        monitoringTokenAuthFilter.setConfiguredMonitoringTokenForTesting("");
        jiraProperties.setApiToken("");
    }

    @Test
    @DisplayName("MON-SEC-01: /actuator/health y /api/health continúan funcionando sin autenticación para Render")
    void monSec01_actuatorHealthAndApiHealthContinueWorking() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("MON-SEC-02: /actuator/prometheus sin token es rechazado con 401/403")
    void monSec02_prometheusWithoutTokenIsRejected() throws Exception {
        MvcResult res = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "Expected 401 or 403 without token, got " + st);
                })
                .andReturn();

        assertFalse(res.getResponse().getContentAsString().contains(TEST_MONITORING_TOKEN));
    }

    @Test
    @DisplayName("MON-SEC-03: /actuator/prometheus con token incorrecto o con JWT de usuario es rechazado con 401/403")
    void monSec03_prometheusWithInvalidTokenOrUserJwtIsRejected() throws Exception {
        // Token incorrecto
        MvcResult wrongRes = mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token_incorrecto_12345"))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "Expected 401 or 403 with wrong token, got " + st);
                })
                .andReturn();
        assertFalse(wrongRes.getResponse().getContentAsString().contains(TEST_MONITORING_TOKEN));

        // JWT válido de usuario normal de ExporTrace NO debe servir para /actuator/prometheus
        mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "Expected 401 or 403 with user JWT on /actuator/prometheus, got " + st);
                });
    }

    @Test
    @DisplayName("MON-SEC-04 & MON-SEC-05 & MON-SEC-07: /actuator/prometheus con MONITORING_TOKEN válido devuelve HTTP 200 en formato Prometheus con métricas JVM, CPU, HTTP y sin exponer ningún secreto")
    void monSec04_05_07_prometheusWithValidMonitoringTokenReturnsMetricsWithoutSecrets() throws Exception {
        // Primero realizamos una petición HTTP previa para asegurar que http_server_requests_seconds esté registrado
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/actuator/prometheus")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_MONITORING_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains("text/plain"), "Content-Type debe ser text/plain compatible con Prometheus: " + contentType);

        String body = result.getResponse().getContentAsString();
        assertNotNull(body);
        assertFalse(body.isBlank());

        // MON-SEC-05: Formato Prometheus (# HELP, # TYPE) y métricas requeridas (JVM, CPU/proceso, HTTP requests/latencia, tag application)
        assertTrue(body.contains("# HELP"), "Debe contener directivas # HELP de Prometheus");
        assertTrue(body.contains("# TYPE"), "Debe contener directivas # TYPE de Prometheus");
        assertTrue(body.contains("jvm_memory_used_bytes"), "Debe incluir métricas de memoria JVM");
        assertTrue(body.contains("system_cpu_usage") || body.contains("process_cpu_usage"), "Debe incluir métricas de CPU/proceso");
        assertTrue(body.contains("http_server_requests_seconds"), "Debe incluir métricas de peticiones y latencia HTTP");
        assertTrue(body.contains("application=\"exportrace-backend\""), "Debe incluir tag de estado/aplicación exportrace-backend");

        // MON-SEC-07: Ningún secreto ni credencial aparece en las métricas
        assertFalse(body.contains(TEST_MONITORING_TOKEN), "Nunca debe exponer MONITORING_TOKEN en métricas");
        assertFalse(body.contains(TEST_JIRA_SECRET), "Nunca debe exponer JIRA_API_TOKEN en métricas");
        assertFalse(body.contains(userJwtToken), "Nunca debe exponer JWT en métricas");
        assertFalse(body.contains("SecretPass123!"), "Nunca debe exponer contraseñas en métricas");
    }

    @Test
    @DisplayName("MON-SEC-06 & MON-SEC-07: JWT/RBAC normal de ExporTrace no se altera, MONITORING_TOKEN no sirve para APIs de negocio y endpoints Actuator sensibles están bloqueados")
    void monSec06_07_normalJwtRbacUnchangedAndSensitiveActuatorEndpointsBlocked() throws Exception {
        // 1. El JWT de usuario normal sigue accediendo normalmente a endpoints de negocio
        mockMvc.perform(get("/api/lots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken))
                .andExpect(status().isOk());

        // 2. El MONITORING_TOKEN NO otorga acceso a ningún endpoint de negocio de ExporTrace
        mockMvc.perform(get("/api/lots")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_MONITORING_TOKEN))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "MONITORING_TOKEN no debe autenticar en /api/lots");
                });

        // 3. Endpoints Actuator sensibles (/env, /beans, /configprops, /heapdump, /threaddump, /mappings, /loggers) están bloqueados
        String[] forbiddenActuatorPaths = {
                "/actuator/env",
                "/actuator/beans",
                "/actuator/configprops",
                "/actuator/heapdump",
                "/actuator/threaddump",
                "/actuator/mappings",
                "/actuator/loggers",
                "/actuator/shutdown"
        };

        for (String path : forbiddenActuatorPaths) {
            mockMvc.perform(get(path)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_MONITORING_TOKEN))
                    .andExpect(result -> {
                        int st = result.getResponse().getStatus();
                        assertTrue(st == 401 || st == 403 || st == 404,
                                "El endpoint sensible " + path + " debe estar bloqueado (status=" + st + ")");
                    });
        }
    }
}
