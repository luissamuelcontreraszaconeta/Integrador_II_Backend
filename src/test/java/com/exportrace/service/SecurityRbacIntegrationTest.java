package com.exportrace.service;

import com.exportrace.dto.CreateReinspectionRequest;
import com.exportrace.dto.QAInspectionDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ColdChainRecordRepository coldChainRecordRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private User userProduccion;
    private User userQa;
    private User userLogistica;
    private String tokenProduccion;
    private String tokenQa;
    private String tokenLogistica;
    private Lot testLot;

    @BeforeEach
    void setUp() {
        Role roleProd = roleRepository.findByNombre("PRODUCCION").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("PRODUCCION");
            r.setDescripcion("Operador de Planta");
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

        Role roleLogistica = roleRepository.findByNombre("LOGISTICA").orElseGet(() -> {
            Role r = new Role();
            r.setNombre("LOGISTICA");
            r.setDescripcion("Despacho y Logística");
            r.setActivo(true);
            return roleRepository.save(r);
        });

        userProduccion = new User();
        userProduccion.setNombre("Pedro");
        userProduccion.setApellido("Produccion");
        userProduccion.setEmail("pedro.prod." + System.currentTimeMillis() + "@exportrace.pe");
        userProduccion.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userProduccion.setRole(roleProd);
        userProduccion.setActivo(true);
        userProduccion = userRepository.save(userProduccion);
        tokenProduccion = jwtUtil.generateAccessToken(userProduccion, "sess-prod-1");

        userQa = new User();
        userQa.setNombre("Queta");
        userQa.setApellido("QA");
        userQa.setEmail("queta.qa." + System.currentTimeMillis() + "@exportrace.pe");
        userQa.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userQa.setRole(roleQa);
        userQa.setActivo(true);
        userQa = userRepository.save(userQa);
        tokenQa = jwtUtil.generateAccessToken(userQa, "sess-qa-1");

        userLogistica = new User();
        userLogistica.setNombre("Luis");
        userLogistica.setApellido("Logistica");
        userLogistica.setEmail("luis.log." + System.currentTimeMillis() + "@exportrace.pe");
        userLogistica.setPasswordHash(passwordEncoder.encode("Pass123!"));
        userLogistica.setRole(roleLogistica);
        userLogistica.setActivo(true);
        userLogistica = userRepository.save(userLogistica);
        tokenLogistica = jwtUtil.generateAccessToken(userLogistica, "sess-log-1");

        Product prod = productRepository.findAll().stream().findFirst().orElseGet(() -> {
            Product p = new Product();
            p.setCodigo("PROD-RBAC");
            p.setNombre("Pota Congelada");
            p.setTipoConservacion("CONGELADO");
            return productRepository.save(p);
        });

        testLot = new Lot();
        testLot.setCodigo("EXP-RBAC-" + System.currentTimeMillis());
        testLot.setProducto(prod);
        testLot.setEstado(LotStatus.REGISTERED.name());
        testLot.setPesoNetoKg(20000.0);
        testLot.setFechaProduccion(LocalDate.now());
        testLot.setQrToken("QR-RBAC-" + System.currentTimeMillis());
        testLot = lotRepository.save(testLot);

        // Add fresh cold chain reading
        ColdChainRecord ccr = new ColdChainRecord();
        ccr.setLote(testLot);
        ccr.setTemperaturaCelsius(-20.0);
        ccr.setEstadoMedicion("NORMAL");
        ccr.setUbicacionCamara("Cámara 1");
        ccr.setResponsableNombre("QA Frío");
        ccr.setFechaHora(LocalDateTime.now().minusHours(1));
        coldChainRecordRepository.save(ccr);

        // Add mandatory document
        Document doc = new Document("expediente_dj.pdf", "DECLARACION_JURADA", "/app/data/uploads/dj.pdf", "Doc QA", testLot);
        documentRepository.save(doc);
    }

    @Test
    @DisplayName("CASO 17.1: PRODUCCIÓN intenta registrar inspección QA -> HTTP 403 Forbidden")
    void testCaso17_ProduccionTriesQaInspection_Forbidden403() throws Exception {
        QAInspectionDTO qiDto = new QAInspectionDTO();
        qiDto.setOrganolepticResult("CONFORME");
        qiDto.setInspectorName("Pedro Hacker");
        qiDto.setObservations("Intento no autorizado");

        mockMvc.perform(post("/api/quality/lot/" + testLot.getId())
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qiDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CASO 17.2: PRODUCCIÓN intenta ejecutar reinspección QA -> HTTP 403 Forbidden")
    void testCaso17_ProduccionTriesQaReinspection_Forbidden403() throws Exception {
        CreateReinspectionRequest req = new CreateReinspectionRequest(
                "Subsanación técnica de defectos menores",
                "CONFORME",
                "Pedro Prod"
        );

        mockMvc.perform(post("/api/quality/lot/" + testLot.getId() + "/reinspect")
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CASO 17.3: QA registra inspección de calidad -> HTTP 200 OK")
    void testCaso17_QaRegistersInspection_Ok200() throws Exception {
        QAInspectionDTO qiDto = new QAInspectionDTO();
        qiDto.setOrganolepticResult("CONFORME");
        qiDto.setInspectorName("Dra. Queta QA");
        qiDto.setObservations("Inspección rigurosa autorizada");

        mockMvc.perform(post("/api/quality/lot/" + testLot.getId())
                        .header("Authorization", "Bearer " + tokenQa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qiDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organolepticResult").value("CONFORME"));
    }

    @Test
    @DisplayName("CASO 19.1: PRODUCCIÓN intenta autorizar despacho de contenedor -> HTTP 403 Forbidden")
    void testCaso19_ProduccionTriesDispatch_Forbidden403() throws Exception {
        Dispatch req = new Dispatch();
        req.setNumeroContenedor("MSCU-123456-7");
        req.setPrecintoSeguridad("SEAL-123");

        mockMvc.perform(post("/api/dispatches/lot/" + testLot.getId())
                        .header("Authorization", "Bearer " + tokenProduccion)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CASO 19.2: LOGÍSTICA accede a endpoints de despacho -> Autorizado por Spring Security")
    void testCaso19_LogisticaAccessesDispatch_AuthorizedBySecurity() throws Exception {
        // Logística doesn't get 403 Forbidden (it reaches the controller service logic)
        Dispatch req = new Dispatch();
        req.setNumeroContenedor("MSCU-999999-9");
        req.setPrecintoSeguridad("SEAL-999");

        mockMvc.perform(post("/api/dispatches/lot/" + testLot.getId())
                        .header("Authorization", "Bearer " + tokenLogistica)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @DisplayName("CASO 17/19.3: Petición anónima (sin token) a endpoints protegidos -> HTTP 403 o 401")
    void testAnonymousRequest_Blocked() throws Exception {
        mockMvc.perform(post("/api/quality/lot/" + testLot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403));
    }
}
