package com.exportrace.config;

import com.exportrace.entity.*;
import com.exportrace.entity.Module;
import com.exportrace.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

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
    private UserModuleAccessRepository userModuleAccessRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SessionPolicyRepository sessionPolicyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Seed Modules
        Module modDashboard = getOrCreateModule("DASHBOARD", "Panel Principal", "Dashboards y visualizaciones generales", "/dashboard", "LayoutDashboard", 1);
        Module modLots = getOrCreateModule("LOTES", "Gestión de Lotes", "Registro y consulta de lotes de producción", "/lots", "PackageCheck", 2);
        Module modQuality = getOrCreateModule("CALIDAD", "QualityTrac (QA)", "Inspección organoléptica y evaluación de calidad", "/quality", "ShieldCheck", 3);
        Module modCold = getOrCreateModule("FRIO", "Cadena de Frío", "Monitoreo y registro de temperaturas frigoríficas", "/quality/coldchain", "Thermometer", 4);
        Module modLogistics = getOrCreateModule("LOGISTICA", "LogisTrac Comex", "Gestión logística de exportación y DUA", "/logistics", "FileSpreadsheet", 5);
        Module modCert = getOrCreateModule("CERTIFICACION", "Certificación SANIPES", "Expedientes digitales y certificados sanitarios", "/certification", "Award", 6);
        Module modDispatch = getOrCreateModule("DESPACHO", "Autorización Despacho", "Control de precintos, contenedores y salida", "/dispatch", "Truck", 7);
        Module modManagement = getOrCreateModule("GERENCIA", "Supervisión Gerencial", "KPIs consolidados e indicadores ejecutivos", "/management", "BarChart3", 8);
        Module modAdmin = getOrCreateModule("ADMINISTRACION", "Administración del Sistema", "Gestión de usuarios, roles, permisos y módulos", "/admin", "Sliders", 9);
        Module modAudit = getOrCreateModule("AUDITORIA", "Auditoría & Trazabilidad", "Historial de actividad y registros inmutables", "/admin/audit", "History", 10);

        // 2. Seed Granular Permissions
        List<Permission> allPerms = new ArrayList<>();
        // Users & Roles
        allPerms.add(getOrCreatePermission("USERS_VIEW", "Ver Usuarios", "Permite listar y ver detalles de usuarios", modAdmin));
        allPerms.add(getOrCreatePermission("USERS_CREATE", "Crear Usuarios", "Permite registrar nuevos usuarios en el sistema", modAdmin));
        allPerms.add(getOrCreatePermission("USERS_UPDATE", "Actualizar Usuarios", "Permite editar datos y roles de usuarios", modAdmin));
        allPerms.add(getOrCreatePermission("USERS_DISABLE", "Desactivar Usuarios", "Permite habilitar y deshabilitar cuentas", modAdmin));
        allPerms.add(getOrCreatePermission("USERS_RESET_PASSWORD", "Restablecer Contraseña", "Permite reiniciar credenciales de acceso", modAdmin));
        allPerms.add(getOrCreatePermission("ROLES_VIEW", "Ver Roles", "Permite consultar roles y permisos", modAdmin));
        allPerms.add(getOrCreatePermission("ROLES_MANAGE", "Gestionar Roles", "Permite configurar la matriz de permisos por rol", modAdmin));
        allPerms.add(getOrCreatePermission("ADMIN_DASHBOARD_VIEW", "Dashboard Admin", "Permite acceder al panel administrativo y métricas del sistema", modAdmin));

        // Lotes & Producción
        allPerms.add(getOrCreatePermission("LOTS_VIEW", "Ver Lotes", "Permite visualizar el catálogo y detalle de lotes", modLots));
        allPerms.add(getOrCreatePermission("LOTS_CREATE", "Crear Lotes", "Permite registrar nuevos lotes de materia prima", modLots));
        allPerms.add(getOrCreatePermission("LOTS_UPDATE", "Editar Lotes", "Permite actualizar información de lotes", modLots));

        // QA & Calidad
        allPerms.add(getOrCreatePermission("QUALITY_VIEW", "Ver Calidad", "Permite consultar dictámenes de calidad e inspecciones", modQuality));
        allPerms.add(getOrCreatePermission("QUALITY_MANAGE", "Gestionar Calidad", "Permite realizar y registrar inspecciones organolépticas", modQuality));

        // Frío
        allPerms.add(getOrCreatePermission("COLD_CHAIN_VIEW", "Ver Cadena de Frío", "Permite consultar registros de temperatura", modCold));
        allPerms.add(getOrCreatePermission("COLD_CHAIN_MANAGE", "Gestionar Cadena de Frío", "Permite registrar y validar temperaturas de congelamiento", modCold));

        // Logística & Certificación & Despacho
        allPerms.add(getOrCreatePermission("LOGISTICS_VIEW", "Ver Logística", "Permite consultar estado logístico y DUA", modLogistics));
        allPerms.add(getOrCreatePermission("LOGISTICS_MANAGE", "Gestionar Logística", "Permite gestionar despachos y DUA", modLogistics));
        allPerms.add(getOrCreatePermission("CERTIFICATION_VIEW", "Ver Certificación", "Permite ver expedientes y certificados SANIPES", modCert));
        allPerms.add(getOrCreatePermission("CERTIFICATION_MANAGE", "Gestionar Certificación", "Permite tramitar y registrar certificados sanitarios", modCert));
        allPerms.add(getOrCreatePermission("DISPATCH_VIEW", "Ver Despachos", "Permite consultar programaciones de embarque", modDispatch));
        allPerms.add(getOrCreatePermission("DISPATCH_MANAGE", "Gestionar Despachos", "Permite autorizar salidas y precintado de contenedores", modDispatch));

        // Supervisión & Auditoría
        allPerms.add(getOrCreatePermission("EXECUTIVE_DASHBOARD_VIEW", "Dashboard Ejecutivo", "Permite ver métricas consolidadas de gerencia", modManagement));
        allPerms.add(getOrCreatePermission("AUDIT_VIEW", "Ver Auditoría", "Permite consultar la bitácora inmutable de eventos", modAudit));

        // 3. Seed Roles & Assign Default Permissions
        Role superAdminRole = getOrCreateRole("SUPERADMIN", "Administrador técnico de la plataforma ExporTrace");
        Role adminRole = getOrCreateRole("ADMINISTRADOR", "Jefatura de Administración & TI empresarial");
        Role prodRole = getOrCreateRole("PRODUCCION", "Operaciones y registro de producción");
        Role qaRole = getOrCreateRole("QA", "Control de calidad e inspecciones sanitarias");
        Role logRole = getOrCreateRole("LOGISTICA", "Gestión de cert. SANIPES y despachos");
        Role gerRole = getOrCreateRole("GERENCIA", "Auditoría, trazabilidad y supervisión general");

        // Set default permissions per role
        setRolePermissions(superAdminRole, allPerms); // SuperAdmin gets everything
        setRolePermissions(adminRole, findPermissions(allPerms, "USERS_VIEW", "USERS_CREATE", "USERS_UPDATE", "USERS_DISABLE", "ROLES_VIEW", "LOTS_VIEW", "QUALITY_VIEW", "COLD_CHAIN_VIEW", "LOGISTICS_VIEW", "CERTIFICATION_VIEW", "DISPATCH_VIEW", "EXECUTIVE_DASHBOARD_VIEW", "ADMIN_DASHBOARD_VIEW", "AUDIT_VIEW"));
        setRolePermissions(prodRole, findPermissions(allPerms, "LOTS_VIEW", "LOTS_CREATE", "LOTS_UPDATE", "QUALITY_VIEW"));
        setRolePermissions(qaRole, findPermissions(allPerms, "LOTS_VIEW", "QUALITY_VIEW", "QUALITY_MANAGE", "COLD_CHAIN_VIEW", "COLD_CHAIN_MANAGE", "CERTIFICATION_VIEW"));
        setRolePermissions(logRole, findPermissions(allPerms, "LOTS_VIEW", "COLD_CHAIN_VIEW", "LOGISTICS_VIEW", "LOGISTICS_MANAGE", "CERTIFICATION_VIEW", "CERTIFICATION_MANAGE", "DISPATCH_VIEW", "DISPATCH_MANAGE"));
        setRolePermissions(gerRole, findPermissions(allPerms, "LOTS_VIEW", "QUALITY_VIEW", "COLD_CHAIN_VIEW", "LOGISTICS_VIEW", "CERTIFICATION_VIEW", "DISPATCH_VIEW", "EXECUTIVE_DASHBOARD_VIEW", "AUDIT_VIEW"));

        // 4. Seed Users
        String superAdminPass = System.getenv("SUPERADMIN_INITIAL_PASSWORD") != null ? System.getenv("SUPERADMIN_INITIAL_PASSWORD") : "SuperAdmin2026!";
        User superAdminUser = createUserIfMissing("Super", "Administrador", "superadmin@exportrace.pe", superAdminPass, "Seguridad & Arquitectura Cloud", superAdminRole);
        User adminUser = createUserIfMissing("Carlos", "Mendoza", "admin@exportrace.pe", "Admin123", "Administración & TI", adminRole);
        User prodUser = createUserIfMissing("Renzo", "Alva", "produccion@exportrace.pe", "Prod123", "Operaciones / Producción", prodRole);
        User qaUser = createUserIfMissing("María Elena", "Quispe", "qa@exportrace.pe", "QA123", "Control de Calidad & Frío", qaRole);
        User logUser = createUserIfMissing("Fernando", "Prado", "logistica@exportrace.pe", "Log123", "Logística de Exportación & Comex", logRole);
        User gerUser = createUserIfMissing("Roberto", "Silva", "gerencia@exportrace.pe", "Ger123", "Gerencia General & Operaciones", gerRole);

        // Seed Sample UserModuleAccess (Excepción individual: Dra. María Elena Quispe tiene acceso adicional al módulo CERTIFICACION)
        if (userModuleAccessRepository.count() == 0 && qaUser != null && modCert != null) {
            userModuleAccessRepository.save(new UserModuleAccess(qaUser, modCert, "superadmin@exportrace.pe"));
        }

        // 5. Seed Products
        Product potaBlock = createProductIfMissing("POTA_CONGELADA_BLOCK", "Pota congelada en bloques (Giant Squid Blocks)", "Dosidicus gigas", "Bloques congelados de potera -40°C");
        createProductIfMissing("POTA_ANILLOS_IQF", "Anillos de pota IQF", "Dosidicus gigas", "Anillos de pota congelados individualmente");
        createProductIfMissing("LANGOSTINO_ENTERO", "Langostino entero congelado", "Penaeus vannamei", "Langostino entero de acuicultura Paita");
        createProductIfMissing("CONCHA_DE_ABANICO", "Concha de abanico con coral", "Argopecten purpuratus", "Valvas de abanico frescas de exportación");

        // 6. Seed Initial Lot EXP-2026-001 if DB is empty
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
            qi.setInspectorNombre("Dra. María Elena Quispe");
            qi.setResultadoOrganoleptico("CONFORME");
            qi.setObservaciones("Excelente textura y brillo característico de Dosidicus gigas.");
            qi.setExamenParasitologico("AUSENCIA");
            qualityRepository.save(qi);

            // Cold Chain
            ColdChainRecord cc1 = new ColdChainRecord();
            cc1.setLote(savedLot);
            cc1.setTemperaturaCelsius(-22.4);
            cc1.setUbicacionCamara("Cámara Frigorífica 03");
            cc1.setResponsableNombre("María Elena Quispe");
            cc1.setEstadoMedicion("NORMAL");
            coldChainRepository.save(cc1);

            ColdChainRecord cc2 = new ColdChainRecord();
            cc2.setLote(savedLot);
            cc2.setTemperaturaCelsius(-21.8);
            cc2.setUbicacionCamara("Túnel Congelación Rápida 01");
            cc2.setResponsableNombre("María Elena Quispe");
            cc2.setEstadoMedicion("NORMAL");
            coldChainRepository.save(cc2);

            // Document
            Document doc = new Document("Declaracion_Jurada_Origen_EXP-2026-001.pdf", "DECLARACION_JURADA", "https://exportrace-docs.s3.amazonaws.com/DJ-001.pdf", "Renzo Alva", savedLot);
            documentRepository.save(doc);

            // Sanitary Certification
            SanitaryCertification cert = new SanitaryCertification();
            cert.setLote(savedLot);
            cert.setNumeroCertificado("CS-2026-094182");
            cert.setEstado("APROBADO");
            cert.setEntidadEmisora("SANIPES");
            cert.setObservaciones("Certificado Sanitario Oficial de Exportación aprobado para destino Unión Europea.");
            certificationRepository.save(cert);

            // Dispatch
            Dispatch disp = new Dispatch();
            disp.setLote(savedLot);
            disp.setDuasExportacion("DUA-118-2026-10-094123");
            disp.setNumeroContenedor("SUDU-7894210");
            disp.setPrecintoSeguridad("SANIPES-SEAL-88412");
            disp.setTransportista("HAPAG-LLOYD");
            disp.setPuertoDestino("Puerto de Valencia (ESVLC)");
            disp.setEstado("DESPACHADO");
            dispatchRepository.save(disp);
        }

        // 7. Seed Initial Audit Logs if empty
        if (auditLogRepository.count() == 0) {
            seedInitialAuditLogs(adminUser, prodUser, qaUser, logUser);
        }

        // 8. Seed Initial Real Notifications if empty
        if (notificationRepository.count() == 0) {
            seedInitialNotifications(superAdminUser, adminUser, prodUser, qaUser, logUser, gerUser);
        }

        // 9. Seed Initial Session Policies per Role (Idempotent)
        seedInitialSessionPolicies();
    }

    private void seedInitialSessionPolicies() {
        getOrCreateSessionPolicy("SUPERADMIN", 15, 120, 2);
        getOrCreateSessionPolicy("ADMINISTRADOR", 20, 240, 2);
        getOrCreateSessionPolicy("QA", 30, 360, 2);
        getOrCreateSessionPolicy("PRODUCCION", 60, 480, 5);
        getOrCreateSessionPolicy("LOGISTICA", 45, 480, 5);
        getOrCreateSessionPolicy("GERENCIA", 30, 240, 2);
    }

    private void getOrCreateSessionPolicy(String role, int idle, int absolute, int warning) {
        if (sessionPolicyRepository.findByRole(role).isEmpty()) {
            sessionPolicyRepository.save(new SessionPolicy(role, idle, absolute, warning, "SYSTEM_INIT"));
        }
    }

    private Module getOrCreateModule(String codigo, String nombre, String descripcion, String ruta, String icono, Integer orden) {
        return moduleRepository.findByCodigo(codigo).orElseGet(() ->
                moduleRepository.save(new Module(codigo, nombre, descripcion, ruta, icono, orden)));
    }

    private Permission getOrCreatePermission(String codigo, String nombre, String descripcion, Module module) {
        return permissionRepository.findByCodigo(codigo).orElseGet(() ->
                permissionRepository.save(new Permission(codigo, nombre, descripcion, module)));
    }

    private Role getOrCreateRole(String nombre, String descripcion) {
        return roleRepository.findByNombre(nombre).orElseGet(() ->
                roleRepository.save(new Role(nombre, descripcion)));
    }

    private void setRolePermissions(Role role, List<Permission> perms) {
        role.setPermissions(new HashSet<>(perms));
        roleRepository.save(role);
    }

    private List<Permission> findPermissions(List<Permission> all, String... codes) {
        Set<String> codeSet = new HashSet<>(Arrays.asList(codes));
        List<Permission> result = new ArrayList<>();
        for (Permission p : all) {
            if (codeSet.contains(p.getCodigo())) {
                result.add(p);
            }
        }
        return result;
    }

    private User createUserIfMissing(String nombre, String apellido, String email, String plainPassword, String area, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setNombre(nombre);
            u.setApellido(apellido);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(plainPassword));
            u.setArea(area);
            u.setRole(role);
            u.setActivo(true);
            u.setFechaCreacion(LocalDateTime.now().minusDays(15));
            u.setUltimoAcceso(LocalDateTime.now().minusHours(2));
            return userRepository.save(u);
        });
    }

    private Product createProductIfMissing(String codigo, String nombre, String cientifico, String desc) {
        return productRepository.findByCodigo(codigo).orElseGet(() -> {
            Product p = new Product();
            p.setCodigo(codigo);
            p.setNombre(nombre);
            p.setNombreCientifico(cientifico);
            p.setDescripcion(desc);
            p.setActivo(true);
            return productRepository.save(p);
        });
    }

    private void seedInitialAuditLogs(User admin, User prod, User qa, User log) {
        auditLogRepository.save(new AuditLog(
                admin != null ? admin.getId() : 1L,
                admin != null ? admin.getNombre() : "Carlos Mendoza",
                "ADMINISTRADOR",
                "LOGIN_SUCCESS",
                "AUTENTICACION",
                "User",
                "1",
                "Inicio de sesión exitoso en la plataforma ExporTrace",
                null,
                "Rol: ADMINISTRADOR",
                "EXITOSO",
                "192.168.1.10",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        ));

        auditLogRepository.save(new AuditLog(
                prod != null ? prod.getId() : 2L,
                prod != null ? prod.getNombre() : "Renzo Alva",
                "PRODUCCION",
                "LOT_CREATED",
                "LOTES",
                "Lot",
                "EXP-2026-001",
                "Registro de lote de materia prima EXP-2026-001 - Pota congelada en bloques",
                null,
                "Estado: DRAFT, Peso: 26500 kg",
                "EXITOSO",
                "192.168.1.25",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        ));

        auditLogRepository.save(new AuditLog(
                qa != null ? qa.getId() : 3L,
                qa != null ? qa.getNombre() : "María Elena Quispe",
                "QA",
                "QUALITY_INSPECTION_CREATED",
                "CALIDAD",
                "QualityInspection",
                "EXP-2026-001",
                "Inspección organoléptica aprobada con resultado CONFORME",
                "DRAFT",
                "READY_FOR_CERTIFICATION",
                "EXITOSO",
                "192.168.1.30",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        ));

        auditLogRepository.save(new AuditLog(
                log != null ? log.getId() : 4L,
                log != null ? log.getNombre() : "Fernando Prado",
                "LOGISTICA",
                "CERTIFICATION_CREATED",
                "CERTIFICACION",
                "SanitaryCertification",
                "CS-2026-094182",
                "Emisión de Certificado Sanitario Oficial SANIPES para lote EXP-2026-001",
                "TRAMITE",
                "EMITIDO",
                "EXITOSO",
                "192.168.1.42",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        ));
    }

    private void seedInitialNotifications(User superAdmin, User admin, User prod, User qa, User log, User ger) {
        // QA Notifications
        notificationRepository.save(new Notification(
                qa, "QA",
                "Inspección Pendiente: Lote EXP-2026-001",
                "El lote de Pota Congelada en Bloques fue recepcionado en Planta Paita #01 y requiere evaluación organoléptica formal.",
                "INSPECTION_PENDING", "HIGH", "QUALITY", "LOT", "EXP-2026-001", "/quality/inspect/1"
        ));

        notificationRepository.save(new Notification(
                qa, "QA",
                "Alerta de Cadena de Frío - Cámara #02",
                "Sensor T-02 registró fluctuación térmica a -17.2°C durante el precintado. Revisión de termorregistrador solicitada.",
                "COLD_CHAIN_ALERT", "URGENT", "COLD_CHAIN", "COLD_ROOM", "CAMARA-02", "/quality/coldchain"
        ));

        // LOGISTICA Notifications
        notificationRepository.save(new Notification(
                log, "LOGISTICA",
                "Certificado SANIPES Listo para Tramitación",
                "Lote EXP-2026-001 validado conforme en calidad organoléptica. Expediente listo para emisión de Certificado Sanitario.",
                "CERTIFICATION_PENDING", "HIGH", "CERTIFICATION", "LOT", "EXP-2026-001", "/certification"
        ));

        notificationRepository.save(new Notification(
                log, "LOGISTICA",
                "Programación de Embarque Puerto Paita",
                "Contenedor CMAU-984120 programado para inspección de precinto y despacho internacional.",
                "DISPATCH_PENDING", "NORMAL", "DISPATCH", "DISPATCH", "EXP-2026-001", "/dispatch"
        ));

        // PRODUCCION Notifications
        notificationRepository.save(new Notification(
                prod, "PRODUCCION",
                "Lote EXP-2026-004 Observado en Muestreo",
                "Se detectó variación leve en textura durante el muestreo preliminar. Requiere calibración en mesa de corte.",
                "QUALITY_OBSERVED", "URGENT", "QUALITY", "LOT", "EXP-2026-004", "/lots"
        ));

        // GERENCIA Notifications
        notificationRepository.save(new Notification(
                ger, "GERENCIA",
                "Reporte Mensual de Certificación Sanitaria",
                "Tasa de cumplimiento organoléptico del 98.4% alcanzada en el periodo actual con 140 TN procesadas.",
                "CERTIFICATION_APPROVED", "NORMAL", "MANAGEMENT", "REPORT", "2026-09", "/management"
        ));

        // SUPERADMIN Notifications
        notificationRepository.save(new Notification(
                superAdmin, "SUPERADMIN",
                "Auditoría del Sistema y Motor JWT Activo",
                "Políticas de seguridad HMAC-SHA256 y control granular de excepciones UserModuleAccess operando al 100%.",
                "SECURITY_ALERT", "NORMAL", "SECURITY", "SYSTEM", "SECURITY-01", "/superadmin/security"
        ));
    }
}
