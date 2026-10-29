package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.entity.AuditLog;
import com.exportrace.entity.IncidentReport;
import com.exportrace.entity.Lot;
import com.exportrace.entity.Product;
import com.exportrace.entity.Role;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.*;
import com.exportrace.repository.*;
import com.exportrace.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de pruebas automatizadas de Fase C: Integración REST ExporTrace -> Jira Service Management.
 * 100% determinista y sin dependencia de Internet mediante MockRestServiceServer.
 * Cubre INC-C01..INC-C15.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class JiraIncidentSyncTest {

    private static final String TEST_JIRA_BASE_URL = "https://exportrace-test.atlassian.net";
    private static final String TEST_SERVICE_DESK_ID = "4";
    private static final String TEST_REQUEST_TYPE_ID = "19";
    private static final String TEST_ACCOUNT_EMAIL = "jira-bot@exportrace.pe";
    private static final String TEST_SECRET_API_TOKEN = "ATATT3xFfGF0_SUPER_SECRET_TOKEN_DO_NOT_LEAK_998877";
    private static final String TEST_CORRELATION_FIELD_ID = "customfield_10055";
    private static final String TEST_CODE_FIELD_ID = "customfield_10056";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraIncidentService jiraIncidentService;

    @Autowired
    private IncidentSyncService incidentSyncService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentReportRepository incidentReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private MockRestServiceServer mockJiraServer;
    private User userQa;
    private User userAdmin;
    private User userSuperAdmin;
    private String tokenQa;
    private String tokenAdmin;
    private String tokenSuperAdmin;
    private Lot testLot;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockJiraServer = MockRestServiceServer.bindTo(builder).build();
        jiraIncidentService.setRestClientForTesting(builder.build());

        configureValidJiraProperties(true);

        Role roleQa = roleRepository.findByNombre("QA").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("QA");
            r.setDescripcion("Inspector de Calidad");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleAdmin = roleRepository.findByNombre("ADMINISTRADOR").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("ADMINISTRADOR");
            r.setDescripcion("Administrador TI");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleSuperAdmin = roleRepository.findByNombre("SUPERADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("SUPERADMIN");
            r.setDescripcion("Superadministrador");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        long suffix = System.nanoTime();

        userQa = new User();
        userQa.setNombre("Elena");
        userQa.setApellido("Vargas");
        userQa.setEmail("qa.fasec." + suffix + "@exportrace.pe");
        userQa.setPasswordHash(passwordEncoder.encode("QA123"));
        userQa.setRole(roleQa);
        userQa.setActivo(true);
        userQa = userRepository.save(userQa);
        tokenQa = jwtUtil.generateAccessToken(userQa, "sess-fasec-qa-" + suffix);

        userAdmin = new User();
        userAdmin.setNombre("Roberto");
        userAdmin.setApellido("Salazar");
        userAdmin.setEmail("admin.fasec." + suffix + "@exportrace.pe");
        userAdmin.setPasswordHash(passwordEncoder.encode("Admin123"));
        userAdmin.setRole(roleAdmin);
        userAdmin.setActivo(true);
        userAdmin = userRepository.save(userAdmin);
        tokenAdmin = jwtUtil.generateAccessToken(userAdmin, "sess-fasec-admin-" + suffix);

        userSuperAdmin = new User();
        userSuperAdmin.setNombre("Marco");
        userSuperAdmin.setApellido("Valdivia");
        userSuperAdmin.setEmail("superadmin.fasec." + suffix + "@exportrace.pe");
        userSuperAdmin.setPasswordHash(passwordEncoder.encode("SuperAdmin2026!"));
        userSuperAdmin.setRole(roleSuperAdmin);
        userSuperAdmin.setActivo(true);
        userSuperAdmin = userRepository.save(userSuperAdmin);
        tokenSuperAdmin = jwtUtil.generateAccessToken(userSuperAdmin, "sess-fasec-super-" + suffix);

        Product prod = productRepository.findAll().stream().findFirst().orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("PROD-FASEC");
            p.setNombre("Pota Congelada Export");
            p.setTipoConservacion("CONGELADO");
            return productRepository.save(p);
        });

        testLot = new Lot();
        testLot.setCodigo("LOT-FASEC-" + (suffix % 100000));
        testLot.setProducto(prod);
        testLot.setEstado("REGISTERED");
        testLot.setPesoNetoKg(18000.0);
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setQrToken("QR-FASEC-" + suffix);
        testLot = lotRepository.save(testLot);
    }

    @AfterEach
    void tearDown() {
        jiraProperties.setEnabled(false);
        jiraProperties.setBaseUrl("");
        jiraProperties.setServiceDeskId("");
        jiraProperties.setRequestTypeId("");
        jiraProperties.setAccountEmail("");
        jiraProperties.setApiToken("");
        jiraProperties.setCorrelationFieldId("");
        jiraProperties.setExportraceCodeFieldId("");
        jiraIncidentService.setRestClientForTesting(null);
    }

    private void configureValidJiraProperties(boolean enabled) {
        jiraProperties.setEnabled(enabled);
        jiraProperties.setBaseUrl(TEST_JIRA_BASE_URL);
        jiraProperties.setServiceDeskId(TEST_SERVICE_DESK_ID);
        jiraProperties.setRequestTypeId(TEST_REQUEST_TYPE_ID);
        jiraProperties.setAccountEmail(TEST_ACCOUNT_EMAIL);
        jiraProperties.setApiToken(TEST_SECRET_API_TOKEN);
        jiraProperties.setCorrelationFieldId(TEST_CORRELATION_FIELD_ID);
        jiraProperties.setExportraceCodeFieldId(TEST_CODE_FIELD_ID);
        jiraProperties.setConnectTimeoutSeconds(5);
        jiraProperties.setReadTimeoutSeconds(10);
    }

    private CreateIncidentRequest buildSampleRequest() {
        CreateIncidentRequest req = new CreateIncidentRequest();
        req.setCategory(IncidentCategory.COLD_CHAIN);
        req.setSummary("Fallo al cargar perfil térmico del lote en cámara 2");
        req.setDescription("El módulo de cadena de frío presenta error al consultar lecturas del sensor principal.");
        req.setImpact(IncidentImpact.HIGH);
        req.setWorkContinuity(WorkContinuity.NO_PUEDE_CONTINUAR);
        req.setModule("FRIO");
        req.setRoute("/quality/coldchain");
        req.setRelatedLotId(testLot.getId());
        return req;
    }

    private String buildRequestTypeFieldsJson() {
        return """
                {
                  "canAddRequestParticipants": true,
                  "requestTypeFields": [
                    { "fieldId": "summary", "name": "Summary", "required": true },
                    { "fieldId": "description", "name": "Description", "required": false },
                    { "fieldId": "customfield_10055", "name": "ExporTrace Correlation ID", "required": true },
                    { "fieldId": "customfield_10056", "name": "ExporTrace Incident Code", "required": false },
                    { "fieldId": "priority", "name": "Priority", "required": false }
                  ]
                }
                """;
    }

    @Test
    @DisplayName("INC-C01: Jira responde 201 Created -> SYNCED + jiraIssueKey y jiraRequestId guardados + auditoría")
    void incC01_jiraReturns201_marksSyncedAndSavesIssueKey() throws Exception {
        // 1. Lookup JQL -> 0 issues (no existe aún)
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        // 2. Request Type Fields -> compatible
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        // 3. Create Customer Request -> 201 Created con EXP-27
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.jsonPath("$.serviceDeskId").value("4"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.requestTypeId").value("19"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.requestFieldValues.summary").value("Fallo al cargar perfil térmico del lote en cámara 2"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.requestFieldValues.customfield_10055").isNotEmpty())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "issueId": "12345",
                                  "issueKey": "EXP-27"
                                }
                                """));

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXP-27"))
                .andExpect(jsonPath("$.slaAuthority").value("JIRA_AUTHORITATIVE"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();

        assertEquals(JiraSyncStatus.SYNCED, saved.getSyncStatus());
        assertEquals("EXP-27", saved.getJiraIssueKey());
        assertEquals("12345", saved.getJiraRequestId());
        assertEquals(1, saved.getSyncAttempts());
        assertNotNull(saved.getJiraLastSyncAt());
        assertNull(saved.getLastSyncError());

        List<AuditLog> audits = auditLogRepository.findAll();
        assertTrue(audits.stream().anyMatch(a -> "INCIDENT_SYNC_ATTEMPT".equals(a.getAction())
                && String.valueOf(saved.getId()).equals(a.getEntityId())));
        assertTrue(audits.stream().anyMatch(a -> "INCIDENT_SYNCED_TO_JIRA".equals(a.getAction())
                && String.valueOf(saved.getId()).equals(a.getEntityId())
                && a.getNewValue() != null && a.getNewValue().contains("EXP-27")));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C02: Jira timeout -> incidencia local permanece en BD con estado RETRY_PENDING")
    void incC02_jiraTimeout_localIncidentPersistsWithRetryPending() throws Exception {
        // Lookup JQL -> 0 issues
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        // Request Type Fields -> compatible
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        // Create Customer Request -> lanza SocketTimeoutException
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out waiting for Jira", new SocketTimeoutException("Read timed out"));
                });

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.syncStatus").value("RETRY_PENDING"))
                .andExpect(jsonPath("$.slaAuthority").value("LOCAL_ESTIMATE"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();

        assertEquals(IncidentStatus.REPORTED, saved.getStatus());
        assertEquals(JiraSyncStatus.RETRY_PENDING, saved.getSyncStatus());
        assertNull(saved.getJiraIssueKey());
        assertEquals(1, saved.getSyncAttempts());
        assertNotNull(saved.getLastSyncError());

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C03: Jira responde HTTP 503 Service Unavailable -> RETRY_PENDING")
    void incC03_jiraReturns503_marksRetryPending() throws Exception {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessages\":[\"Service temporarily unavailable\"]}"));

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("RETRY_PENDING"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.RETRY_PENDING, saved.getSyncStatus());
        assertTrue(saved.getLastSyncError().contains("503"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C04: Jira responde HTTP 400 Bad Request -> ERROR permanente")
    void incC04_jiraReturns400_marksError() throws Exception {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessage\":\"Invalid request payload\"}"));

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("ERROR"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.ERROR, saved.getSyncStatus());
        assertTrue(saved.getLastSyncError().contains("400"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C05: Jira responde HTTP 401 Unauthorized -> ERROR de credenciales")
    void incC05_jiraReturns401_marksError() throws Exception {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Client must be authenticated to access this resource.\"}"));

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("ERROR"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.ERROR, saved.getSyncStatus());
        assertTrue(saved.getLastSyncError().contains("401"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C06: Jira responde HTTP 429 Too Many Requests con Retry-After -> RETRY_PENDING")
    void incC06_jiraReturns429WithRetryAfter_marksRetryPending() throws Exception {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "45");

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .headers(headers)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Rate limit exceeded\"}"));

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("RETRY_PENDING"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.RETRY_PENDING, saved.getSyncStatus());
        assertTrue(saved.getLastSyncError().contains("429"));
        assertTrue(saved.getLastSyncError().contains("Retry-After: 45s"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C07: Retry encuentra ticket existente por correlationId (EXP-20) -> recupera issueKey y NO crea segundo ticket")
    void incC07_retryFindsExistingTicketByCorrelationId_recoversWithoutCreatingDuplicate() throws Exception {
        // Primero creamos la incidencia con Jira deshabilitado para simular una incidencia en RETRY_PENDING tras timeout
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);
        IncidentReport entity = incidentReportRepository.findById(created.getId()).orElseThrow();
        entity.setSyncStatus(JiraSyncStatus.RETRY_PENDING);
        entity.setSyncAttempts(1);
        entity.setLastSyncError("Timeout previo simulado");
        incidentReportRepository.saveAndFlush(entity);

        // Ahora habilitamos Jira y configuramos que el LOOKUP JQL devuelva EXP-20 (sin esperar POST /request)
        configureValidJiraProperties(true);
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "total": 1,
                          "issues": [
                            {
                              "id": "10020",
                              "key": "EXP-20",
                              "fields": { "summary": "Fallo al cargar perfil térmico" }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/admin/incidents/" + entity.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXP-20"));

        IncidentReport updated = incidentReportRepository.findById(entity.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.SYNCED, updated.getSyncStatus());
        assertEquals("EXP-20", updated.getJiraIssueKey());
        assertEquals("10020", updated.getJiraRequestId());
        assertEquals(2, updated.getSyncAttempts());
        assertNull(updated.getLastSyncError());

        List<AuditLog> audits = auditLogRepository.findAll();
        assertTrue(audits.stream().anyMatch(a -> "INCIDENT_SYNC_RETRY_REQUESTED".equals(a.getAction())
                && String.valueOf(entity.getId()).equals(a.getEntityId())));
        assertTrue(audits.stream().anyMatch(a -> "INCIDENT_SYNC_DUPLICATE_RECOVERED".equals(a.getAction())
                && String.valueOf(entity.getId()).equals(a.getEntityId())
                && a.getNewValue() != null && a.getNewValue().contains("EXP-20")));

        // Verifica que NO se ejecutó POST /rest/servicedeskapi/request
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C08: Retry no encuentra ticket en JQL -> procede a crear uno nuevo (EXP-28)")
    void incC08_retryFindsNoTicket_createsNewTicket() throws Exception {
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);
        IncidentReport entity = incidentReportRepository.findById(created.getId()).orElseThrow();
        entity.setSyncStatus(JiraSyncStatus.RETRY_PENDING);
        entity.setSyncAttempts(1);
        incidentReportRepository.saveAndFlush(entity);

        configureValidJiraProperties(true);

        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "total": 0, "issues": [] }
                        """, MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/4/requesttype/19/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildRequestTypeFieldsJson(), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "issueId": "10028",
                                  "issueKey": "EXP-28"
                                }
                                """));

        mockMvc.perform(post("/api/admin/incidents/" + entity.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXP-28"));

        IncidentReport updated = incidentReportRepository.findById(entity.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.SYNCED, updated.getSyncStatus());
        assertEquals("EXP-28", updated.getJiraIssueKey());
        assertEquals("10028", updated.getJiraRequestId());

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C09: Dos tickets Jira encontrados con el mismo correlationId -> ERROR de inconsistencia y NO crea tercero")
    void incC09_multipleTicketsFoundWithSameCorrelationId_marksConflictErrorAndDoesNotCreateThird() throws Exception {
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);
        IncidentReport entity = incidentReportRepository.findById(created.getId()).orElseThrow();
        entity.setSyncStatus(JiraSyncStatus.RETRY_PENDING);
        incidentReportRepository.saveAndFlush(entity);

        configureValidJiraProperties(true);

        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "total": 2,
                          "issues": [
                            { "id": "10020", "key": "EXP-20" },
                            { "id": "10021", "key": "EXP-21" }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/admin/incidents/" + entity.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("ERROR"));

        IncidentReport updated = incidentReportRepository.findById(entity.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.ERROR, updated.getSyncStatus());
        assertNull(updated.getJiraIssueKey());
        assertTrue(updated.getLastSyncError().contains("EXP-20"));
        assertTrue(updated.getLastSyncError().contains("EXP-21"));

        List<AuditLog> audits = auditLogRepository.findAll();
        assertTrue(audits.stream().anyMatch(a -> "INCIDENT_SYNC_DUPLICATE_CONFLICT".equals(a.getAction())
                && String.valueOf(entity.getId()).equals(a.getEntityId())));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C10: JIRA_ENABLED=false -> ninguna llamada HTTP externa y la incidencia queda PENDING")
    void incC10_jiraDisabled_noExternalHttpCallMade() throws Exception {
        jiraProperties.setEnabled(false);

        // No registramos expectativas en mockJiraServer: si hiciera cualquier llamada HTTP fallaría inmediatamente
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.jiraIssueKey").isEmpty());

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C11: Configuración incompleta con JIRA_ENABLED=true -> fallo controlado (ERROR) sin llamadas externas rotas")
    void incC11_missingConfigWhenJiraEnabled_marksControlledError() throws Exception {
        jiraProperties.setEnabled(true);
        jiraProperties.setBaseUrl("");
        jiraProperties.setApiToken("");

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("ERROR"))
                .andReturn();

        IncidentDTO dto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.ERROR, saved.getSyncStatus());
        assertTrue(saved.getLastSyncError().contains("JIRA_BASE_URL"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C12: Usuario operacional (QA) intenta retry-sync -> HTTP 403 Forbidden")
    void incC12_operationalUserAttemptsRetrySync_returns403() throws Exception {
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);

        mockMvc.perform(post("/api/admin/incidents/" + created.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("INC-C13: ADMINISTRADOR ejecuta retry-sync -> permitido (200 OK)")
    void incC13_administradorRetrySync_allowed() throws Exception {
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);

        configureValidJiraProperties(true);
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withSuccess("""
                        { "total": 1, "issues": [{ "id": "10030", "key": "EXP-30" }] }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/admin/incidents/" + created.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXP-30"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C14: SUPERADMIN ejecuta retry-sync -> permitido (200 OK)")
    void incC14_superAdminRetrySync_allowed() throws Exception {
        jiraProperties.setEnabled(false);
        IncidentDTO created = incidentService.createIncident(buildSampleRequest(), userQa.getEmail(), null);

        configureValidJiraProperties(true);
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(withSuccess("""
                        { "total": 1, "issues": [{ "id": "10031", "key": "EXP-31" }] }
                        """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/admin/incidents/" + created.getId() + "/retry-sync")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXP-31"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("INC-C15: Respuesta Jira, DTO, lastSyncError y logs de auditoría NUNCA exponen JIRA_API_TOKEN ni Authorization header")
    void incC15_jiraErrorAndDtoNeverExposeApiTokenOrAuthHeader() throws Exception {
        // Simulamos un fallo donde el mensaje de excepción contiene accidentalmente el token y una cabecera Basic
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search")))
                .andRespond(request -> {
                    throw new ResourceAccessException(
                            "Failed with header Authorization: Basic dGVzdDpzZWNyZXQ= and token " + TEST_SECRET_API_TOKEN
                    );
                });

        MvcResult mvcResult = mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.syncStatus").value("RETRY_PENDING"))
                .andReturn();

        String rawJsonResponse = mvcResult.getResponse().getContentAsString();
        assertFalse(rawJsonResponse.contains(TEST_SECRET_API_TOKEN), "El JSON del DTO nunca debe contener el JIRA_API_TOKEN");
        assertFalse(rawJsonResponse.contains("dGVzdDpzZWNyZXQ="), "El JSON del DTO nunca debe contener credenciales Basic");
        assertFalse(rawJsonResponse.contains("lastSyncError"), "El DTO operacional no debe serializar lastSyncError");

        IncidentDTO dto = objectMapper.readValue(rawJsonResponse, IncidentDTO.class);
        IncidentReport saved = incidentReportRepository.findById(dto.getId()).orElseThrow();

        assertNotNull(saved.getLastSyncError());
        assertFalse(saved.getLastSyncError().contains(TEST_SECRET_API_TOKEN), "lastSyncError debe redactar el token");
        assertFalse(saved.getLastSyncError().contains("dGVzdDpzZWNyZXQ="), "lastSyncError debe redactar cabeceras Basic");
        assertTrue(saved.getLastSyncError().contains("[REDACTED]"));

        // Verificar también JiraProperties.toString() y AuditLog
        assertFalse(jiraProperties.toString().contains(TEST_SECRET_API_TOKEN));
        List<AuditLog> audits = auditLogRepository.findAll();
        for (AuditLog audit : audits) {
            if (audit.getNewValue() != null) {
                assertFalse(audit.getNewValue().contains(TEST_SECRET_API_TOKEN),
                        "AuditLog nunca debe almacenar JIRA_API_TOKEN");
            }
        }

        mockJiraServer.verify();
    }
}
