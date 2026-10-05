package com.exportrace.dto;

public class SuperAdminSettingsDTO {
    private int sessionTimeoutMinutes = 1440; // 24 hours
    private int maxFailedLoginAttempts = 5;
    private boolean requireComplexPasswords = true;
    private int auditRetentionDays = 365;
    private boolean maintenanceMode = false;
    private String systemVersion = "ExporTrace v2.6-RBAC-Enterprise";
    private String databaseEngine = "SQLite (exportrace.db)";
    private String activeSecurityProfile = "Strict RBAC + Real-Time Method Security";

    public SuperAdminSettingsDTO() {}

    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }

    public int getMaxFailedLoginAttempts() { return maxFailedLoginAttempts; }
    public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) { this.maxFailedLoginAttempts = maxFailedLoginAttempts; }

    public boolean isRequireComplexPasswords() { return requireComplexPasswords; }
    public void setRequireComplexPasswords(boolean requireComplexPasswords) { this.requireComplexPasswords = requireComplexPasswords; }

    public int getAuditRetentionDays() { return auditRetentionDays; }
    public void setAuditRetentionDays(int auditRetentionDays) { this.auditRetentionDays = auditRetentionDays; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getSystemVersion() { return systemVersion; }
    public void setSystemVersion(String systemVersion) { this.systemVersion = systemVersion; }

    public String getDatabaseEngine() { return databaseEngine; }
    public void setDatabaseEngine(String databaseEngine) { this.databaseEngine = databaseEngine; }

    public String getActiveSecurityProfile() { return activeSecurityProfile; }
    public void setActiveSecurityProfile(String activeSecurityProfile) { this.activeSecurityProfile = activeSecurityProfile; }
}
