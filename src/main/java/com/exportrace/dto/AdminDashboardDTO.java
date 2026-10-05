package com.exportrace.dto;

import java.util.List;

public class AdminDashboardDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalRoles;
    private long totalModules;
    private long totalAuditEvents;
    private long accessDeniedCount;
    private List<AuditLogDTO> recentActivity;

    public AdminDashboardDTO() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }

    public long getTotalRoles() { return totalRoles; }
    public void setTotalRoles(long totalRoles) { this.totalRoles = totalRoles; }

    public long getTotalModules() { return totalModules; }
    public void setTotalModules(long totalModules) { this.totalModules = totalModules; }

    public long getTotalAuditEvents() { return totalAuditEvents; }
    public void setTotalAuditEvents(long totalAuditEvents) { this.totalAuditEvents = totalAuditEvents; }

    public long getAccessDeniedCount() { return accessDeniedCount; }
    public void setAccessDeniedCount(long accessDeniedCount) { this.accessDeniedCount = accessDeniedCount; }

    public List<AuditLogDTO> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<AuditLogDTO> recentActivity) { this.recentActivity = recentActivity; }
}
