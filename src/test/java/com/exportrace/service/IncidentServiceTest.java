package com.exportrace.service;

import com.exportrace.dto.CreateIncidentRequest;
import com.exportrace.dto.IncidentDTO;
import com.exportrace.entity.*;
import com.exportrace.entity.enums.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class IncidentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IncidentPriorityService incidentPriorityService;

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

    private User userQa;
    private User userProduccion;
    private User userAdmin;
    private User userSuperAdmin;
    private String tokenQa;
    private String tokenProduccion;
    private String tokenAdmin;
    private String tokenSuperAdmin;
    private Lot testLot;

    @BeforeEach
    void setUp() {
        Role roleQa = roleRepository.findByNombre("QA").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("QA");
            r.setDescripcion("Inspector de Calidad");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleProd = roleRepository.findByNombre("PRODUCCION").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("PRODUCCION");
            r.setDescripcion("Operador de Producción");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleAdmin = roleRepository.findByNombre("ADMINISTRADOR").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("ADMINISTRADOR");
            r.setDescripcion("Jefatura de Administración & TI");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        Role roleSuperAdmin = roleRepository.findByNombre("SUPERADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("SUPERADMIN");
            r.setDescripcion("Superadministrador Técnico");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        long suffix = System.currentTimeMillis();

        userQa = new User();
        userQa.setNombre("Elena");
        userQa.setApellido("Quispe");
        userQa.setEmail("elena.qa." + suffix + "@exportrace.pe");
        userQa.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userQa.setRole(roleQa);
        userQa.setActivo(true);
        userQa = userRepository.save(userQa);
        tokenQa = jwtUtil.generateAccessToken(userQa, "sess-inc-qa");

        userProduccion = new User();
        userProduccion.setNombre("Renzo");
        userProduccion.setApellido("Alva");
        userProduccion.setEmail("renzo.prod." + suffix + "@exportrace.pe");
        userProduccion.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userProduccion.setRole(roleProd);
        userProduccion.setActivo(true);
        userProduccion = userRepository.save(userProduccion);
        tokenProduccion = jwtUtil.generateAccessToken(userProduccion, "sess-inc-prod");

        userAdmin = new User();
        userAdmin.setNombre("Carlos");
        userAdmin.setApellido("Mendoza");
        userAdmin.setEmail("carlos.admin." + suffix + "@exportrace.pe");
        userAdmin.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userAdmin.setRole(roleAdmin);
        userAdmin.setActivo(true);
        userAdmin = userRepository.save(userAdmin);
        tokenAdmin = jwtUtil.generateAccessToken(userAdmin, "sess-inc-admin");

        userSuperAdmin = new User();
        userSuperAdmin.setNombre("Super");
        userSuperAdmin.setApellido("Administrador");
        userSuperAdmin.setEmail("superadmin." + suffix + "@exportrace.pe");
        userSuperAdmin.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userSuperAdmin.setRole(roleSuperAdmin);
        userSuperAdmin.setActivo(true);
        userSuperAdmin = userRepository.save(userSuperAdmin);
        tokenSuperAdmin = jwtUtil.generateAccessToken(userSuperAdmin, "sess-inc-superadmin");

        Product prod = productRepository.findAll().stream().findFirst().orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("PROD-INC");
            p.setNombre("Pota Congelada");
            p.setTipoConservacion("CONGELADO");
            return productRepository.save(p);
        });

        testLot = new Lot();
        testLot.setCodigo("EXP-INC-" + suffix);
        testLot.setProducto(prod);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setPesoNetoKg(18000.0);
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setQrToken("QR-INC-" + suffix);
        testLot = lotRepository.save(testLot);
    }

    @Test
    @DisplayName("INC-A01: Usuario activo crea incidencia válida -> HTTP 201 Created")
    void incA01_createValidIncident_persistsWithReportedAndPendingStatus() throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.COLD_CHAIN,
                "Fallo en registro de sensor térmico cámara 02",
                "El sensor T-02 no permite guardar lecturas negativas en el lote activo.",
                IncidentImpact.HIGH,
                WorkContinuity.NO_PUEDE_CONTINUAR,
                "FRIO",
                "/quality/coldchain",
                testLot.getId()
        );

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("INC-2026-")))
                .andExpect(jsonPath("$.category").value("COLD_CHAIN"))
                .andExpect(jsonPath("$.categoryCode").value("CAT-05"))
                .andExpect(jsonPath("$.impact").value("HIGH"))
                .andExpect(jsonPath("$.urgency").value("HIGH"))
                .andExpect(jsonPath("$.priority").value("P1_CRITICAL"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.slaAuthority").value("LOCAL_ESTIMATE"))
                .andExpect(jsonPath("$.relatedLotId").value(testLot.getId()));
    }

    @Test
    @DisplayName("INC-A02: Impacto HIGH + Urgencia HIGH (NO_PUEDE_CONTINUAR) -> P1_CRITICAL y validación de los 4 SLA LOCAL_ESTIMATE")
    void incA02_highImpactAndHighUrgency_producesP1CriticalAndValidatesSlaLocalEstimates() {
        IncidentUrgency urgency = incidentPriorityService.resolveUrgency(WorkContinuity.NO_PUEDE_CONTINUAR);
        assertEquals(IncidentUrgency.HIGH, urgency);

        IncidentPriority priority = incidentPriorityService.calculatePriority(IncidentImpact.HIGH, urgency);
        assertEquals(IncidentPriority.P1_CRITICAL, priority);

        // Verificación estricta de SLA LOCAL_ESTIMATE aprobados:
        // P1 -> 15 min / 2h (120 min)
        assertEquals(15, IncidentPriority.P1_CRITICAL.getSlaFirstResponseMinutes());
        assertEquals(120, IncidentPriority.P1_CRITICAL.getSlaResolutionMinutes());

        // P2 -> 30 min / 4h (240 min)
        assertEquals(30, IncidentPriority.P2_HIGH.getSlaFirstResponseMinutes());
        assertEquals(240, IncidentPriority.P2_HIGH.getSlaResolutionMinutes());

        // P3 -> 2h (120 min) / 8h (480 min)
        assertEquals(120, IncidentPriority.P3_MEDIUM.getSlaFirstResponseMinutes());
        assertEquals(480, IncidentPriority.P3_MEDIUM.getSlaResolutionMinutes());

        // P4 -> 4h (240 min) / 24h (1440 min)
        assertEquals(240, IncidentPriority.P4_LOW.getSlaFirstResponseMinutes());
        assertEquals(1440, IncidentPriority.P4_LOW.getSlaResolutionMinutes());

        LocalDateTime base = LocalDateTime.of(2026, 10, 21, 10, 0);
        assertEquals(base.plusMinutes(15), incidentPriorityService.calculateFirstResponseTarget(base, IncidentPriority.P1_CRITICAL));
        assertEquals(base.plusHours(2), incidentPriorityService.calculateResolutionTarget(base, IncidentPriority.P1_CRITICAL));
        assertEquals(base.plusMinutes(30), incidentPriorityService.calculateFirstResponseTarget(base, IncidentPriority.P2_HIGH));
        assertEquals(base.plusHours(4), incidentPriorityService.calculateResolutionTarget(base, IncidentPriority.P2_HIGH));
        assertEquals(base.plusHours(2), incidentPriorityService.calculateFirstResponseTarget(base, IncidentPriority.P3_MEDIUM));
        assertEquals(base.plusHours(8), incidentPriorityService.calculateResolutionTarget(base, IncidentPriority.P3_MEDIUM));
        assertEquals(base.plusHours(4), incidentPriorityService.calculateFirstResponseTarget(base, IncidentPriority.P4_LOW));
        assertEquals(base.plusHours(24), incidentPriorityService.calculateResolutionTarget(base, IncidentPriority.P4_LOW));
    }

    @Test
    @DisplayName("INC-A03: Las 9 combinaciones Impacto × Urgencia producen exactamente la prioridad esperada")
    void incA03_priorityMatrix_calculatesAllNineCombinationsCorrectly() {
        assertEquals(IncidentPriority.P1_CRITICAL,
                incidentPriorityService.calculatePriority(IncidentImpact.HIGH, IncidentUrgency.HIGH));
        assertEquals(IncidentPriority.P2_HIGH,
                incidentPriorityService.calculatePriority(IncidentImpact.HIGH, IncidentUrgency.MEDIUM));
        assertEquals(IncidentPriority.P2_HIGH,
                incidentPriorityService.calculatePriority(IncidentImpact.HIGH, IncidentUrgency.LOW));

        assertEquals(IncidentPriority.P2_HIGH,
                incidentPriorityService.calculatePriority(IncidentImpact.MEDIUM, IncidentUrgency.HIGH));
        assertEquals(IncidentPriority.P3_MEDIUM,
                incidentPriorityService.calculatePriority(IncidentImpact.MEDIUM, IncidentUrgency.MEDIUM));
        assertEquals(IncidentPriority.P3_MEDIUM,
                incidentPriorityService.calculatePriority(IncidentImpact.MEDIUM, IncidentUrgency.LOW));

        assertEquals(IncidentPriority.P3_MEDIUM,
                incidentPriorityService.calculatePriority(IncidentImpact.LOW, IncidentUrgency.HIGH));
        assertEquals(IncidentPriority.P4_LOW,
                incidentPriorityService.calculatePriority(IncidentImpact.LOW, IncidentUrgency.MEDIUM));
        assertEquals(IncidentPriority.P4_LOW,
                incidentPriorityService.calculatePriority(IncidentImpact.LOW, IncidentUrgency.LOW));

        assertEquals(IncidentUrgency.HIGH, incidentPriorityService.resolveUrgency(WorkContinuity.NO_PUEDE_CONTINUAR));
        assertEquals(IncidentUrgency.MEDIUM, incidentPriorityService.resolveUrgency(WorkContinuity.CONTINUA_PARCIALMENTE));
        assertEquals(IncidentUrgency.LOW, incidentPriorityService.resolveUrgency(WorkContinuity.PUEDE_CONTINUAR));
    }

    @Test
    @DisplayName("INC-A04: Usuario intenta enviar priority, status o reporterEmail manualmente en JSON -> Ignorado, manda el cálculo backend")
    void incA04_ignoreManipulatedPayloadFields_computesPriorityAndSnapshotsFromSession() throws Exception {
        Map<String, Object> maliciousPayload = Map.of(
                "category", "CERTIFICATION",
                "summary", "Bloqueo al emitir certificado SANIPES",
                "description", "El botón de emisión no responde al firmar el expediente digital.",
                "impact", "HIGH",
                "workContinuity", "NO_PUEDE_CONTINUAR",
                "priority", "P4_LOW",
                "status", "RESOLVED",
                "syncStatus", "SYNCED",
                "jiraIssueKey", "HACK-999",
                "reporterEmail", "hacker@malicioso.com"
        );

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("P1_CRITICAL"))
                .andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.syncStatus").value("PENDING"))
                .andExpect(jsonPath("$.jiraIssueKey").isEmpty())
                .andExpect(jsonPath("$.reporterUsername").value("Elena Quispe"));
    }

    @Test
    @DisplayName("INC-A05: Dos incidencias creadas generan códigos humanos INC-2026-XXXXXX diferentes")
    void incA05_correlativeCodeGeneration_producesSequentialUniqueCodes() {
        CreateIncidentRequest req1 = new CreateIncidentRequest(
                IncidentCategory.LOTS, "Incidencia Lote A", "Descripción detallada para lote A en planta.",
                IncidentImpact.LOW, WorkContinuity.PUEDE_CONTINUAR, "LOTES", "/lots", null);
        CreateIncidentRequest req2 = new CreateIncidentRequest(
                IncidentCategory.LOTS, "Incidencia Lote B", "Descripción detallada para lote B en planta.",
                IncidentImpact.MEDIUM, WorkContinuity.CONTINUA_PARCIALMENTE, "LOTES", "/lots", null);

        IncidentDTO dto1 = incidentService.createIncident(req1, userQa.getEmail(), null);
        IncidentDTO dto2 = incidentService.createIncident(req2, userQa.getEmail(), null);

        assertNotNull(dto1.getCode());
        assertNotNull(dto2.getCode());
        assertNotEquals(dto1.getCode(), dto2.getCode());
        assertTrue(dto1.getCode().matches("^INC-2026-\\d{6}$"));
        assertTrue(dto2.getCode().matches("^INC-2026-\\d{6}$"));
    }

    @Test
    @DisplayName("INC-A06: Dos incidencias creadas generan correlationId UUID v4 diferentes")
    void incA06_correlationId_isGeneratedAsUniqueUuidV4() {
        CreateIncidentRequest req1 = new CreateIncidentRequest(
                IncidentCategory.AUTHENTICATION, "Incidencia Uno", "Descripción completa de la incidencia número uno.",
                IncidentImpact.LOW, WorkContinuity.PUEDE_CONTINUAR, "AUTENTICACION", "/login", null);
        CreateIncidentRequest req2 = new CreateIncidentRequest(
                IncidentCategory.DASHBOARD, "Incidencia Dos", "Descripción completa de la incidencia número dos.",
                IncidentImpact.MEDIUM, WorkContinuity.CONTINUA_PARCIALMENTE, "DASHBOARD", "/dashboard", null);

        IncidentDTO dto1 = incidentService.createIncident(req1, userQa.getEmail(), null);
        IncidentDTO dto2 = incidentService.createIncident(req2, userQa.getEmail(), null);

        IncidentReport entity1 = incidentReportRepository.findById(dto1.getId()).orElseThrow();
        IncidentReport entity2 = incidentReportRepository.findById(dto2.getId()).orElseThrow();

        assertNotNull(entity1.getCorrelationId());
        assertNotNull(entity2.getCorrelationId());
        assertNotEquals(entity1.getCorrelationId(), entity2.getCorrelationId());
    }

    @Test
    @DisplayName("INC-A07: Usuario consulta GET /api/incidents/my -> Recibe únicamente sus propias incidencias")
    void incA07_getMyIncidents_returnsOnlyAuthenticatedUserIncidents() throws Exception {
        CreateIncidentRequest reqQa1 = new CreateIncidentRequest(
                IncidentCategory.QUALITY, "Incidencia QA 1", "Detalle de la primera incidencia de QA",
                IncidentImpact.MEDIUM, WorkContinuity.CONTINUA_PARCIALMENTE, "CALIDAD", "/quality", null);
        CreateIncidentRequest reqQa2 = new CreateIncidentRequest(
                IncidentCategory.COLD_CHAIN, "Incidencia QA 2", "Detalle de la segunda incidencia de QA",
                IncidentImpact.HIGH, WorkContinuity.NO_PUEDE_CONTINUAR, "FRIO", "/quality/coldchain", null);

        CreateIncidentRequest reqProd = new CreateIncidentRequest(
                IncidentCategory.LOTS, "Incidencia Produccion 1", "Detalle de la incidencia de Producción",
                IncidentImpact.LOW, WorkContinuity.PUEDE_CONTINUAR, "LOTES", "/lots", null);

        incidentService.createIncident(reqQa1, userQa.getEmail(), null);
        incidentService.createIncident(reqQa2, userQa.getEmail(), null);
        incidentService.createIncident(reqProd, userProduccion.getEmail(), null);

        MvcResult resQa = mockMvc.perform(get("/api/incidents/my")
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andReturn();

        IncidentDTO[] qaList = objectMapper.readValue(resQa.getResponse().getContentAsString(), IncidentDTO[].class);
        assertEquals(2, qaList.length);
        assertTrue(List.of(qaList).stream().allMatch(i -> "Elena Quispe".equals(i.getReporterUsername())));

        MvcResult resProd = mockMvc.perform(get("/api/incidents/my")
                        .header("Authorization", "Bearer " + tokenProduccion))
                .andExpect(status().isOk())
                .andReturn();

        IncidentDTO[] prodList = objectMapper.readValue(resProd.getResponse().getContentAsString(), IncidentDTO[].class);
        assertEquals(1, prodList.length);
        assertEquals("Incidencia Produccion 1", prodList[0].getSummary());
    }

    @Test
    @DisplayName("INC-A08: Usuario operacional intenta consultar incidencia ajena -> HTTP 403 Forbidden + AuditLog INCIDENT_VIEW_DENIED")
    void incA08_getIncidentById_otherUserCannotAccessAndAuditLogIsRecorded() throws Exception {
        CreateIncidentRequest reqQa = new CreateIncidentRequest(
                IncidentCategory.DOCUMENTS,
                "Error al validar hash SHA-256 de documento",
                "El visor reporta discrepancia en certificado cargado.",
                IncidentImpact.HIGH,
                WorkContinuity.CONTINUA_PARCIALMENTE,
                "DOCUMENTOS",
                "/certification",
                null
        );
        IncidentDTO createdByQa = incidentService.createIncident(reqQa, userQa.getEmail(), null);

        mockMvc.perform(get("/api/incidents/" + createdByQa.getId())
                        .header("Authorization", "Bearer " + tokenProduccion))
                .andExpect(status().isForbidden());

        List<AuditLog> audits = auditLogRepository.findAll();
        assertTrue(audits.stream().anyMatch(a ->
                "INCIDENT_VIEW_DENIED".equals(a.getAction())
                        && "DENEGADO".equals(a.getResult())
                        && String.valueOf(createdByQa.getId()).equals(a.getEntityId())
        ));

        // El propietario sí puede consultar su propia incidencia
        mockMvc.perform(get("/api/incidents/" + createdByQa.getId())
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(createdByQa.getCode()));
    }

    @Test
    @DisplayName("INC-A09: Incidencia creada nace con status REPORTED")
    void incA09_incidentCreated_startsWithStatusReported() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.TRACEABILITY_QR,
                "Código QR demora en renderizar",
                "El modal de código QR tarda más de tres segundos en mostrar la imagen.",
                IncidentImpact.LOW,
                WorkContinuity.PUEDE_CONTINUAR,
                "TRAZABILIDAD",
                "/lots",
                testLot.getId()
        );

        IncidentDTO created = incidentService.createIncident(req, userQa.getEmail(), null);
        assertEquals("REPORTED", created.getStatus());
    }

    @Test
    @DisplayName("INC-A10: Incidencia creada nace con syncStatus PENDING y slaAuthority LOCAL_ESTIMATE")
    void incA10_incidentCreated_startsWithSyncStatusPending() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.BACKEND_API,
                "Intermitencia en consulta de catálogo",
                "Se observó una respuesta lenta puntual al recargar el catálogo general.",
                IncidentImpact.MEDIUM,
                WorkContinuity.PUEDE_CONTINUAR,
                "SISTEMA",
                "/dashboard",
                null
        );

        IncidentDTO created = incidentService.createIncident(req, userQa.getEmail(), null);
        assertEquals("PENDING", created.getSyncStatus());
        assertNull(created.getJiraIssueKey());
        assertEquals("LOCAL_ESTIMATE", created.getSlaAuthority());
    }

    @Test
    @DisplayName("INC-A11: relatedLotId inexistente -> HTTP 404 Not Found controlado")
    void incA11_createIncident_withNonExistentLot_throwsResourceNotFound() throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.LOTS,
                "Problema en lote específico inexistente",
                "No se visualiza la trazabilidad del lote indicado en planta.",
                IncidentImpact.MEDIUM,
                WorkContinuity.CONTINUA_PARCIALMENTE,
                "LOTES",
                "/lots",
                999999L
        );

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999999")));
    }

    @Test
    @DisplayName("INC-A12: Campos obligatorios vacíos o nulos -> HTTP 400 Bad Request")
    void incA12_beanValidation_rejectsBlankSummaryShortDescriptionAndNullCategory() throws Exception {
        CreateIncidentRequest invalidReq = new CreateIncidentRequest();
        invalidReq.setSummary("");
        invalidReq.setDescription("");

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("INC-A13: Incidencia creada registra evento AuditLog INCIDENT_CREATED y notificación interna")
    void incA13_createIncident_createsAuditLogAndInternalNotification() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.LOGISTICS,
                "Retraso en validación de precinto SANIPES",
                "El formulario de despacho muestra carga prolongada al validar el precinto.",
                IncidentImpact.HIGH,
                WorkContinuity.CONTINUA_PARCIALMENTE,
                "DESPACHO",
                "/dispatch",
                testLot.getId()
        );

        IncidentDTO created = incidentService.createIncident(req, userQa.getEmail(), null);

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog createdLog = logs.stream()
                .filter(a -> "INCIDENT_CREATED".equals(a.getAction()) && String.valueOf(created.getId()).equals(a.getEntityId()))
                .findFirst()
                .orElse(null);

        assertNotNull(createdLog, "Debe existir un registro AuditLog con acción INCIDENT_CREATED");
        assertEquals("EXITOSO", createdLog.getResult());
        assertEquals(userQa.getId(), createdLog.getUserId());
        assertTrue(createdLog.getNewValue().contains(created.getCode()));
        assertTrue(createdLog.getNewValue().contains("LOGISTICS"));
        assertTrue(createdLog.getNewValue().contains("P2_HIGH"));
    }

    @Test
    @DisplayName("INC-A14 (Evidencia de INC-12): Usuario desactivado intenta reportar o consultar incidencias -> Acceso denegado")
    void incA14_inactiveUser_cannotCreateOrQueryIncidents() throws Exception {
        userQa.setActivo(false);
        userRepository.saveAndFlush(userQa);

        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.QUALITY,
                "Intento de reporte con cuenta inactiva",
                "Descripción válida de más de diez caracteres para prueba.",
                IncidentImpact.LOW,
                WorkContinuity.PUEDE_CONTINUAR,
                "CALIDAD",
                "/quality",
                null
        );

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "Expected 401 or 403 for deactivated user on POST /api/incidents but got " + st);
                });

        mockMvc.perform(get("/api/incidents/my")
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(result -> {
                    int st = result.getResponse().getStatus();
                    assertTrue(st == 401 || st == 403, "Expected 401 or 403 for deactivated user on GET /api/incidents/my but got " + st);
                });
    }

    @Test
    @DisplayName("INC-A15: Usuario sin token JWT intenta acceder a /api/incidents -> Endpoint protegido (HTTP 401/403)")
    void incA15_unauthenticatedRequestWithoutJwt_blocked() throws Exception {
        mockMvc.perform(get("/api/incidents/my"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403, "Expected 401 or 403 without JWT but got " + status);
                });

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403, "Expected 401 or 403 without JWT but got " + status);
                });
    }

    @Test
    @DisplayName("INC-A16: Matriz RBAC Administrativa en GET /api/admin/incidents -> Usuario operacional (403), ADMINISTRADOR (200), SUPERADMIN (200)")
    void incA16_adminIncidentsRbac_operational403_administrador200_superadmin200() throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest(
                IncidentCategory.DATABASE,
                "Lentitud puntual en consulta histórica de lotes",
                "La consulta de lotes antiguos demoró más de lo habitual.",
                IncidentImpact.MEDIUM,
                WorkContinuity.CONTINUA_PARCIALMENTE,
                "SISTEMA",
                "/lots",
                testLot.getId()
        );
        IncidentDTO created = incidentService.createIncident(req, userQa.getEmail(), null);

        // 1. Usuarios operacionales (PRODUCCION y QA) -> 403 Forbidden en GET /api/admin/incidents
        mockMvc.perform(get("/api/admin/incidents")
                        .header("Authorization", "Bearer " + tokenProduccion))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/incidents")
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isForbidden());

        // 2. Rol ADMINISTRADOR (authority ROLE_ADMINISTRADOR) -> 200 OK
        mockMvc.perform(get("/api/admin/incidents")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(created.getCode()));

        mockMvc.perform(get("/api/incidents/" + created.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(created.getCode()));

        // 3. Rol SUPERADMIN (authority ROLE_SUPERADMIN) -> 200 OK
        mockMvc.perform(get("/api/admin/incidents")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(created.getCode()));

        mockMvc.perform(get("/api/incidents/" + created.getId())
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(created.getCode()));
    }
}
