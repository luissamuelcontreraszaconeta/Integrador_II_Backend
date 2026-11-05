package com.exportrace.service;

import com.exportrace.dto.CreateLotRequest;
import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.dto.UpdateLotRequest;
import com.exportrace.entity.*;
import com.exportrace.repository.*;
import com.exportrace.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CrossRoleLotVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String tokenProduccion;
    private String tokenQa;
    private String tokenLogistica;
    private String tokenGerencia;
    private String tokenSinPermisoLotsView;

    @BeforeEach
    void setUp() {
        User prodUser = userRepository.findByEmail("produccion@exportrace.pe").orElseThrow();
        User qaUser = userRepository.findByEmail("qa@exportrace.pe").orElseThrow();
        User logUser = userRepository.findByEmail("logistica@exportrace.pe").orElseThrow();
        User gerUser = userRepository.findByEmail("gerencia@exportrace.pe").orElseThrow();

        tokenProduccion = jwtUtil.generateAccessToken(prodUser, "sess-cross-prod");
        tokenQa = jwtUtil.generateAccessToken(qaUser, "sess-cross-qa");
        tokenLogistica = jwtUtil.generateAccessToken(logUser, "sess-cross-log");
        tokenGerencia = jwtUtil.generateAccessToken(gerUser, "sess-cross-ger");

        // Create a restricted role without LOTS_VIEW for CROSS-06
        Role restrictedRole = roleRepository.findByNombre("INVITADO_SIN_LOTES").orElseGet(() -> {
            Role r = new Role("INVITADO_SIN_LOTES", "Rol restringido sin permiso LOTS_VIEW");
            r.setPermissions(new HashSet<>());
            return roleRepository.save(r);
        });
        restrictedRole.setPermissions(new HashSet<>());
        roleRepository.save(restrictedRole);

        User restrictedUser = new User();
        restrictedUser.setNombre("Invitado");
        restrictedUser.setApellido("Restringido");
        restrictedUser.setEmail("restringido." + UUID.randomUUID().toString().substring(0, 6) + "@exportrace.pe");
        restrictedUser.setPasswordHash(passwordEncoder.encode("Pass123!"));
        restrictedUser.setArea("Externo");
        restrictedUser.setRole(restrictedRole);
        restrictedUser.setActivo(true);
        restrictedUser = userRepository.save(restrictedUser);

        tokenSinPermisoLotsView = jwtUtil.generateAccessToken(restrictedUser, "sess-cross-restricted");
    }

    private CreateLotRequest buildNewLotRequest(String customCode) {
        CreateLotRequest req = new CreateLotRequest();
        req.setCodigo(customCode);
        req.setProducto("POTA_CONGELADA_BLOCK");
        req.setEspecie("Dosidicus gigas");
        req.setCantidadEmpaques(24);
        req.setTipoEmpaque("TN");
        req.setPesoNetoKg(24000.0);
        req.setPlantaProcesamiento("Planta Paita #01");
        req.setLineaProcesamiento("Línea 02 - Bloques Exportación");
        req.setFechaProduccion(LocalDate.now().toString());
        req.setProveedor("Asociación Pesquera Artesanal Paita Norte");
        req.setEmbarcacion("E/P Don Luis II (CO-18492-PM)");
        req.setInspeccionadoPor("Renzo Alva");
        req.setObservaciones("Lote operacional creado por Producción para prueba transversal");
        return req;
    }

    @Test
    @DisplayName("CROSS-01: PRODUCCIÓN crea lote nuevo -> QA consulta y visualiza exactamente ese lote")
    void cross01_produccionCreatesLot_qaVisualizesExactSameLot() throws Exception {
        String code = "EXP-CROSS01-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        CreateLotRequest createReq = buildNewLotRequest(code);

        // 1. PRODUCCION creates lot via POST /api/lots
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andReturn();

        JsonNode createdLot = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String lotId = createdLot.get("id").asText();

        // 2. QA queries GET /api/lots and sees the newly created lot in the list
        MvcResult listRes = mockMvc.perform(get("/api/lots")
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode lotsArray = objectMapper.readTree(listRes.getResponse().getContentAsString());
        boolean foundInQaList = false;
        for (JsonNode node : lotsArray) {
            if (code.equals(node.get("code").asText()) && lotId.equals(node.get("id").asText())) {
                foundInQaList = true;
                assertEquals("REGISTERED", node.get("status").asText());
                break;
            }
        }
        assertTrue(foundInQaList, "QA debe visualizar en GET /api/lots el lote recién creado por PRODUCCIÓN");

        // 3. QA queries GET /api/lots/{id} directly
        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lotId))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    @Test
    @DisplayName("CROSS-02: QA no puede crear lotes ni modificar campos exclusivos de PRODUCCIÓN (HTTP 403)")
    void cross02_qaCannotModifyExclusiveProductionFields_returns403() throws Exception {
        String code = "EXP-CROSS02-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        CreateLotRequest createReq = buildNewLotRequest(code);

        // PRODUCCION creates the lot
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createdLot = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String lotId = createdLot.get("id").asText();
        long version = createdLot.get("version").asLong();

        // QA attempts POST /api/lots -> 403 Forbidden
        mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest("EXP-QA-FORBIDDEN"))))
                .andExpect(status().isForbidden());

        // QA attempts PUT /api/lots/{id} to edit production fields -> 403 Forbidden
        UpdateLotRequest updateReq = new UpdateLotRequest();
        updateReq.setVersion(version);
        updateReq.setPesoNetoKg(99000.0);
        updateReq.setPlantaProcesamiento("Planta Alterada por QA");

        mockMvc.perform(put("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CROSS-03: QA registra inspección del lote nuevo -> PRODUCCIÓN consulta nuevamente y visualiza estado actualizado")
    void cross03_qaRegistersInspection_produccionSeesUpdatedStatus() throws Exception {
        String code = "EXP-CROSS03-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();

        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // QA registers CONFORME inspection
        QAInspectionDTO qaDto = new QAInspectionDTO();
        qaDto.setInspectorName("Dra. María Elena Quispe");
        qaDto.setAppearance("EXCELENTE");
        qaDto.setColor("CONFORME");
        qaDto.setTexture("FIRM");
        qaDto.setSmell("CARACTERISTICO");
        qaDto.setParasiteCheck("AUSENCIA");
        qaDto.setOrganolepticResult("CONFORME");
        qaDto.setObservations("Lote nuevo evaluado CONFORME por QA");

        mockMvc.perform(post("/api/quality/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qaDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organolepticResult").value("CONFORME"));

        // PRODUCCION queries GET /api/lots/{id} and sees READY_FOR_CERTIFICATION + QA data
        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenProduccion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lotId))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("READY_FOR_CERTIFICATION"))
                .andExpect(jsonPath("$.qa.organolepticResult").value("CONFORME"));
    }

    @Test
    @DisplayName("CROSS-04: Lote avanza a certificación -> Usuario autorizado de Certificación puede visualizarlo")
    void cross04_lotAdvancesToCertification_certificationUserSeesIt() throws Exception {
        String code = "EXP-CROSS04-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();

        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // QA approves inspection -> READY_FOR_CERTIFICATION
        QAInspectionDTO qaDto = new QAInspectionDTO();
        qaDto.setOrganolepticResult("CONFORME");
        qaDto.setInspectorName("Dra. María Elena Quispe");
        mockMvc.perform(post("/api/quality/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qaDto)))
                .andExpect(status().isOk());

        // Authorized Certification / Logistics user initiates SANIPES certification
        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/request")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SOLICITADO"));

        // Both LOGISTICA and QA (who has CERTIFICATION_VIEW / MODULE_CERTIFICACION) see the lot in IN_CERTIFICATION
        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_CERTIFICATION"))
                .andExpect(jsonPath("$.certificationId").value(lotId));

        mockMvc.perform(get("/api/certifications/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SOLICITADO"));
    }

    @Test
    @DisplayName("CROSS-05: Lote queda READY_FOR_DISPATCH -> LOGÍSTICA puede visualizarlo")
    void cross05_lotBecomesReadyForDispatch_logisticaSeesIt() throws Exception {
        String code = "EXP-CROSS05-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();

        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // QA inspection CONFORME
        QAInspectionDTO qaDto = new QAInspectionDTO();
        qaDto.setOrganolepticResult("CONFORME");
        mockMvc.perform(post("/api/quality/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qaDto)))
                .andExpect(status().isOk());

        // Certification request -> approve -> enable-dispatch
        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/request")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/approve")
                        .header("Authorization", "Bearer " + tokenLogistica)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("certNumber", "CS-2026-CROSS05"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/enable-dispatch")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());

        // LOGISTICA queries GET /api/lots/{id} and sees READY_FOR_DISPATCH
        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("READY_FOR_DISPATCH"));
    }

    @Test
    @DisplayName("CROSS-06: Usuario sin permiso LOTS_VIEW intenta consultar lotes -> recibe HTTP 403 Forbidden")
    void cross06_userWithoutLotsViewPermission_receives403() throws Exception {
        mockMvc.perform(get("/api/lots")
                        .header("Authorization", "Bearer " + tokenSinPermisoLotsView))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/lots/1")
                        .header("Authorization", "Bearer " + tokenSinPermisoLotsView))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CROSS-07: Cerrar sesión Producción e iniciar QA -> sesión anterior se invalida y QA recibe estado real desde BD")
    void cross07_logoutProduccionAndLoginQa_noStaleSessionAndSeesLiveData() throws Exception {
        // 1. Login as PRODUCCION via /api/auth/login
        MvcResult prodLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "produccion@exportrace.pe",
                                "password", "Prod123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode prodAuth = objectMapper.readTree(prodLogin.getResponse().getContentAsString());
        String prodJwt = prodAuth.get("token").asText();
        String prodRefresh = prodAuth.get("refreshToken").asText();

        // 2. PRODUCCION creates a lot
        String code = "EXP-CROSS07-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + prodJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();
        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // 3. PRODUCCION logs out via /api/auth/logout -> refresh token of previous session is revoked
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + prodJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", prodRefresh))))
                .andExpect(status().isOk());

        // Attempting to refresh PRODUCCION's logged-out session must fail
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", prodRefresh))))
                .andExpect(status().isUnauthorized());

        // 4. QA logs in via /api/auth/login and queries GET /api/lots
        MvcResult qaLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "qa@exportrace.pe",
                                "password", "QA123"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String qaJwt = objectMapper.readTree(qaLogin.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + qaJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lotId))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("REGISTERED"));
    }

    @Test
    @DisplayName("CROSS-08: Lotes creados por DataInitializer y lotes creados por POST /api/lots siguen exactamente la misma política de visibilidad")
    void cross08_dataInitializerAndApiLotsFollowIdenticalVisibilityPolicy() throws Exception {
        String apiLotCode = "EXP-CROSS08-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(apiLotCode))))
                .andExpect(status().isOk())
                .andReturn();

        String apiLotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        for (String roleToken : new String[]{tokenProduccion, tokenQa, tokenLogistica, tokenGerencia}) {
            MvcResult res = mockMvc.perform(get("/api/lots")
                            .header("Authorization", "Bearer " + roleToken))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode list = objectMapper.readTree(res.getResponse().getContentAsString());
            JsonNode seededLot = null;
            JsonNode createdLot = null;
            for (JsonNode n : list) {
                if ("EXP-2026-001".equals(n.get("code").asText())) {
                    seededLot = n;
                }
                if (apiLotCode.equals(n.get("code").asText()) && apiLotId.equals(n.get("id").asText())) {
                    createdLot = n;
                }
            }
            assertNotNull(seededLot, "El lote inicial EXP-2026-001 debe ser visible para el rol autorizado");
            assertNotNull(createdLot, "El lote creado por API debe ser igualmente visible para el mismo rol autorizado");
            assertTrue(createdLot.get("documents").size() >= 2, "El lote creado por API debe contar con su documentación inicial en BD");
            assertTrue(createdLot.get("coldChainLogs").size() >= 1, "El lote creado por API debe contar con su lectura térmica inicial en BD");
            assertEquals(6, createdLot.get("timeline").size(), "El lote creado por API debe exponer las 6 etapas del timeline");
        }
    }

    @Test
    @DisplayName("CROSS-09: Lote DISPATCHED sigue siendo consultable históricamente por todos los roles autorizados")
    void cross09_dispatchedLotRemainsHistoricallyQueryableByAuthorizedRoles() throws Exception {
        String code = "EXP-CROSS09-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();
        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // Advance lot through QA -> Certification -> Ready for Dispatch -> Dispatched
        QAInspectionDTO qaDto = new QAInspectionDTO();
        qaDto.setOrganolepticResult("CONFORME");
        mockMvc.perform(post("/api/quality/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qaDto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/request")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/approve")
                        .header("Authorization", "Bearer " + tokenLogistica)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("certNumber", "CS-2026-CROSS09"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/certifications/lot/" + lotId + "/enable-dispatch")
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk());

        Dispatch dispReq = new Dispatch();
        dispReq.setPuertoDestino("Puerto de Valencia, España");
        dispReq.setNumeroContenedor("MSCU-554433-2");
        dispReq.setPrecintoSeguridad("SANIPES-SEAL-9901");
        dispReq.setTransportista("MSC Mediterranean Shipping");

        mockMvc.perform(post("/api/dispatches/lot/" + lotId)
                        .header("Authorization", "Bearer " + tokenLogistica)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispReq)))
                .andExpect(status().isOk());

        // Verify PRODUCCION, QA, LOGISTICA, and GERENCIA can all query the historical DISPATCHED lot
        for (String roleToken : new String[]{tokenProduccion, tokenQa, tokenLogistica, tokenGerencia}) {
            mockMvc.perform(get("/api/lots/" + lotId)
                            .header("Authorization", "Bearer " + roleToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(lotId))
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.status").value("DISPATCHED"))
                    .andExpect(jsonPath("$.dispatchInfo.containerNumber").value("MSCU-554433-2"));
        }
    }

    @Test
    @DisplayName("CROSS-10: Dos usuarios autorizados consultan el mismo lote y reciben el mismo id, codigo, status y version")
    void cross10_twoAuthorizedUsersQuerySameLot_receiveIdenticalIdCodeStatusVersion() throws Exception {
        String code = "EXP-CROSS10-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult createRes = mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildNewLotRequest(code))))
                .andExpect(status().isOk())
                .andReturn();
        String lotId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        MvcResult prodQuery = mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenProduccion))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult qaQuery = mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenQa))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult logQuery = mockMvc.perform(get("/api/lots/" + lotId)
                        .header("Authorization", "Bearer " + tokenLogistica))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode prodNode = objectMapper.readTree(prodQuery.getResponse().getContentAsString());
        JsonNode qaNode = objectMapper.readTree(qaQuery.getResponse().getContentAsString());
        JsonNode logNode = objectMapper.readTree(logQuery.getResponse().getContentAsString());

        assertEquals(prodNode.get("id").asText(), qaNode.get("id").asText());
        assertEquals(prodNode.get("code").asText(), qaNode.get("code").asText());
        assertEquals(prodNode.get("status").asText(), qaNode.get("status").asText());
        assertEquals(prodNode.get("version").asLong(), qaNode.get("version").asLong());

        assertEquals(prodNode.get("id").asText(), logNode.get("id").asText());
        assertEquals(prodNode.get("code").asText(), logNode.get("code").asText());
        assertEquals(prodNode.get("status").asText(), logNode.get("status").asText());
        assertEquals(prodNode.get("version").asLong(), logNode.get("version").asLong());
    }
}
