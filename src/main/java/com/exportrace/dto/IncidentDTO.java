package com.exportrace.dto;

import com.exportrace.entity.IncidentReport;

import java.time.LocalDateTime;

/**
 * DTO de salida seguro para IncidentReport.
 * No expone lastSyncError crudo, rutas físicas de almacenamiento ni credenciales.
 * El SLA local se informa explícitamente con slaAuthority = "LOCAL_ESTIMATE" mientras jiraIssueKey sea null.
 */
public class IncidentDTO {

    private Long id;
    private String code;
    private String category;
    private String categoryCode;
    private String categoryLabel;
    private String summary;
    private String description;
    private String impact;
    private String urgency;
    private String priority;
    private String priorityLabel;
    private String status;
    private String statusLabel;
    private String module;
    private String route;
    private Long relatedLotId;
    private String relatedLotCode;
    private String reporterUsername;
    private String reporterRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String syncStatus;
    private String jiraIssueKey;
    private String assignedAgentName;
    private String resolutionSummary;
    private LocalDateTime slaFirstResponseTargetAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime slaResolutionTargetAt;
    private Boolean slaBreached;
    private String slaAuthority;

    public IncidentDTO() {}

    public IncidentDTO(IncidentReport entity) {
        this.id = entity.getId();
        this.code = entity.getCode();
        if (entity.getCategory() != null) {
            this.category = entity.getCategory().name();
            this.categoryCode = entity.getCategory().getCatalogCode();
            this.categoryLabel = entity.getCategory().getLabel();
        }
        this.summary = entity.getSummary();
        this.description = entity.getDescription();
        this.impact = entity.getImpact() != null ? entity.getImpact().name() : null;
        this.urgency = entity.getUrgency() != null ? entity.getUrgency().name() : null;
        if (entity.getPriority() != null) {
            this.priority = entity.getPriority().name();
            this.priorityLabel = entity.getPriority().getLabel();
        }
        if (entity.getStatus() != null) {
            this.status = entity.getStatus().name();
            this.statusLabel = entity.getStatus().getLabel();
        }
        this.module = entity.getModule();
        this.route = entity.getRoute();
        if (entity.getRelatedLot() != null) {
            this.relatedLotId = entity.getRelatedLot().getId();
            this.relatedLotCode = entity.getRelatedLot().getCodigo();
        }
        this.reporterUsername = entity.getReporterUsername();
        this.reporterRole = entity.getReporterRole();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.resolvedAt = entity.getResolvedAt();
        this.closedAt = entity.getClosedAt();
        this.syncStatus = entity.getSyncStatus() != null ? entity.getSyncStatus().name() : null;
        this.jiraIssueKey = entity.getJiraIssueKey();
        this.assignedAgentName = entity.getAssignedAgentName();
        this.resolutionSummary = entity.getResolutionSummary();
        this.slaFirstResponseTargetAt = entity.getSlaFirstResponseTargetAt();
        this.firstResponseAt = entity.getFirstResponseAt();
        this.slaResolutionTargetAt = entity.getSlaResolutionTargetAt();
        this.slaBreached = entity.getSlaBreached();
        this.slaAuthority = (entity.getJiraIssueKey() != null && !entity.getJiraIssueKey().isBlank())
                ? "JIRA_AUTHORITATIVE"
                : "LOCAL_ESTIMATE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public Long getRelatedLotId() { return relatedLotId; }
    public void setRelatedLotId(Long relatedLotId) { this.relatedLotId = relatedLotId; }

    public String getRelatedLotCode() { return relatedLotCode; }
    public void setRelatedLotCode(String relatedLotCode) { this.relatedLotCode = relatedLotCode; }

    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }

    public String getAssignedAgentName() { return assignedAgentName; }
    public void setAssignedAgentName(String assignedAgentName) { this.assignedAgentName = assignedAgentName; }

    public String getResolutionSummary() { return resolutionSummary; }
    public void setResolutionSummary(String resolutionSummary) { this.resolutionSummary = resolutionSummary; }

    public LocalDateTime getSlaFirstResponseTargetAt() { return slaFirstResponseTargetAt; }
    public void setSlaFirstResponseTargetAt(LocalDateTime slaFirstResponseTargetAt) { this.slaFirstResponseTargetAt = slaFirstResponseTargetAt; }

    public LocalDateTime getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(LocalDateTime firstResponseAt) { this.firstResponseAt = firstResponseAt; }

    public LocalDateTime getSlaResolutionTargetAt() { return slaResolutionTargetAt; }
    public void setSlaResolutionTargetAt(LocalDateTime slaResolutionTargetAt) { this.slaResolutionTargetAt = slaResolutionTargetAt; }

    public Boolean getSlaBreached() { return slaBreached; }
    public void setSlaBreached(Boolean slaBreached) { this.slaBreached = slaBreached; }

    public String getSlaAuthority() { return slaAuthority; }
    public void setSlaAuthority(String slaAuthority) { this.slaAuthority = slaAuthority; }
}
