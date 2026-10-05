package com.exportrace.dto;

import java.util.List;

public class SuperAdminDashboardDTO {
    private String period; // month, quarter, year

    // 1. Top KPI Metrics
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long superAdminCount;
    private long totalRoles;
    private long activeModules;
    private long totalAuditEvents;
    private long failedLoginsCount;
    private long accessDeniedCount;

    private long auditedLots;
    private String auditedLotsVariation;

    private long qaInspectionsCount;
    private String qaConformingRate;
    private long qaPendingCount;

    private double exportedVolumeKg;
    private String exportedVolumeFormatted;
    private String exportedVolumeVariation;

    private long qrQueriesCount;
    private String qrQueriesVariation;

    // 2. Structured Distribution and Charts
    private List<PlantDistributionDTO> plantsDistribution;
    private List<PlantLotsChartDTO> lotsByPlant;
    private List<TraceabilityPointDTO> traceabilityEvolution;
    private String traceabilityTotalFormatted;
    private String traceabilityTotalVariation;

    // 3. Actionable Alerts & Drilldown
    private List<DashboardAlertDTO> alerts;

    // 4. Activity and Recent Transactions
    private List<AuditLogDTO> recentActivity;
    private List<RecentLotDTO> recentLots;
    private List<UserAdminDTO> recentUsers;

    public SuperAdminDashboardDTO() {}

    // Inner DTOs
    public static class PlantDistributionDTO {
        private String plantCode;
        private String plantName;
        private long totalLots;
        private double percentage;
        private long conformingLots;
        private long dispatchedLots;
        private String statusNote;
        private String badgeColor;

        public PlantDistributionDTO() {}

        public PlantDistributionDTO(String plantCode, String plantName, long totalLots, double percentage, long conformingLots, long dispatchedLots, String statusNote, String badgeColor) {
            this.plantCode = plantCode;
            this.plantName = plantName;
            this.totalLots = totalLots;
            this.percentage = percentage;
            this.conformingLots = conformingLots;
            this.dispatchedLots = dispatchedLots;
            this.statusNote = statusNote;
            this.badgeColor = badgeColor;
        }

        public String getPlantCode() { return plantCode; }
        public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

        public String getPlantName() { return plantName; }
        public void setPlantName(String plantName) { this.plantName = plantName; }

        public long getTotalLots() { return totalLots; }
        public void setTotalLots(long totalLots) { this.totalLots = totalLots; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public long getConformingLots() { return conformingLots; }
        public void setConformingLots(long conformingLots) { this.conformingLots = conformingLots; }

        public long getDispatchedLots() { return dispatchedLots; }
        public void setDispatchedLots(long dispatchedLots) { this.dispatchedLots = dispatchedLots; }

        public String getStatusNote() { return statusNote; }
        public void setStatusNote(String statusNote) { this.statusNote = statusNote; }

        public String getBadgeColor() { return badgeColor; }
        public void setBadgeColor(String badgeColor) { this.badgeColor = badgeColor; }
    }

    public static class PlantLotsChartDTO {
        private String plant;
        private String fullName;
        private long lots;
        private double percentage;
        private String variation;
        private String fillColor;

        public PlantLotsChartDTO() {}

        public PlantLotsChartDTO(String plant, String fullName, long lots, double percentage, String variation, String fillColor) {
            this.plant = plant;
            this.fullName = fullName;
            this.lots = lots;
            this.percentage = percentage;
            this.variation = variation;
            this.fillColor = fillColor;
        }

        public String getPlant() { return plant; }
        public void setPlant(String plant) { this.plant = plant; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public long getLots() { return lots; }
        public void setLots(long lots) { this.lots = lots; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public String getVariation() { return variation; }
        public void setVariation(String variation) { this.variation = variation; }

        public String getFillColor() { return fillColor; }
        public void setFillColor(String fillColor) { this.fillColor = fillColor; }
    }

    public static class TraceabilityPointDTO {
        private String label;
        private double volumeKg;
        private String formattedVolume;
        private long lotsCount;
        private String variation;

        public TraceabilityPointDTO() {}

        public TraceabilityPointDTO(String label, double volumeKg, String formattedVolume, long lotsCount, String variation) {
            this.label = label;
            this.volumeKg = volumeKg;
            this.formattedVolume = formattedVolume;
            this.lotsCount = lotsCount;
            this.variation = variation;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public double getVolumeKg() { return volumeKg; }
        public void setVolumeKg(double volumeKg) { this.volumeKg = volumeKg; }

        public String getFormattedVolume() { return formattedVolume; }
        public void setFormattedVolume(String formattedVolume) { this.formattedVolume = formattedVolume; }

        public long getLotsCount() { return lotsCount; }
        public void setLotsCount(long lotsCount) { this.lotsCount = lotsCount; }

        public String getVariation() { return variation; }
        public void setVariation(String variation) { this.variation = variation; }
    }

    public static class DashboardAlertDTO {
        private Long id;
        private String title;
        private String message;
        private String priority;
        private String type;
        private String route;
        private String actionLabel;
        private String entityId;

        public DashboardAlertDTO() {}

        public DashboardAlertDTO(Long id, String title, String message, String priority, String type, String route, String actionLabel, String entityId) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.type = type;
            this.route = route;
            this.actionLabel = actionLabel;
            this.entityId = entityId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getRoute() { return route; }
        public void setRoute(String route) { this.route = route; }

        public String getActionLabel() { return actionLabel; }
        public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }

        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
    }

    public static class RecentLotDTO {
        private Long id;
        private String codigo;
        private String productoNombre;
        private String plantaProcesamiento;
        private double pesoNetoKg;
        private String estado;
        private String fechaProduccion;

        public RecentLotDTO() {}

        public RecentLotDTO(Long id, String codigo, String productoNombre, String plantaProcesamiento, double pesoNetoKg, String estado, String fechaProduccion) {
            this.id = id;
            this.codigo = codigo;
            this.productoNombre = productoNombre;
            this.plantaProcesamiento = plantaProcesamiento;
            this.pesoNetoKg = pesoNetoKg;
            this.estado = estado;
            this.fechaProduccion = fechaProduccion;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }

        public String getProductoNombre() { return productoNombre; }
        public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

        public String getPlantaProcesamiento() { return plantaProcesamiento; }
        public void setPlantaProcesamiento(String plantaProcesamiento) { this.plantaProcesamiento = plantaProcesamiento; }

        public double getPesoNetoKg() { return pesoNetoKg; }
        public void setPesoNetoKg(double pesoNetoKg) { this.pesoNetoKg = pesoNetoKg; }

        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }

        public String getFechaProduccion() { return fechaProduccion; }
        public void setFechaProduccion(String fechaProduccion) { this.fechaProduccion = fechaProduccion; }
    }

    // Getters and Setters
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }

    public long getSuperAdminCount() { return superAdminCount; }
    public void setSuperAdminCount(long superAdminCount) { this.superAdminCount = superAdminCount; }

    public long getTotalRoles() { return totalRoles; }
    public void setTotalRoles(long totalRoles) { this.totalRoles = totalRoles; }

    public long getActiveModules() { return activeModules; }
    public void setActiveModules(long activeModules) { this.activeModules = activeModules; }

    public long getTotalAuditEvents() { return totalAuditEvents; }
    public void setTotalAuditEvents(long totalAuditEvents) { this.totalAuditEvents = totalAuditEvents; }

    public long getFailedLoginsCount() { return failedLoginsCount; }
    public void setFailedLoginsCount(long failedLoginsCount) { this.failedLoginsCount = failedLoginsCount; }

    public long getAccessDeniedCount() { return accessDeniedCount; }
    public void setAccessDeniedCount(long accessDeniedCount) { this.accessDeniedCount = accessDeniedCount; }

    public long getAuditedLots() { return auditedLots; }
    public void setAuditedLots(long auditedLots) { this.auditedLots = auditedLots; }

    public String getAuditedLotsVariation() { return auditedLotsVariation; }
    public void setAuditedLotsVariation(String auditedLotsVariation) { this.auditedLotsVariation = auditedLotsVariation; }

    public long getQaInspectionsCount() { return qaInspectionsCount; }
    public void setQaInspectionsCount(long qaInspectionsCount) { this.qaInspectionsCount = qaInspectionsCount; }

    public String getQaConformingRate() { return qaConformingRate; }
    public void setQaConformingRate(String qaConformingRate) { this.qaConformingRate = qaConformingRate; }

    public long getQaPendingCount() { return qaPendingCount; }
    public void setQaPendingCount(long qaPendingCount) { this.qaPendingCount = qaPendingCount; }

    public double getExportedVolumeKg() { return exportedVolumeKg; }
    public void setExportedVolumeKg(double exportedVolumeKg) { this.exportedVolumeKg = exportedVolumeKg; }

    public String getExportedVolumeFormatted() { return exportedVolumeFormatted; }
    public void setExportedVolumeFormatted(String exportedVolumeFormatted) { this.exportedVolumeFormatted = exportedVolumeFormatted; }

    public String getExportedVolumeVariation() { return exportedVolumeVariation; }
    public void setExportedVolumeVariation(String exportedVolumeVariation) { this.exportedVolumeVariation = exportedVolumeVariation; }

    public long getQrQueriesCount() { return qrQueriesCount; }
    public void setQrQueriesCount(long qrQueriesCount) { this.qrQueriesCount = qrQueriesCount; }

    public String getQrQueriesVariation() { return qrQueriesVariation; }
    public void setQrQueriesVariation(String qrQueriesVariation) { this.qrQueriesVariation = qrQueriesVariation; }

    public List<PlantDistributionDTO> getPlantsDistribution() { return plantsDistribution; }
    public void setPlantsDistribution(List<PlantDistributionDTO> plantsDistribution) { this.plantsDistribution = plantsDistribution; }

    public List<PlantLotsChartDTO> getLotsByPlant() { return lotsByPlant; }
    public void setLotsByPlant(List<PlantLotsChartDTO> lotsByPlant) { this.lotsByPlant = lotsByPlant; }

    public List<TraceabilityPointDTO> getTraceabilityEvolution() { return traceabilityEvolution; }
    public void setTraceabilityEvolution(List<TraceabilityPointDTO> traceabilityEvolution) { this.traceabilityEvolution = traceabilityEvolution; }

    public String getTraceabilityTotalFormatted() { return traceabilityTotalFormatted; }
    public void setTraceabilityTotalFormatted(String traceabilityTotalFormatted) { this.traceabilityTotalFormatted = traceabilityTotalFormatted; }

    public String getTraceabilityTotalVariation() { return traceabilityTotalVariation; }
    public void setTraceabilityTotalVariation(String traceabilityTotalVariation) { this.traceabilityTotalVariation = traceabilityTotalVariation; }

    public List<DashboardAlertDTO> getAlerts() { return alerts; }
    public void setAlerts(List<DashboardAlertDTO> alerts) { this.alerts = alerts; }

    public List<AuditLogDTO> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<AuditLogDTO> recentActivity) { this.recentActivity = recentActivity; }

    public List<RecentLotDTO> getRecentLots() { return recentLots; }
    public void setRecentLots(List<RecentLotDTO> recentLots) { this.recentLots = recentLots; }

    public List<UserAdminDTO> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<UserAdminDTO> recentUsers) { this.recentUsers = recentUsers; }
}
