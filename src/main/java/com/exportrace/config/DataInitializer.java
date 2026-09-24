package com.exportrace.config;

import com.exportrace.entity.*;
import com.exportrace.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Seed Roles
        Role adminRole = getOrCreateRole("ADMINISTRADOR", "Administrador total del sistema");
        Role prodRole = getOrCreateRole("PRODUCCION", "Operaciones y registro de producción");
        Role qaRole = getOrCreateRole("QA", "Control de calidad e inspecciones sanitarias");
        Role logRole = getOrCreateRole("LOGISTICA", "Gestión de cert. SANIPES y despachos");
        Role gerRole = getOrCreateRole("GERENCIA", "Auditoría, trazabilidad y supervisión general");

        // 2. Seed Users
        createUserIfMissing("Ing. Carlos Mendoza", "admin@exportrace.pe", "Admin123", "Administración & TI", adminRole);
        createUserIfMissing("Renzo Alva", "produccion@exportrace.pe", "Prod123", "Operaciones / Producción", prodRole);
        createUserIfMissing("Dra. María Elena Quispe", "qa@exportrace.pe", "QA123", "Control de Calidad & Frío", qaRole);
        createUserIfMissing("Lic. Fernando Prado", "logistica@exportrace.pe", "Log123", "Logística de Exportación & Comex", logRole);
        createUserIfMissing("Ing. Roberto Silva (CEO)", "gerencia@exportrace.pe", "Ger123", "Gerencia General & Operaciones", gerRole);

        // 3. Seed Products
        Product potaBlock = createProductIfMissing("POTA_CONGELADA_BLOCK", "Pota congelada en bloques (Giant Squid Blocks)", "Dosidicus gigas", "Bloques congelados de potera -40°C");
        createProductIfMissing("POTA_ANILLOS_IQF", "Anillos de pota IQF", "Dosidicus gigas", "Anillos de pota congelados individualmente");
        createProductIfMissing("LANGOSTINO_ENTERO", "Langostino entero congelado", "Penaeus vannamei", "Langostino entero de acuicultura Paita");
        createProductIfMissing("CONCHA_DE_ABANICO", "Concha de abanico con coral", "Argopecten purpuratus", "Valvas de abanico frescas de exportación");

        // 4. Seed Initial Lot EXP-2026-001 if DB is empty
        if (lotRepository.count() == 0) {
            Lot lot = new Lot();
            lot.setCodigo("EXP-2026-001");
            lot.setEstado("READY_FOR_DISPATCH");
            lot.setProducto(potaBlock);
            lot.setCantidadEmpaques(1060);
            lot.setTipoEmpaque("TN");
            lot.setPesoNetoKg(26500.0);
            lot.setPlantaProcesamiento("Planta Paita #01");
            lot.setLineaProcesamiento("Línea 02 - Bloques Exportación");
            lot.setProveedor("Asociación Pesquera Artesanal Paita Norte");
            lot.setEmbarcacion("E/P Don Luis II (CO-18492-PM)");
            lot.setFechaProduccion(LocalDate.of(2026, 8, 20));
            lot.setFechaVencimiento(LocalDate.of(2028, 8, 20));
            lot.setInspeccionadoPor("Renzo Alva");
            lot.setObservaciones("Materia prima de primera frescura, captura nocturna con potera.");
            lot.setQrToken("EXP2026001HASH98412089421");

            Lot savedLot = lotRepository.save(lot);

            // History
            lotHistoryRepository.save(new LotHistory(savedLot, null, "DRAFT", "Renzo Alva", "PRODUCCION", "Creación inicial de lote en planta"));
            lotHistoryRepository.save(new LotHistory(savedLot, "DRAFT", "IN_QA", "Renzo Alva", "PRODUCCION", "Envío a inspección QA"));
            lotHistoryRepository.save(new LotHistory(savedLot, "IN_QA", "READY_FOR_CERTIFICATION", "Dra. María Elena Quispe", "QA", "Inspección organoléptica CONFORME"));
            lotHistoryRepository.save(new LotHistory(savedLot, "READY_FOR_CERTIFICATION", "CERTIFIED", "Lic. Fernando Prado", "LOGISTICA", "Certificado SANIPES emitido: CS-2026-094182"));

            // QA
            QualityInspection qi = new QualityInspection();
            qi.setLote(savedLot);
            qi.setFechaInspeccion(LocalDateTime.of(2026, 8, 21, 10, 0));
            qi.setInspectorNombre("Dra. María Elena Quispe");
            qi.setApariencia("EXCELENTE");
            qi.setEvaluacionColor("CONFORME");
            qi.setTextura("FIRM");
            qi.setOlor("CARACTERISTICO");
            qi.setExamenParasitologico("AUSENCIA");
            qi.setResultadoOrganoleptico("CONFORME");
            qi.setObservaciones("Muestra representativa de 50 bloques auditados. Cero anomalías.");
            qualityRepository.save(qi);

            // Cold Chain
            ColdChainRecord c1 = new ColdChainRecord();
            c1.setLote(savedLot);
            c1.setFechaHora(LocalDateTime.of(2026, 8, 20, 12, 0));
            c1.setTemperaturaCelsius(-18.5);
            c1.setUbicacionCamara("Cámara 02 - Paita");
            c1.setResponsableNombre("Técnico Frigorífico QA");
            c1.setEstadoMedicion("NORMAL");
            c1.setObservaciones("Ingreso a cámara tras congelamiento rápido");
            coldChainRepository.save(c1);

            ColdChainRecord c2 = new ColdChainRecord();
            c2.setLote(savedLot);
            c2.setFechaHora(LocalDateTime.of(2026, 8, 25, 14, 30));
            c2.setTemperaturaCelsius(-19.2);
            c2.setUbicacionCamara("Cámara 02 - Paita");
            c2.setResponsableNombre("Técnico Frigorífico QA");
            c2.setEstadoMedicion("NORMAL");
            c2.setObservaciones("Verificación semanal de control de frío");
            coldChainRepository.save(c2);

            // Documents
            documentRepository.save(new Document("Declaración Jurada de Origen.pdf", "DECLARACION_JURADA", "/documents/DJ_Origen.pdf", "Renzo Alva", savedLot));
            documentRepository.save(new Document("Informe de Ensayos Microbiológicos SANIPES.pdf", "CERTIFICADO_ANALISIS", "/documents/Lab_SANIPES.pdf", "Dra. María Elena Quispe", savedLot));
            documentRepository.save(new Document("Certificado Sanitario SANIPES.pdf", "CERTIFICADO_SANITARIO", "/documents/CS_SANIPES.pdf", "Lic. Fernando Prado", savedLot));

            // Certification
            SanitaryCertification sc = new SanitaryCertification();
            sc.setLote(savedLot);
            sc.setNumeroCertificado("CS-2026-094182");
            sc.setEstado("APROBADO");
            sc.setEntidadEmisora("SANIPES");
            sc.setFechaSolicitud(LocalDateTime.of(2026, 8, 22, 9, 0));
            sc.setFechaEmision(LocalDateTime.of(2026, 8, 24, 16, 0));
            sc.setFechaVencimiento(LocalDateTime.of(2027, 2, 24, 23, 59));
            sc.setObservaciones("Apto para consumo humano directo e importación a mercados de Asia y UE.");
            certificationRepository.save(sc);
        }
    }

    private Role getOrCreateRole(String nombre, String descripcion) {
        return roleRepository.findByNombre(nombre)
                .orElseGet(() -> roleRepository.save(new Role(nombre, descripcion)));
    }

    private void createUserIfMissing(String nombre, String email, String password, String area, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setNombre(nombre);
            user.setEmail(email);
            user.setContrasena(passwordEncoder.encode(password));
            user.setArea(area);
            user.setRole(role);
            user.setEstado("ACTIVO");
            userRepository.save(user);
        }
    }

    private Product createProductIfMissing(String codigo, String nombreComercial, String nombreCientifico, String descripcion) {
        return productRepository.findByCodigo(codigo)
                .orElseGet(() -> productRepository.save(new Product(codigo, nombreComercial, nombreCientifico, descripcion)));
    }
}
