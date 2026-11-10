package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.CreateServiceRequestDTO;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.entity.Role;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.*;
import com.exportrace.repository.RoleRepository;
import com.exportrace.repository.ServiceRequestCommentRepository;
import com.exportrace.repository.ServiceRequestRepository;
import com.exportrace.repository.UserRepository;
import com.exportrace.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite completa de pruebas automatizadas para Fase D2 (SRJ-01..SRJ-16) y Fase D3 (SYNC-IN-01..SYNC-IN-10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ServiceRequestCatalogAndSyncIntegrationTest {

    private static final String TEST_JIRA_BASE_URL = "https://utp-team-f25gkbq9.atlassian.net";
    private static final String TEST_SERVICE_DESK_ID = "2";
    private static final String TEST_INCIDENT_REQUEST_TYPE_ID = "10083";
    private static final String TEST_ACCOUNT_EMAIL = "jira-bot@exportrace.pe";
    private static final String TEST_SECRET_API_TOKEN = "ATATT3xFfGF0_CATALOG_SECRET_TOKEN_DO_NOT_LEAK";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JiraProperties jiraProperties;

    @Autowired
    private JiraServiceRequestService jiraServiceRequestService;

    @Autowired
    private JiraIncidentService jiraIncidentService;

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private ServiceRequestSyncService serviceRequestSyncService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceRequestCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private MockRestServiceServer mockJiraServer;
    private User userA;
    private User userB;
    private User userAdmin;
    private String tokenA;
    private String tokenB;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockJiraServer = MockRestServiceServer.bindTo(builder).build();
        RestClient sharedMockClient = builder.build();
        jiraServiceRequestService.setRestClientForTesting(sharedMockClient);
        jiraIncidentService.setRestClientForTesting(sharedMockClient);

        configureJiraProperties(true);

        Role roleProd = roleRepository.findByNombre("PRODUCCION").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("PRODUCCION");
            r.setDescripcion("Operador de Producción");
            r.setActivo(true);
            return roleRepository.save(r);
        });

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

        long suffix = System.nanoTime();

        userA = new User();
        userA.setNombre("Carlos");
        userA.setApellido("Paredes");
        userA.setEmail("usera.d2." + suffix + "@exportrace.pe");
        userA.setPasswordHash(passwordEncoder.encode("Pass123"));
        userA.setRole(roleProd);
        userA.setActivo(true);
        userA = userRepository.save(userA);
        tokenA = jwtUtil.generateAccessToken(userA, "sess-d2-a-" + suffix);

        userB = new User();
        userB.setNombre("Lucia");
        userB.setApellido("Ramos");
        userB.setEmail("userb.d2." + suffix + "@exportrace.pe");
        userB.setPasswordHash(passwordEncoder.encode("Pass123"));
        userB.setRole(roleQa);
        userB.setActivo(true);
        userB = userRepository.save(userB);
        tokenB = jwtUtil.generateAccessToken(userB, "sess-d2-b-" + suffix);

        userAdmin = new User();
        userAdmin.setNombre("Admin");
        userAdmin.setApellido("Soporte");
        userAdmin.setEmail("admin.d2." + suffix + "@exportrace.pe");
        userAdmin.setPasswordHash(passwordEncoder.encode("Admin123"));
        userAdmin.setRole(roleAdmin);
        userAdmin.setActivo(true);
        userAdmin = userRepository.save(userAdmin);
        tokenAdmin = jwtUtil.generateAccessToken(userAdmin, "sess-d2-admin-" + suffix);
    }

    @AfterEach
    void tearDown() {
        jiraServiceRequestService.setRestClientForTesting(null);
        jiraIncidentService.setRestClientForTesting(null);
        configureJiraProperties(false);
    }

    private void configureJiraProperties(boolean enabled) {
        jiraProperties.setEnabled(enabled);
        jiraProperties.setBaseUrl(TEST_JIRA_BASE_URL);
        jiraProperties.setServiceDeskId(TEST_SERVICE_DESK_ID);
        jiraProperties.setRequestTypeId(TEST_INCIDENT_REQUEST_TYPE_ID);
        jiraProperties.setAccountEmail(TEST_ACCOUNT_EMAIL);
        jiraProperties.setApiToken(TEST_SECRET_API_TOKEN);
        jiraProperties.setCorrelationFieldId("customfield_10221");
        jiraProperties.setSrInf01RequestTypeId("10116");
        jiraProperties.setSrInf01LotCodeFieldId("customfield_10263");
        jiraProperties.setSrInf01RequestedInfoFieldId("customfield_10264");
        jiraProperties.setSrInf01AdditionalDetailFieldId("customfield_10265");
        jiraProperties.setSrInf01CorrelationFieldId("customfield_10266");
    }

    private String preflightFieldsJsonFor(String requestTypeId) {
        return switch (requestTypeId) {
            case "10116" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10263", "required": true },
                        { "fieldId": "customfield_10264", "required": true },
                        { "fieldId": "customfield_10265", "required": false },
                        { "fieldId": "customfield_10266", "required": false }
                    ]}""";
            case "10119" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10267", "required": true },
                        { "fieldId": "customfield_10268", "required": true },
                        { "fieldId": "customfield_10269", "required": false },
                        { "fieldId": "customfield_10221", "required": false }
                    ]}""";
            case "10120" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10263", "required": true },
                        { "fieldId": "customfield_10270", "required": true },
                        { "fieldId": "customfield_10271", "required": true },
                        { "fieldId": "attachment", "required": false },
                        { "fieldId": "customfield_10221", "required": false }
                    ]}""";
            case "10121" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10263", "required": true },
                        { "fieldId": "customfield_10272", "required": true },
                        { "fieldId": "customfield_10273", "required": true },
                        { "fieldId": "customfield_10274", "required": true },
                        { "fieldId": "customfield_10258", "required": true },
                        { "fieldId": "attachment", "required": false },
                        { "fieldId": "customfield_10221", "required": false }
                    ]}""";
            case "10117" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10254", "required": true },
                        { "fieldId": "customfield_10255", "required": true },
                        { "fieldId": "customfield_10256", "required": true },
                        { "fieldId": "customfield_10257", "required": true },
                        { "fieldId": "customfield_10258", "required": true },
                        { "fieldId": "attachment", "required": false },
                        { "fieldId": "customfield_10221", "required": false }
                    ]}""";
            case "10118" -> """
                    { "requestTypeFields": [
                        { "fieldId": "summary", "required": true },
                        { "fieldId": "customfield_10260", "required": true },
                        { "fieldId": "customfield_10255", "required": true },
                        { "fieldId": "customfield_10256", "required": true },
                        { "fieldId": "customfield_10261", "required": true },
                        { "fieldId": "customfield_10257", "required": true },
                        { "fieldId": "customfield_10262", "required": true },
                        { "fieldId": "description", "required": true },
                        { "fieldId": "attachment", "required": false },
                        { "fieldId": "customfield_10221", "required": false }
                    ]}""";
            default -> "{\"requestTypeFields\":[]}";
        };
    }

    private AtomicReference<String> expectCreateRequestFlow(String requestTypeId, String issueKey, String issueId) {
        AtomicReference<String> bodyRef = new AtomicReference<>();
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/" + requestTypeId + "/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(preflightFieldsJsonFor(requestTypeId), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(req -> {
                    bodyRef.set(req.getBody().toString());
                    return withSuccess("{\"issueKey\":\"" + issueKey + "\",\"issueId\":\"" + issueId + "\"}", MediaType.APPLICATION_JSON)
                            .createResponse(req);
                });
        return bodyRef;
    }

    @Test
    @DisplayName("SRJ-01 & SRJ-16: SR-INF-01 mapea campos correctamente sin regresión")
    void srj01_16_srInf01MapsFieldsCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10116", "EXT-201", "30201");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Seguimiento completo de lote EXP-2026-001",
                "EXP-2026-001",
                "Trazabilidad de producción y QA",
                "Consulta previa a despacho"
        );

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-201", created.getJiraIssueKey());
        assertEquals("4 horas hábiles", created.getSlaTarget());
        assertFalse(created.isRequiresApproval());
        assertEquals("NOT_REQUIRED", created.getApprovalStatus());

        JsonNode payload = objectMapper.readTree(captured.get());
        assertEquals("10116", payload.path("requestTypeId").asText());
        JsonNode fields = payload.path("requestFieldValues");
        assertEquals("Seguimiento completo de lote EXP-2026-001", fields.path("summary").asText());
        assertEquals("EXP-2026-001", fields.path("customfield_10263").asText());
        assertEquals("Trazabilidad de producción y QA", fields.path("customfield_10264").asText());
        assertEquals("Consulta previa a despacho", fields.path("customfield_10265").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10266").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-02 & SRJ-07 & SRJ-08: SR-INF-02 mapea campos correctamente, usa validValues real para customfield_10268 e incluye correlationId en customfield_10221")
    void srj02_07_08_srInf02MapsFieldsAndOptionIdCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10119", "EXT-202", "30202");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-02");
        req.setSummary("Consulta de expediente documental lote EXP-2026-002");
        req.setLotCode("EXP-2026-002");
        req.setDocumentQueryType("Estado del expediente digital");
        req.setAdditionalDetail("Verificar certificado fitosanitario");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-202", created.getJiraIssueKey());
        assertEquals("10119", created.getJiraRequestTypeId());
        assertEquals("4 horas hábiles", created.getSlaTarget());

        JsonNode payload = objectMapper.readTree(captured.get());
        assertEquals("10119", payload.path("requestTypeId").asText());
        JsonNode fields = payload.path("requestFieldValues");
        assertEquals("Consulta de expediente documental lote EXP-2026-002", fields.path("summary").asText());
        assertEquals("EXP-2026-002", fields.path("customfield_10267").asText());
        assertEquals("10071", fields.path("customfield_10268").path("id").asText());
        assertEquals("Verificar certificado fitosanitario", fields.path("customfield_10269").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10221").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-03: SR-INF-03 mapea campos correctamente (customfield_10263, customfield_10270, customfield_10271, customfield_10221)")
    void srj03_srInf03MapsFieldsCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10120", "EXT-203", "30203");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-03");
        req.setSummary("Inconsistencia en peso neto del lote EXP-2026-003");
        req.setLotCode("EXP-2026-003");
        req.setObservedField("Peso neto (kg)");
        req.setProblemDescription("El peso registrado en sistema difiere de la guía de recepción.");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-203", created.getJiraIssueKey());
        assertEquals("4 horas hábiles para resultado inicial", created.getSlaTarget());
        assertFalse(created.isRequiresApproval());

        JsonNode fields = objectMapper.readTree(captured.get()).path("requestFieldValues");
        assertEquals("EXP-2026-003", fields.path("customfield_10263").asText());
        assertEquals("Peso neto (kg)", fields.path("customfield_10270").asText());
        assertEquals("El peso registrado en sistema difiere de la guía de recepción.", fields.path("customfield_10271").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10221").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-04: SR-INF-04 mapea campos correctamente y marca requiresApproval=true / PENDING_APPROVAL")
    void srj04_srInf04MapsFieldsCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10121", "EXT-204", "30204");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-04");
        req.setSummary("Corrección de variedad registrada en lote EXP-2026-004");
        req.setLotCode("EXP-2026-004");
        req.setFieldToCorrect("Variedad de uva");
        req.setCurrentValue("Red Globe");
        req.setProposedValue("Sweet Globe");
        req.setJustification("Acta de cosecha firmada por jefe de campo confirma Sweet Globe.");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-204", created.getJiraIssueKey());
        assertEquals("1 día hábil desde aprobación", created.getSlaTarget());
        assertTrue(created.isRequiresApproval());
        assertEquals("PENDING_APPROVAL", created.getApprovalStatus());

        JsonNode fields = objectMapper.readTree(captured.get()).path("requestFieldValues");
        assertEquals("EXP-2026-004", fields.path("customfield_10263").asText());
        assertEquals("Variedad de uva", fields.path("customfield_10272").asText());
        assertEquals("Red Globe", fields.path("customfield_10273").asText());
        assertEquals("Sweet Globe", fields.path("customfield_10274").asText());
        assertEquals("Acta de cosecha firmada por jefe de campo confirma Sweet Globe.", fields.path("customfield_10258").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10221").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-05: SR-ACC-01 mapea campos y validValues reales de Área y Rol requerido")
    void srj05_srAcc01MapsFieldsAndValidValuesCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10117", "EXT-205", "30205");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-ACC-01");
        req.setSummary("Activación de cuenta para nuevo inspector QA");
        req.setWorkerName("Valeria Quispe");
        req.setInstitutionalEmail("vquispe@exportrace.pe");
        req.setArea("Calidad");
        req.setRequiredRole("QA");
        req.setJustification("Incorporación al turno mañana de aseguramiento de calidad.");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-205", created.getJiraIssueKey());
        assertEquals("ACCESS", created.getCategoryKey());
        assertTrue(created.isRequiresApproval());
        assertEquals("PENDING_APPROVAL", created.getApprovalStatus());

        JsonNode fields = objectMapper.readTree(captured.get()).path("requestFieldValues");
        assertEquals("Valeria Quispe", fields.path("customfield_10254").asText());
        assertEquals("vquispe@exportrace.pe", fields.path("customfield_10255").asText());
        assertEquals("10055", fields.path("customfield_10256").path("id").asText());
        assertEquals("10061", fields.path("customfield_10257").path("id").asText());
        assertEquals("Incorporación al turno mañana de aseguramiento de calidad.", fields.path("customfield_10258").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10221").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-06: SR-ACC-02 mapea campos y validValues reales de Área, Tipo de solicitud, Rol actual y Rol/permisos requeridos")
    void srj06_srAcc02MapsFieldsAndValidValuesCorrectly() throws Exception {
        AtomicReference<String> captured = expectCreateRequestFlow("10118", "EXT-206", "30206");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-ACC-02");
        req.setSummary("Cambio de permisos por rotación a Logística");
        req.setAffectedUser("mcastro@exportrace.pe");
        req.setInstitutionalEmail("mcastro@exportrace.pe");
        req.setArea("Logística");
        req.setAccessRequestType("Cambio de permisos");
        req.setCurrentRoleField("PRODUCCION");
        req.setRequiredPermissions("LOGISTICA");
        req.setReason("Traslado interno autorizado por jefatura de operaciones.");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-206", created.getJiraIssueKey());
        assertTrue(created.isRequiresApproval());
        assertEquals("PENDING_APPROVAL", created.getApprovalStatus());

        JsonNode fields = objectMapper.readTree(captured.get()).path("requestFieldValues");
        assertEquals("mcastro@exportrace.pe", fields.path("customfield_10260").asText());
        assertEquals("mcastro@exportrace.pe", fields.path("customfield_10255").asText());
        assertEquals("10056", fields.path("customfield_10256").path("id").asText());
        assertEquals("10066", fields.path("customfield_10261").path("id").asText());
        assertEquals("10054", fields.path("customfield_10257").path("id").asText());
        assertEquals("10074", fields.path("customfield_10262").path("id").asText());
        assertEquals("Traslado interno autorizado por jefatura de operaciones.", fields.path("description").asText());
        assertEquals(created.getCorrelationId(), fields.path("customfield_10221").asText());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-09: Jira 429 Too Many Requests produce RETRY_PENDING")
    void srj09_jira429ProducesRetryPending() {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10119/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(preflightFieldsJsonFor("10119"), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessages\":[\"Rate limit exceeded\"]}"));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-02");
        req.setSummary("Consulta de expediente con límite de tasa");
        req.setLotCode("EXP-2026-009");
        req.setDocumentQueryType("Consulta completa");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("RETRY_PENDING", created.getSyncStatus());
        assertNotNull(created.getId());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-10: Jira 401/403 produce ERROR sin perder el registro local")
    void srj10_jira401Or403ProducesError() {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessages\":[\"Forbidden\"]}"));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-03");
        req.setSummary("Problema en lote con error de permisos Jira");
        req.setLotCode("EXP-2026-010");
        req.setObservedField("Temperatura inicial");
        req.setProblemDescription("Dato fuera de rango");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("ERROR", created.getSyncStatus());
        assertNotNull(created.getId());
        ServiceRequest persisted = serviceRequestRepository.findById(created.getId()).orElseThrow();
        assertFalse(persisted.getLastSyncError().contains(TEST_SECRET_API_TOKEN));
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-11 & SRJ-12 & SRJ-14: Retry y Lookup-Before-Create por correlationId recuperan ticket existente sin duplicar, y Admin puede ver y reintentar")
    void srj11_12_14_retryAndCorrelationIdRecoveryAndAdminAccess() throws Exception {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Timeout")));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-ACC-01");
        req.setSummary("Creación de cuenta con recuperación idempotente");
        req.setWorkerName("Pedro Luna");
        req.setInstitutionalEmail("pluna@exportrace.pe");
        req.setArea("Producción");
        req.setRequiredRole("PRODUCCION");
        req.setJustification("Nuevo operador de línea");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("RETRY_PENDING", created.getSyncStatus());
        mockJiraServer.verify();

        // Admin puede listar todas las solicitudes
        mockMvc.perform(get("/api/admin/service-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // En el retry, Jira ya tiene EXT-211 con el correlationId
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"issues\":[{\"key\":\"EXT-211\",\"id\":\"30211\",\"fields\":{\"summary\":\"Creación\"}}]}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/api/admin/service-requests/" + created.getId() + "/retry-sync")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXT-211"));

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SRJ-13: Usuario A no puede ver solicitud de Usuario B (403 Forbidden)")
    void srj13_userACannotViewUserBRequest() throws Exception {
        configureJiraProperties(false);

        CreateServiceRequestDTO reqB = new CreateServiceRequestDTO();
        reqB.setServiceCode("SR-INF-02");
        reqB.setSummary("Solicitud de Usuario B");
        reqB.setLotCode("EXP-2026-013");
        reqB.setDocumentQueryType("Documentos registrados");

        ServiceRequestDTO createdByB = serviceRequestService.createServiceRequest(reqB, userB.getEmail(), null);

        mockMvc.perform(get("/api/service-requests/" + createdByB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SRJ-15: IncidentReport continúa funcionando sin regresión")
    void srj15_incidentReportWorksWithoutRegression() {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10083/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "requestTypeFields": [
                            { "fieldId": "summary", "required": true },
                            { "fieldId": "description", "required": true },
                            { "fieldId": "customfield_10221", "required": false }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"issueKey\":\"EXT-215\",\"issueId\":\"30215\"}", MediaType.APPLICATION_JSON));

        CreateIncidentRequest incReq = new CreateIncidentRequest(
                IncidentCategory.COLD_CHAIN,
                "Alerta en sensor de cámara",
                "Sensor no reporta lectura desde hace 10 minutos.",
                IncidentImpact.MEDIUM,
                WorkContinuity.NO_PUEDE_CONTINUAR,
                "COLD_CHAIN",
                "/cold-chain",
                null
        );

        IncidentDTO dto = incidentService.createIncident(incReq, userA.getEmail(), null);
        assertEquals("SYNCED", dto.getSyncStatus());
        assertEquals("EXT-215", dto.getJiraIssueKey());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SYNC-IN-01 & SYNC-IN-02 & SYNC-IN-03 & SYNC-IN-04: Refresh desde Jira actualiza estado, importa solo comentarios públicos (ignora public=false) y no duplica comentarios al repetir refresh")
    void syncIn01_02_03_04_refreshUpdatesStatusAndPublicCommentsIdempotently() throws Exception {
        expectCreateRequestFlow("10119", "EXT-301", "40301");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-02");
        req.setSummary("Consulta de expediente para refresh");
        req.setLotCode("EXP-2026-301");
        req.setDocumentQueryType("Consulta completa");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        mockJiraServer.verify();

        String jiraRequestSnapshotJson = """
                {
                  "issueId": "40301",
                  "issueKey": "EXT-301",
                  "currentStatus": {
                    "status": "EN REVISIÓN",
                    "statusCategory": "INDETERMINATE"
                  }
                }
                """;

        String jiraSlaJson = """
                {
                  "values": [
                    {
                      "name": "Time to resolution",
                      "ongoingCycle": {
                        "remainingTime": { "friendly": "3h 25m" },
                        "breached": false
                      }
                    }
                  ]
                }
                """;

        String jiraCommentsJson = """
                {
                  "values": [
                    {
                      "id": "9001",
                      "body": "Hemos verificado el expediente digital del lote EXP-2026-301 y se encuentra completo.",
                      "public": true,
                      "author": { "displayName": "Mesa de Ayuda ExporTrace" },
                      "created": { "iso8601": "2026-11-09T18:00:00.000-0500" }
                    },
                    {
                      "id": "9002",
                      "body": "NOTA INTERNA PRIVADA: Revisar logs de base de datos interna.",
                      "public": false,
                      "author": { "displayName": "Agente TI Interno" },
                      "created": { "iso8601": "2026-11-09T18:05:00.000-0500" }
                    }
                  ]
                }
                """;

        // Primer refresh (SR-INF-02 no requiere aprobación: consulta request, comment y sla)
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraRequestSnapshotJson, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301/comment"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraCommentsJson, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301/sla"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraSlaJson, MediaType.APPLICATION_JSON));

        ServiceRequestDTO refreshed1 = serviceRequestSyncService.refreshFromJira(created.getId(), userA.getEmail(), null);
        assertEquals("IN_REVIEW", refreshed1.getStatus());
        assertEquals("EN REVISIÓN", refreshed1.getJiraStatus());
        assertTrue(refreshed1.getJiraSlaSummary().contains("3h 25m"));
        assertEquals(1, refreshed1.getComments().size(), "Solo debe importarse el comentario público (public=true)");
        assertEquals("9001", refreshed1.getComments().get(0).getJiraCommentId());
        assertFalse(refreshed1.getComments().get(0).getBody().contains("NOTA INTERNA PRIVADA"));
        mockJiraServer.verify();

        // Segundo refresh repetido -> no duplica comentarios
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraRequestSnapshotJson, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301/comment"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraCommentsJson, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-301/sla"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jiraSlaJson, MediaType.APPLICATION_JSON));

        ServiceRequestDTO refreshed2 = serviceRequestSyncService.refreshFromJira(created.getId(), userA.getEmail(), null);
        assertEquals(1, refreshed2.getComments().size(), "El refresh repetido no debe duplicar comentarios");
        assertEquals(1, commentRepository.findByServiceRequestIdAndPublicCommentTrueOrderByCreatedAtAsc(created.getId()).size());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SYNC-IN-05 & SYNC-IN-06: Aprobación y rechazo en Jira se reflejan localmente en approvalStatus y status")
    void syncIn05_06_approvalAndRejectionReflectedLocally() {
        expectCreateRequestFlow("10121", "EXT-305", "40305");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO();
        req.setServiceCode("SR-INF-04");
        req.setSummary("Corrección de lote sujeta a aprobación");
        req.setLotCode("EXP-2026-305");
        req.setFieldToCorrect("Cantidad de cajas");
        req.setCurrentValue("120");
        req.setProposedValue("125");
        req.setJustification("Conteo físico verificado en cámara.");

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userA.getEmail(), null);
        assertEquals("PENDING_APPROVAL", created.getApprovalStatus());
        mockJiraServer.verify();

        // SYNC-IN-05: Aprobada en Jira (SR-INF-04 requiere aprobación: consulta request, comment, approval y sla)
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "issueId": "40305",
                          "issueKey": "EXT-305",
                          "currentStatus": { "status": "EN IMPLEMENTACIÓN", "statusCategory": "INDETERMINATE" }
                        }
                        """, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/comment"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/approval"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "values": [ { "id": "1", "finalDecision": "approved" } ] }
                        """, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/sla"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

        ServiceRequestDTO afterApprove = serviceRequestSyncService.refreshFromJira(created.getId(), userA.getEmail(), null);
        assertEquals("APPROVED", afterApprove.getApprovalStatus());
        assertEquals("IN_PROGRESS", afterApprove.getStatus());
        mockJiraServer.verify();

        // SYNC-IN-06: Rechazada en Jira
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "issueId": "40305",
                          "issueKey": "EXT-305",
                          "currentStatus": { "status": "RECHAZADA", "statusCategory": "DONE" }
                        }
                        """, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/comment"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/approval"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        { "values": [ { "id": "1", "finalDecision": "declined" } ] }
                        """, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-305/sla"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

        ServiceRequestDTO afterReject = serviceRequestSyncService.refreshFromJira(created.getId(), userA.getEmail(), null);
        assertEquals("REJECTED", afterReject.getApprovalStatus());
        assertEquals("REJECTED", afterReject.getStatus());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("SYNC-IN-07 & SYNC-IN-08 & SYNC-IN-09 & SYNC-IN-10: Jira caído conserva datos locales, Usuario A no puede refrescar solicitud de Usuario B, Admin sí puede refrescar")
    void syncIn07_08_09_10_resilienceAndRbacOnRefresh() throws Exception {
        expectCreateRequestFlow("10116", "EXT-307", "40307");

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Solicitud de Usuario B para prueba de refresh",
                "EXP-2026-307",
                "Historial completo",
                "Detalle"
        );
        ServiceRequestDTO createdByB = serviceRequestService.createServiceRequest(req, userB.getEmail(), null);
        mockJiraServer.verify();

        // SYNC-IN-08: Usuario A intenta refrescar solicitud de Usuario B -> 403 Forbidden
        mockMvc.perform(post("/api/service-requests/" + createdByB.getId() + "/refresh-from-jira")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // SYNC-IN-07: Jira caído durante refresh conserva datos locales sin lanzar 500
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-307"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(post("/api/service-requests/" + createdByB.getId() + "/refresh-from-jira")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(createdByB.getCode()))
                .andExpect(jsonPath("$.jiraIssueKey").value("EXT-307"));
        mockJiraServer.verify();

        // SYNC-IN-09: Admin puede refrescar la solicitud y actualizar a COMPLETED
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-307"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "issueId": "40307",
                          "issueKey": "EXT-307",
                          "currentStatus": { "status": "COMPLETADA", "statusCategory": "DONE" }
                        }
                        """, MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-307/comment"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));
        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request/EXT-307/sla"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/service-requests/" + createdByB.getId() + "/refresh-from-jira")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.jiraStatus").value("COMPLETADA"));
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("WORKFLOW-COMPAT-01: Compatibilidad de mapeo con estados actuales y modelo objetivo de Workflows Jira (3.1, 3.2 y 3.3)")
    void workflowCompat01_jiraWorkflowTargetAndCurrentStatusesMapCleanly() {
        // 3.2 Solicitudes directas
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.REGISTERED,
                jiraServiceRequestService.mapJiraStatusToLocal("SOLICITUD REGISTRADA", "INDETERMINATE", "NOT_REQUIRED", false));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.IN_REVIEW,
                jiraServiceRequestService.mapJiraStatusToLocal("EN REVISIÓN", "INDETERMINATE", "NOT_REQUIRED", false));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.IN_PROGRESS,
                jiraServiceRequestService.mapJiraStatusToLocal("EN IMPLEMENTACIÓN", "INDETERMINATE", "NOT_REQUIRED", false));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.COMPLETED,
                jiraServiceRequestService.mapJiraStatusToLocal("COMPLETADA", "DONE", "NOT_REQUIRED", false));

        // 3.3 Solicitudes con aprobación
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.PENDING_APPROVAL,
                jiraServiceRequestService.mapJiraStatusToLocal("PENDIENTE DE APROBACIÓN", "INDETERMINATE", "PENDING_APPROVAL", true));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.IN_PROGRESS,
                jiraServiceRequestService.mapJiraStatusToLocal("EN IMPLEMENTACIÓN", "INDETERMINATE", "APPROVED", true));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.REJECTED,
                jiraServiceRequestService.mapJiraStatusToLocal("RECHAZADA", "DONE", "REJECTED", true));
        assertEquals(com.exportrace.entity.enums.ServiceRequestStatus.REJECTED,
                jiraServiceRequestService.mapJiraStatusToLocal("COMPLETADA", "DONE", "REJECTED", true));

        // 3.1 Incidentes
        assertEquals(com.exportrace.entity.enums.IncidentStatus.REPORTED,
                jiraIncidentService.mapJiraIncidentStatusToLocal("REPORTADA", "NEW"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.TRIAGE,
                jiraIncidentService.mapJiraIncidentStatusToLocal("EN TRIAJE", "INDETERMINATE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.IN_PROGRESS,
                jiraIncidentService.mapJiraIncidentStatusToLocal("EN PROGRESO", "INDETERMINATE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.WAITING_USER,
                jiraIncidentService.mapJiraIncidentStatusToLocal("EN ESPERA DEL USUARIO", "INDETERMINATE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.ESCALATED,
                jiraIncidentService.mapJiraIncidentStatusToLocal("ESCALADA", "INDETERMINATE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.RESOLVED,
                jiraIncidentService.mapJiraIncidentStatusToLocal("RESUELTA", "DONE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.CLOSED,
                jiraIncidentService.mapJiraIncidentStatusToLocal("CERRADA", "DONE"));
        assertEquals(com.exportrace.entity.enums.IncidentStatus.REOPENED,
                jiraIncidentService.mapJiraIncidentStatusToLocal("REABIERTA", "INDETERMINATE"));
    }
}
