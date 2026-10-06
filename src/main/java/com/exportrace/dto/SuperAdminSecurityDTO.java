package com.exportrace.dto;

import java.util.List;

public class SuperAdminSecurityDTO {
    private long failedLoginsCount;
    private long accessDeniedCount;
    private long deactivatedUsersCount;
    private long totalSuperAdmins;
    private long activeSessionsCount;
    private long expiredSessionsCount;
    private long revokedSessionsCount;
    private long totalPoliciesCount;
    private List<AuditLogDTO> failedLoginEvents;
    private List<AuditLogDTO> accessDeniedEvents;
    private List<AuditLogDTO> recentPrivilegeChanges;

    public SuperAdminSecurityDTO() {}

    public long getFailedLoginsCount() { return failedLoginsCount; }
    public void setFailedLoginsCount(long failedLoginsCount) { this.failedLoginsCount = failedLoginsCount; }

    public long getAccessDeniedCount() { return accessDeniedCount; }
    public void setAccessDeniedCount(long accessDeniedCount) { this.accessDeniedCount = accessDeniedCount; }

    public long getDeactivatedUsersCount() { return deactivatedUsersCount; }
    public void setDeactivatedUsersCount(long deactivatedUsersCount) { this.deactivatedUsersCount = deactivatedUsersCount; }

    public long getTotalSuperAdmins() { return totalSuperAdmins; }
    public void setTotalSuperAdmins(long totalSuperAdmins) { this.totalSuperAdmins = totalSuperAdmins; }

    public long getActiveSessionsCount() { return activeSessionsCount; }
    public void setActiveSessionsCount(long activeSessionsCount) { this.activeSessionsCount = activeSessionsCount; }

    public long getExpiredSessionsCount() { return expiredSessionsCount; }
    public void setExpiredSessionsCount(long expiredSessionsCount) { this.expiredSessionsCount = expiredSessionsCount; }

    public long getRevokedSessionsCount() { return revokedSessionsCount; }
    public void setRevokedSessionsCount(long revokedSessionsCount) { this.revokedSessionsCount = revokedSessionsCount; }

    public long getTotalPoliciesCount() { return totalPoliciesCount; }
    public void setTotalPoliciesCount(long totalPoliciesCount) { this.totalPoliciesCount = totalPoliciesCount; }

    public List<AuditLogDTO> getFailedLoginEvents() { return failedLoginEvents; }
    public void setFailedLoginEvents(List<AuditLogDTO> failedLoginEvents) { this.failedLoginEvents = failedLoginEvents; }

    public List<AuditLogDTO> getAccessDeniedEvents() { return accessDeniedEvents; }
    public void setAccessDeniedEvents(List<AuditLogDTO> accessDeniedEvents) { this.accessDeniedEvents = accessDeniedEvents; }

    public List<AuditLogDTO> getRecentPrivilegeChanges() { return recentPrivilegeChanges; }
    public void setRecentPrivilegeChanges(List<AuditLogDTO> recentPrivilegeChanges) { this.recentPrivilegeChanges = recentPrivilegeChanges; }
}
