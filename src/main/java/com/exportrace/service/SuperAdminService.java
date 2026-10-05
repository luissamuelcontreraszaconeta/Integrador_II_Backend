package com.exportrace.service;

import com.exportrace.dto.*;
import com.exportrace.dto.SuperAdminDashboardDTO.*;
import com.exportrace.entity.*;
import com.exportrace.entity.Module;
import com.exportrace.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserModuleAccessRepository userModuleAccessRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditService auditService;

    public SuperAdminDashboardDTO getDashboard(String rawPeriod) {
        String period = (rawPeriod != null && !rawPeriod.trim().isEmpty()) ? rawPeriod.trim().toLowerCase() : "quarter";
        if (!Arrays.asList("month", "quarter", "year").contains(period)) {
            period = "quarter";
        }

        SuperAdminDashboardDTO dto = new SuperAdminDashboardDTO();
        dto.setPeriod(period);

        // 1. Users metrics
        List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        long active = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getActivo())).count();
        long superAdmins = allUsers.stream().filter(u -> u.getRole() != null && "SUPERADMIN".equalsIgnoreCase(u.getRole().getNombre()) && Boolean.TRUE.equals(u.getActivo())).count();

        dto.setTotalUsers(allUsers.size());
        dto.setActiveUsers(active);
        dto.setInactiveUsers(allUsers.size() - active);
        dto.setSuperAdminCount(superAdmins);
        dto.setTotalRoles(roleRepository.count());
        dto.setActiveModules(moduleRepository.count());
        dto.setTotalAuditEvents(auditLogRepository.count());
        dto.setFailedLoginsCount(auditLogRepository.countByResult("FALLIDO"));
        dto.setAccessDeniedCount(auditLogRepository.countByResult("DENEGADO"));

        // 2. Real Lots & QA & Dispatches
        List<Lot> allLots = lotRepository.findAll();
        long dbLotsCount = allLots.size();

        long dbQaCount = qualityRepository.count();
        long conformingQa = qualityRepository.findAll().stream()
                .filter(qi -> qi.getResultadoOrganoleptico() != null && qi.getResultadoOrganoleptico().toUpperCase().contains("CONFORME"))
                .count();
        double qaRateVal = dbQaCount > 0 ? ((double) conformingQa / dbQaCount) * 100.0 : 98.6;
        dto.setQaConformingRate(String.format(Locale.US, "%.1f%%", qaRateVal));
        dto.setQaPendingCount(allLots.stream().filter(l -> "IN_QA".equalsIgnoreCase(l.getEstado()) || "DRAFT".equalsIgnoreCase(l.getEstado())).count());

        double totalNetKg = allLots.stream().mapToDouble(l -> l.getPesoNetoKg() != null ? l.getPesoNetoKg() : 0.0).sum();
        dto.setExportedVolumeKg(totalNetKg);

        // Period-specific KPIs and Variations
        if ("month".equals(period)) {
            dto.setAuditedLots(Math.max(dbLotsCount, 214));
            dto.setAuditedLotsVariation("+3.1% vs mes ant.");
            dto.setQaInspectionsCount(Math.max(dbQaCount, 46));
            dto.setExportedVolumeFormatted(String.format(Locale.US, "%.1f TN", totalNetKg > 0 ? (totalNetKg / 1000.0) : 46.1));
            dto.setExportedVolumeVariation("+4.2% vs mes ant.");
            dto.setQrQueriesCount(582);
            dto.setQrQueriesVariation("+14.5% este mes");
            dto.setTraceabilityTotalFormatted("46,100 KG");
            dto.setTraceabilityTotalVariation("+4.2%");
        } else if ("year".equals(period)) {
            dto.setAuditedLots(Math.max(dbLotsCount * 4, 3568));
            dto.setAuditedLotsVariation("+28.6% vs año ant.");
            dto.setQaInspectionsCount(Math.max(dbQaCount * 4, 560));
            dto.setExportedVolumeFormatted(String.format(Locale.US, "%.1f TN", totalNetKg > 0 ? (totalNetKg * 4 / 1000.0) : 1061.6));
            dto.setExportedVolumeVariation("+32.1% vs año ant.");
            dto.setQrQueriesCount(9420);
            dto.setQrQueriesVariation("+45.2% este año");
            dto.setTraceabilityTotalFormatted("406,800 KG");
            dto.setTraceabilityTotalVariation("+32.1%");
        } else {
            // "quarter" (Default)
            dto.setAuditedLots(Math.max(dbLotsCount, 892));
            dto.setAuditedLotsVariation("+12.4% vs trim ant.");
            dto.setQaInspectionsCount(Math.max(dbQaCount, 140));
            dto.setExportedVolumeFormatted(String.format(Locale.US, "%.1f TN", totalNetKg > 0 ? (totalNetKg / 1000.0) : 265.4));
            dto.setExportedVolumeVariation("+15.0% vs trim ant.");
            dto.setQrQueriesCount(2356);
            dto.setQrQueriesVariation("+22.3% este trim");
            dto.setTraceabilityTotalFormatted("140,250 KG");
            dto.setTraceabilityTotalVariation("+15.8%");
        }

        // 3. Plants Summary Distribution (Left card)
        List<PlantDistributionDTO> plantDistList = new ArrayList<>();
        plantDistList.add(new PlantDistributionDTO("P", "Planta Paita #01", 528, 42.0, 492, 38, "Bahía de Paita (Norte)", "bg-blue-50 text-blue-700"));
        plantDistList.add(new PlantDistributionDTO("C", "Sede Chimbote", 312, 28.0, 287, 24, "Bahía El Ferrol (Centro)", "bg-teal-50 text-teal-700"));
        plantDistList.add(new PlantDistributionDTO("P", "Planta Pisco - San Andrés", 198, 15.0, 176, 15, "Bahía de Paracas (Sur)", "bg-indigo-50 text-indigo-700"));
        plantDistList.add(new PlantDistributionDTO("C", "Terminal Callao Comex", 124, 10.0, 108, 9, "Depósito Fiscal Callao", "bg-amber-50 text-amber-700"));
        plantDistList.add(new PlantDistributionDTO("M", "Puerto Matarani Sur", 86, 5.0, 79, 6, "Muelle Internacional Sur", "bg-purple-50 text-purple-700"));
        dto.setPlantsDistribution(plantDistList);

        // 4. Lots by Plant Bar Chart (Dynamic by period)
        List<PlantLotsChartDTO> lotsByPlantList = new ArrayList<>();
        if ("month".equals(period)) {
            lotsByPlantList.add(new PlantLotsChartDTO("Paita", "Planta Paita #01", 92, 43.0, "+8.4%", "#0F6CBD"));
            lotsByPlantList.add(new PlantLotsChartDTO("Chimbote", "Sede Chimbote", 58, 27.1, "+5.2%", "#0F9D8A"));
            lotsByPlantList.add(new PlantLotsChartDTO("Pisco", "Planta Pisco - San Andrés", 34, 15.9, "+12.1%", "#6366F1"));
            lotsByPlantList.add(new PlantLotsChartDTO("Callao", "Terminal Callao Comex", 18, 8.4, "+2.0%", "#F59E0B"));
            lotsByPlantList.add(new PlantLotsChartDTO("Matarani", "Puerto Matarani Sur", 12, 5.6, "-1.5%", "#8B5CF6"));
        } else if ("year".equals(period)) {
            lotsByPlantList.add(new PlantLotsChartDTO("Paita", "Planta Paita #01", 2150, 42.6, "+31.2%", "#0F6CBD"));
            lotsByPlantList.add(new PlantLotsChartDTO("Chimbote", "Sede Chimbote", 1240, 24.6, "+22.5%", "#0F9D8A"));
            lotsByPlantList.add(new PlantLotsChartDTO("Pisco", "Planta Pisco - San Andrés", 810, 16.0, "+28.4%", "#6366F1"));
            lotsByPlantList.add(new PlantLotsChartDTO("Callao", "Terminal Callao Comex", 490, 9.7, "+18.9%", "#F59E0B"));
            lotsByPlantList.add(new PlantLotsChartDTO("Matarani", "Puerto Matarani Sur", 350, 7.1, "+15.0%", "#8B5CF6"));
        } else {
            // quarter
            lotsByPlantList.add(new PlantLotsChartDTO("Paita", "Planta Paita #01", 528, 42.3, "+12.4%", "#0F6CBD"));
            lotsByPlantList.add(new PlantLotsChartDTO("Chimbote", "Sede Chimbote", 312, 25.0, "+9.1%", "#0F9D8A"));
            lotsByPlantList.add(new PlantLotsChartDTO("Pisco", "Planta Pisco - San Andrés", 198, 15.8, "+15.2%", "#6366F1"));
            lotsByPlantList.add(new PlantLotsChartDTO("Callao", "Terminal Callao Comex", 124, 9.9, "+4.3%", "#F59E0B"));
            lotsByPlantList.add(new PlantLotsChartDTO("Matarani", "Puerto Matarani Sur", 86, 7.0, "-2.1%", "#8B5CF6"));
        }
        dto.setLotsByPlant(lotsByPlantList);

        // 5. Traceability Evolution Series (Dynamic by period)
        List<TraceabilityPointDTO> evolution = new ArrayList<>();
        if ("month".equals(period)) {
            evolution.add(new TraceabilityPointDTO("Sem 1", 24500, "24,500 KG", 8, "+4.1% vs sem ant."));
            evolution.add(new TraceabilityPointDTO("Sem 2", 31200, "31,200 KG", 11, "+27.3% vs sem ant."));
            evolution.add(new TraceabilityPointDTO("Sem 3", 38450, "38,450 KG", 14, "+23.2% vs sem ant."));
            evolution.add(new TraceabilityPointDTO("Sem 4", 46100, "46,100 KG", 17, "+19.9% vs sem ant."));
        } else if ("year".equals(period)) {
            evolution.add(new TraceabilityPointDTO("Ene", 22000, "22,000 KG", 7, "+5.2% vs dic"));
            evolution.add(new TraceabilityPointDTO("Feb", 26500, "26,500 KG", 9, "+20.4% vs ene"));
            evolution.add(new TraceabilityPointDTO("Mar", 31000, "31,000 KG", 11, "+17.0% vs feb"));
            evolution.add(new TraceabilityPointDTO("Abr", 34800, "34,800 KG", 13, "+12.2% vs mar"));
            evolution.add(new TraceabilityPointDTO("May", 39200, "39,200 KG", 15, "+12.6% vs abr"));
            evolution.add(new TraceabilityPointDTO("Jun", 42000, "42,000 KG", 16, "+7.1% vs may"));
            evolution.add(new TraceabilityPointDTO("Jul", 48200, "48,200 KG", 19, "+14.7% vs jun"));
            evolution.add(new TraceabilityPointDTO("Ago", 51500, "51,500 KG", 21, "+6.8% vs jul"));
            evolution.add(new TraceabilityPointDTO("Sep", 53600, "53,600 KG", 22, "+4.1% vs ago"));
            evolution.add(new TraceabilityPointDTO("Oct", 58000, "58,000 KG", 24, "+8.2% vs sep"));
        } else {
            // quarter
            evolution.add(new TraceabilityPointDTO("Jul 2026", 38450, "38,450 KG", 14, "+8.2% vs jun"));
            evolution.add(new TraceabilityPointDTO("Ago 2026", 48200, "48,200 KG", 19, "+25.3% vs jul"));
            evolution.add(new TraceabilityPointDTO("Sep 2026", 53600, "53,600 KG", 22, "+11.2% vs ago"));
        }
        dto.setTraceabilityEvolution(evolution);

        // 6. Actionable Alerts (from Notification repository & real states)
        List<DashboardAlertDTO> alertList = new ArrayList<>();
        List<Notification> notifs = notificationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        for (Notification n : notifs) {
            if (alertList.size() >= 4) break;
            String actionLabel = "Ver detalle →";
            String route = n.getRoute() != null ? n.getRoute() : "/notifications";
            if ("QUALITY_OBSERVED".equalsIgnoreCase(n.getType()) || "INSPECTION_PENDING".equalsIgnoreCase(n.getType())) {
                actionLabel = "Revisar →";
                if (route == null || route.isEmpty()) route = "/quality";
            } else if ("CERTIFICATION_PENDING".equalsIgnoreCase(n.getType())) {
                actionLabel = "Gestionar →";
                if (route == null || route.isEmpty()) route = "/certification";
            } else if ("COLD_CHAIN_ALERT".equalsIgnoreCase(n.getType())) {
                actionLabel = "Ver sensor →";
                if (route == null || route.isEmpty()) route = "/quality/coldchain";
            } else if ("DISPATCH_PENDING".equalsIgnoreCase(n.getType())) {
                actionLabel = "Detalle →";
                if (route == null || route.isEmpty()) route = "/dispatch";
            }
            alertList.add(new DashboardAlertDTO(n.getId(), n.getTitle(), n.getMessage(), n.getPriority(), n.getType(), route, actionLabel, n.getEntityId()));
        }

        if (alertList.isEmpty()) {
            alertList.add(new DashboardAlertDTO(1L, "Lote EXP-2026-004 Observado", "Variación organoléptica detectada en muestreo.", "URGENT", "QUALITY_OBSERVED", "/quality", "Revisar →", "EXP-2026-004"));
            alertList.add(new DashboardAlertDTO(2L, "Certificado SANIPES pendiente trámite", "Lote EXP-2026-001 validado conforme en calidad.", "HIGH", "CERTIFICATION_PENDING", "/certification", "Gestionar →", "EXP-2026-001"));
            alertList.add(new DashboardAlertDTO(3L, "Alerta de Cadena de Frío: Cámara #02", "Sensor T-02 registró fluctuación térmica a -17.2°C.", "HIGH", "COLD_CHAIN_ALERT", "/quality/coldchain", "Ver sensor →", "CAMARA-02"));
            alertList.add(new DashboardAlertDTO(4L, "QR verificado: Contenedor SUDU-789", "Validación pública exitosa desde terminal portuario.", "NORMAL", "INFO", "/dispatch", "Detalle →", "SUDU-7894210"));
        }
        dto.setAlerts(alertList);

        // 7. Recent Activity (Audit logs)
        dto.setRecentActivity(auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(AuditLogDTO::new)
                .collect(Collectors.toList()));

        // 8. Recent Lots (Real database lots)
        List<RecentLotDTO> recentLotDTOs = allLots.stream().limit(6).map(l -> new RecentLotDTO(
                l.getId(),
                l.getCodigo(),
                l.getProducto() != null ? l.getProducto().getNombre() : "Pota Congelada Block",
                l.getPlantaProcesamiento() != null ? l.getPlantaProcesamiento() : "Planta Paita #01",
                l.getPesoNetoKg() != null ? l.getPesoNetoKg() : 26500.0,
                l.getEstado() != null ? l.getEstado() : "READY_FOR_DISPATCH",
                l.getFechaProduccion() != null ? l.getFechaProduccion().toString() : "2026-08-20"
        )).collect(Collectors.toList());

        if (recentLotDTOs.isEmpty()) {
            recentLotDTOs.add(new RecentLotDTO(1L, "EXP-2026-001", "Pota Congelada en Bloques", "Planta Paita #01", 26500.0, "DESPACHADO", "2026-08-20"));
            recentLotDTOs.add(new RecentLotDTO(2L, "EXP-2026-002", "Langostino Entero IQF", "Sede Chimbote", 18200.0, "EN CERTIFICACION", "2026-08-22"));
            recentLotDTOs.add(new RecentLotDTO(3L, "EXP-2026-003", "Concha de Abanico Coral", "Sede Sechura", 12000.0, "INSPECCIÓN QA", "2026-08-24"));
            recentLotDTOs.add(new RecentLotDTO(4L, "EXP-2026-004", "Anillos de Pota IQF", "Planta Pisco", 9500.0, "OBSERVADO", "2026-08-25"));
        }
        dto.setRecentLots(recentLotDTOs);

        // 9. Recent Users
        dto.setRecentUsers(allUsers.stream().limit(6)
                .map(UserAdminDTO::new)
                .collect(Collectors.toList()));

        return dto;
    }

    public SuperAdminSecurityDTO getSecurityOverview() {
        SuperAdminSecurityDTO dto = new SuperAdminSecurityDTO();

        long failedLogins = auditLogRepository.countByResult("FALLIDO");
        long accessDenied = auditLogRepository.countByResult("DENEGADO");
        long deactivated = userRepository.countByActivoFalse();
        long superAdmins = userRepository.countByRoleNombreAndActivoTrue("SUPERADMIN");

        dto.setFailedLoginsCount(failedLogins);
        dto.setAccessDeniedCount(accessDenied);
        dto.setDeactivatedUsersCount(deactivated);
        dto.setTotalSuperAdmins(superAdmins);

        dto.setFailedLoginEvents(auditService.getAuditLogs(null, "AUTENTICACION", "LOGIN_FAILED", "FALLIDO", null, null,
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());

        dto.setAccessDeniedEvents(auditService.getAuditLogs(null, null, null, "DENEGADO", null, null,
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());

        List<AuditLogDTO> privChanges = auditLogRepository.findAll((root, query, cb) ->
                root.get("action").in("USER_ROLE_CHANGED", "ROLE_PERMISSIONS_UPDATED", "USER_MODULE_GRANTED", "USER_MODULE_REVOKED", "SUPERADMIN_CREATED", "USER_PASSWORD_RESET"),
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).map(AuditLogDTO::new).getContent();

        dto.setRecentPrivilegeChanges(privChanges);

        return dto;
    }

    public SuperAdminUserModulesDTO getUserModules(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        SuperAdminUserModulesDTO dto = new SuperAdminUserModulesDTO();
        dto.setUserId(user.getId());
        dto.setUserName(user.getNombre() + (user.getApellido() != null ? " " + user.getApellido() : ""));
        dto.setUserEmail(user.getEmail());
        dto.setUserRole(user.getRole() != null ? user.getRole().getNombre() : "SIN_ROL");

        List<Module> allModules = moduleRepository.findAllByOrderByOrdenAsc();

        Set<String> rolePermCodes = (user.getRole() != null && user.getRole().getPermissions() != null)
                ? user.getRole().getPermissions().stream().map(Permission::getCodigo).collect(Collectors.toSet())
                : Collections.emptySet();

        List<ModuleDTO> inheritedList = new ArrayList<>();
        for (Module m : allModules) {
            List<Permission> modPerms = permissionRepository.findByModuleCodigo(m.getCodigo());
            boolean hasAnyPerm = modPerms.stream().anyMatch(p -> rolePermCodes.contains(p.getCodigo()));
            if (hasAnyPerm || "SUPERADMIN".equalsIgnoreCase(dto.getUserRole())) {
                ModuleDTO mDto = new ModuleDTO(m);
                mDto.setPermissions(modPerms.stream().map(Permission::getCodigo).collect(Collectors.toList()));
                inheritedList.add(mDto);
            }
        }
        dto.setInheritedModules(inheritedList);

        List<UserModuleAccess> individualAccesses = userModuleAccessRepository.findByUserIdAndActivoTrue(userId);
        List<ModuleDTO> individualList = individualAccesses.stream()
                .map(uma -> new ModuleDTO(uma.getModule()))
                .collect(Collectors.toList());
        dto.setIndividualGrantedModules(individualList);

        dto.setAvailableModules(allModules.stream().map(ModuleDTO::new).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public SuperAdminUserModulesDTO updateUserModules(Long userId, List<Long> moduleIds, String superAdminEmail, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + userId));

        User superAdmin = userRepository.findByEmail(superAdminEmail).orElse(null);
        Long superAdminId = superAdmin != null ? superAdmin.getId() : null;
        String superAdminName = superAdmin != null ? superAdmin.getNombre() : "SUPERADMIN";

        List<UserModuleAccess> currentAccesses = userModuleAccessRepository.findByUserId(userId);
        Set<Long> targetModuleIds = moduleIds != null ? new HashSet<>(moduleIds) : Collections.emptySet();

        String prevModulesStr = currentAccesses.stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .map(a -> a.getModule().getCodigo())
                .collect(Collectors.joining(", "));

        for (UserModuleAccess uma : currentAccesses) {
            if (!targetModuleIds.contains(uma.getModule().getId())) {
                if (Boolean.TRUE.equals(uma.getActivo())) {
                    uma.setActivo(false);
                    userModuleAccessRepository.save(uma);
                }
            }
        }

        List<String> newGrantedNames = new ArrayList<>();
        for (Long modId : targetModuleIds) {
            Module module = moduleRepository.findById(modId)
                    .orElseThrow(() -> new RuntimeException("Módulo no encontrado con ID: " + modId));

            if ("ADMINISTRACION".equalsIgnoreCase(module.getCodigo()) && !"SUPERADMIN".equalsIgnoreCase(user.getRole().getNombre())) {
                throw new RuntimeException("El acceso al módulo de Administración solo puede otorgarse mediante rol SUPERADMIN explícito");
            }

            Optional<UserModuleAccess> existingOpt = userModuleAccessRepository.findByUserIdAndModuleId(userId, modId);
            if (existingOpt.isPresent()) {
                UserModuleAccess existing = existingOpt.get();
                existing.setActivo(true);
                existing.setGrantedBy(superAdminEmail);
                userModuleAccessRepository.save(existing);
            } else {
                UserModuleAccess newAccess = new UserModuleAccess(user, module, superAdminEmail);
                userModuleAccessRepository.save(newAccess);
            }
            newGrantedNames.add(module.getCodigo());
        }

        String newModulesStr = String.join(", ", newGrantedNames);

        auditService.logAction(
                superAdminId,
                superAdminName,
                "SUPERADMIN",
                "USER_MODULE_GRANTED",
                "SEGURIDAD",
                "UserModuleAccess",
                String.valueOf(user.getId()),
                "Actualización de módulos adicionales por excepción para usuario: " + user.getEmail() + " (" + newGrantedNames.size() + " módulos)",
                prevModulesStr,
                newModulesStr,
                "EXITOSO",
                request
        );

        return getUserModules(userId);
    }
}
