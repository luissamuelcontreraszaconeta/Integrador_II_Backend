package com.exportrace.service;

import com.exportrace.config.JiraProperties;
import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.CreateServiceRequestDTO;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.dto.ServiceRequestDTO;
import com.exportrace.entity.AuditLog;
import com.exportrace.entity.Role;
import com.exportrace.entity.ServiceRequest;
import com.exportrace.entity.User;
import com.exportrace.entity.enums.*;
import com.exportrace.repository.AuditLogRepository;
import com.exportrace.repository.RoleRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
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
 * Suite de pruebas automatizadas del piloto controlado SR-INF-01:
 * Cubre PILOT-01 a PILOT-10.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SrInf01PilotIntegrationTest {

    private static final String TEST_JIRA_BASE_URL = "https://utp-team-f25gkbq9.atlassian.net";
    private static final String TEST_SERVICE_DESK_ID = "2";
    private static final String TEST_SR_INF_01_REQUEST_TYPE_ID = "10116";
    private static final String TEST_INCIDENT_REQUEST_TYPE_ID = "10083";
    private static final String TEST_ACCOUNT_EMAIL = "jira-bot@exportrace.pe";
    private static final String TEST_SECRET_API_TOKEN = "ATATT3xFfGF0_PILOT_SECRET_TOKEN_DO_NOT_LEAK";

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private MockRestServiceServer mockJiraServer;
    private User userProd;
    private User userQa;
    private User userAdmin;
    private String tokenProd;
    private String tokenQa;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockJiraServer = MockRestServiceServer.bindTo(builder).build();
        RestClient sharedMockClient = builder.build();
        jiraServiceRequestService.setRestClientForTesting(sharedMockClient);
        jiraIncidentService.setRestClientForTesting(sharedMockClient);

        configurePilotJiraProperties(true);

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

        userProd = new User();
        userProd.setNombre("Jorge");
        userProd.setApellido("Salazar");
        userProd.setEmail("prod.pilot." + suffix + "@exportrace.pe");
        userProd.setPasswordHash(passwordEncoder.encode("Prod123"));
        userProd.setRole(roleProd);
        userProd.setActivo(true);
        userProd = userRepository.save(userProd);
        tokenProd = jwtUtil.generateAccessToken(userProd, "sess-pilot-prod-" + suffix);

        userQa = new User();
        userQa.setNombre("Elena");
        userQa.setApellido("Vargas");
        userQa.setEmail("qa.pilot." + suffix + "@exportrace.pe");
        userQa.setPasswordHash(passwordEncoder.encode("QA123"));
        userQa.setRole(roleQa);
        userQa.setActivo(true);
        userQa = userRepository.save(userQa);
        tokenQa = jwtUtil.generateAccessToken(userQa, "sess-pilot-qa-" + suffix);

        userAdmin = new User();
        userAdmin.setNombre("Roberto");
        userAdmin.setApellido("Mendoza");
        userAdmin.setEmail("admin.pilot." + suffix + "@exportrace.pe");
        userAdmin.setPasswordHash(passwordEncoder.encode("Admin123"));
        userAdmin.setRole(roleAdmin);
        userAdmin.setActivo(true);
        userAdmin = userRepository.save(userAdmin);
        tokenAdmin = jwtUtil.generateAccessToken(userAdmin, "sess-pilot-admin-" + suffix);
    }

    @AfterEach
    void tearDown() {
        jiraServiceRequestService.setRestClientForTesting(null);
        jiraIncidentService.setRestClientForTesting(null);
        configurePilotJiraProperties(false);
    }

    private void configurePilotJiraProperties(boolean enabled) {
        jiraProperties.setEnabled(enabled);
        jiraProperties.setBaseUrl(TEST_JIRA_BASE_URL);
        jiraProperties.setServiceDeskId(TEST_SERVICE_DESK_ID);
        jiraProperties.setRequestTypeId(TEST_INCIDENT_REQUEST_TYPE_ID);
        jiraProperties.setAccountEmail(TEST_ACCOUNT_EMAIL);
        jiraProperties.setApiToken(TEST_SECRET_API_TOKEN);
        jiraProperties.setCorrelationFieldId("customfield_10221");
        jiraProperties.setSrInf01RequestTypeId(TEST_SR_INF_01_REQUEST_TYPE_ID);
        jiraProperties.setSrInf01LotCodeFieldId("customfield_10263");
        jiraProperties.setSrInf01RequestedInfoFieldId("customfield_10264");
        jiraProperties.setSrInf01AdditionalDetailFieldId("customfield_10265");
        jiraProperties.setSrInf01CorrelationFieldId("customfield_10266");
    }

    private String buildPreflightFieldsResponseJson(boolean include10221) {
        String extra10221 = include10221
                ? """
                  ,
                  {
                    "fieldId": "customfield_10221",
                    "name": "ExporTrace Correlation ID",
                    "required": false
                  }
                  """
                : "";
        return """
            {
              "requestTypeFields": [
                { "fieldId": "summary", "name": "Resumen", "required": true },
                { "fieldId": "customfield_10263", "name": "Código del lote", "required": true },
                { "fieldId": "customfield_10264", "name": "Información requerida", "required": true },
                { "fieldId": "customfield_10265", "name": "Detalle adicional", "required": false },
                { "fieldId": "customfield_10266", "name": "ExporTrace Correlation ID", "required": false }
                %s
              ]
            }
            """.formatted(extra10221);
    }

    @Test
    @DisplayName("PILOT-01: SR-INF-01 se guarda localmente antes de invocar Jira (Store-and-Forward)")
    void pilot01_srInf01SavedLocallyBeforeJiraCall() {
        // Verifica durante la llamada a Jira que el registro local ya existe en SQLite
        AtomicReference<Boolean> existedInDbDuringJiraCall = new AtomicReference<>(false);

        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    long countInDb = serviceRequestRepository.count();
                    existedInDbDuringJiraCall.set(countInDb > 0);
                    return withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON).createResponse(request);
                });

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10116/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildPreflightFieldsResponseJson(false), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"issueKey\":\"EXT-101\",\"issueId\":\"20101\"}", MediaType.APPLICATION_JSON));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Consulta de seguimiento de lote EXP-2026-001",
                "EXP-2026-001",
                "Estado actual e historial de trazabilidad",
                "Revisión previa a embarque"
        );

        ServiceRequestDTO result = serviceRequestService.createServiceRequest(req, userProd.getEmail(), null);

        assertTrue(existedInDbDuringJiraCall.get(), "La solicitud SR-INF-01 debe existir en SQLite antes de llamar a Jira");
        assertNotNull(result.getId());
        assertTrue(result.getCode().startsWith("SR-2026-"));
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("PILOT-02 & PILOT-03 & PILOT-04: Payload Jira usa requestTypeId 10116, envía todos los fieldIds reales del preflight y correlationId en customfield_10221 / customfield_10266")
    void pilot02_03_04_jiraPayloadUsesRequestType10116AndPreflightFieldIdsAndCorrelationId() throws Exception {
        AtomicReference<String> capturedPostBody = new AtomicReference<>();

        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10116/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildPreflightFieldsResponseJson(true), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    capturedPostBody.set(request.getBody().toString());
                    return withSuccess("{\"issueKey\":\"EXT-102\",\"issueId\":\"20102\"}", MediaType.APPLICATION_JSON)
                            .createResponse(request);
                });

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Seguimiento completo del lote EXP-2026-002",
                "EXP-2026-002",
                "Producción, QA, cadena de frío y despacho",
                "Detalle adicional de prueba piloto"
        );

        ServiceRequestDTO dto = serviceRequestService.createServiceRequest(req, userProd.getEmail(), null);
        assertNotNull(capturedPostBody.get());

        JsonNode payload = objectMapper.readTree(capturedPostBody.get());
        // PILOT-02
        assertEquals("2", payload.path("serviceDeskId").asText());
        assertEquals("10116", payload.path("requestTypeId").asText());

        // PILOT-03
        JsonNode fields = payload.path("requestFieldValues");
        assertEquals("Seguimiento completo del lote EXP-2026-002", fields.path("summary").asText());
        assertEquals("EXP-2026-002", fields.path("customfield_10263").asText());
        assertEquals("Producción, QA, cadena de frío y despacho", fields.path("customfield_10264").asText());
        assertEquals("Detalle adicional de prueba piloto", fields.path("customfield_10265").asText());

        // PILOT-04
        assertEquals(dto.getCorrelationId(), fields.path("customfield_10221").asText());
        assertEquals(dto.getCorrelationId(), fields.path("customfield_10266").asText());

        mockJiraServer.verify();
    }

    @Test
    @DisplayName("PILOT-05 & PILOT-06: Creación exitosa en Jira produce SYNCED y persiste jiraIssueKey y jiraRequestId")
    void pilot05_06_successfulJiraSyncProducesSyncedAndPersistsKeys() {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10116/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildPreflightFieldsResponseJson(false), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"issueKey\":\"EXT-105\",\"issueId\":\"20105\"}", MediaType.APPLICATION_JSON));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Consulta de lote para auditoría",
                "EXP-2026-001",
                "Historial completo",
                "Verificación"
        );

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userProd.getEmail(), null);

        assertEquals("SYNCED", created.getSyncStatus());
        assertEquals("EXT-105", created.getJiraIssueKey());
        assertEquals("20105", created.getJiraRequestId());
        assertEquals("10116", created.getJiraRequestTypeId());
        assertEquals("4 horas hábiles", created.getSlaTarget());
        assertEquals("JIRA_AUTHORITATIVE", created.getSlaAuthority());

        ServiceRequest persisted = serviceRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(JiraSyncStatus.SYNCED, persisted.getSyncStatus());
        assertEquals("EXT-105", persisted.getJiraIssueKey());
        assertEquals("20105", persisted.getJiraRequestId());
        assertEquals("10116", persisted.getJiraRequestTypeId());
        assertNotNull(persisted.getJiraLastSyncAt());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("PILOT-07: Retry con mismo correlationId ejecuta Lookup-Before-Create y recupera ticket existente sin duplicar")
    void pilot07_retryWithExistingCorrelationIdRecoversTicketWithoutDuplicating() {
        // 1. Primer intento falla por timeout de red
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Consulta con reintento idempotente",
                "EXP-2026-001",
                "Estado actual del lote",
                "Prueba de idempotencia"
        );

        ServiceRequestDTO firstAttempt = serviceRequestService.createServiceRequest(req, userProd.getEmail(), null);
        assertEquals("RETRY_PENDING", firstAttempt.getSyncStatus());
        assertNull(firstAttempt.getJiraIssueKey());
        mockJiraServer.verify();

        // 2. En el retry, Lookup-Before-Create encuentra que el ticket EXT-107 ya existe para ese correlationId
        mockJiraServer.reset();
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"issues\":[{\"key\":\"EXT-107\",\"id\":\"20107\",\"fields\":{\"summary\":\"Consulta\"}}]}",
                        MediaType.APPLICATION_JSON
                ));
        // Nota: NO se registra expectativa para POST /rest/servicedeskapi/request; si intentara crear otro ticket fallaría el test.

        ServiceRequestDTO retried = serviceRequestSyncService.retrySyncForAdmin(
                firstAttempt.getId(),
                userAdmin.getEmail(),
                null
        );

        assertEquals("SYNCED", retried.getSyncStatus());
        assertEquals("EXT-107", retried.getJiraIssueKey());
        assertEquals("20107", retried.getJiraRequestId());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("PILOT-08: Jira temporalmente caído (503 Service Unavailable) produce RETRY_PENDING y conserva solicitud local")
    void pilot08_jiraTemporarilyDownProducesRetryPendingAndPreservesLocalRecord() {
        mockJiraServer.expect(requestTo(containsString("/rest/api/3/search/jql")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"issues\":[]}", MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/servicedesk/2/requesttype/10116/field"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(buildPreflightFieldsResponseJson(false), MediaType.APPLICATION_JSON));

        mockJiraServer.expect(requestTo(TEST_JIRA_BASE_URL + "/rest/servicedeskapi/request"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessages\":[\"Service temporarily unavailable\"]}"));

        CreateServiceRequestDTO req = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Consulta durante caída temporal de Jira",
                "EXP-2026-001",
                "Trazabilidad completa",
                "Detalle"
        );

        ServiceRequestDTO created = serviceRequestService.createServiceRequest(req, userProd.getEmail(), null);

        assertNotNull(created.getId());
        assertEquals("RETRY_PENDING", created.getSyncStatus());
        assertEquals("LOCAL_ESTIMATE", created.getSlaAuthority());
        assertTrue(serviceRequestRepository.findById(created.getId()).isPresent());
        mockJiraServer.verify();
    }

    @Test
    @DisplayName("PILOT-09: Usuario solo puede ver sus propias solicitudes (403 Forbidden ante acceso de otro usuario operacional)")
    void pilot09_userCanOnlyViewOwnServiceRequests() throws Exception {
        configurePilotJiraProperties(false);

        CreateServiceRequestDTO reqProd = new CreateServiceRequestDTO(
                "SR-INF-01",
                "Solicitud privada de Producción",
                "EXP-2026-001",
                "Estado actual",
                "Detalle"
        );
        ServiceRequestDTO createdByProd = serviceRequestService.createServiceRequest(reqProd, userProd.getEmail(), null);

        // 1. Usuario QA consulta /api/service-requests/my -> no ve la solicitud de Producción
        MvcResult myQaResult = mockMvc.perform(get("/api/service-requests/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode qaList = objectMapper.readTree(myQaResult.getResponse().getContentAsString());
        assertEquals(0, qaList.size());

        // 2. Usuario QA intenta acceder por ID a la solicitud de Producción -> 403 Forbidden
        mockMvc.perform(get("/api/service-requests/" + createdByProd.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenQa))
                .andExpect(status().isForbidden());

        // 3. Usuario Producción sí puede ver su propia solicitud -> 200 OK
        mockMvc.perform(get("/api/service-requests/" + createdByProd.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProd))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(createdByProd.getCode()));
    }

    @Test
    @DisplayName("PILOT-10: Integración de IncidentReport continúa funcionando sin regresión")
    void pilot10_incidentReportIntegrationContinuesWorkingWithoutRegression() {
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
                .andRespond(withSuccess("{\"issueKey\":\"EXT-110\",\"issueId\":\"20110\"}", MediaType.APPLICATION_JSON));

        CreateIncidentRequest incReq = new CreateIncidentRequest(
                IncidentCategory.QUALITY,
                "Fallo al guardar inspección QA",
                "Al presionar guardar aparece un error inesperado en el formulario.",
                IncidentImpact.HIGH,
                WorkContinuity.NO_PUEDE_CONTINUAR,
                "QUALITY",
                "/quality",
                null
        );

        IncidentDTO createdIncident = incidentService.createIncident(incReq, userQa.getEmail(), null);
        assertEquals("SYNCED", createdIncident.getSyncStatus());
        assertEquals("EXT-110", createdIncident.getJiraIssueKey());
        mockJiraServer.verify();
    }
}
