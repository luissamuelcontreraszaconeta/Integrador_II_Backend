package com.exportrace.dto;

import com.exportrace.entity.ServiceRequest;

import java.time.LocalDateTime;

/**
 * DTO de salida seguro para ServiceRequest.
 * No expone lastSyncError técnico crudo ni credenciales.
 */
public class ServiceRequestDTO {

    private Long id;
    private String code;
    private String serviceCode;
    private String serviceTitle;
    private String categoryKey;
    private String categoryLabel;
    private String correlationId;
    private String requesterUsername;
    private String requesterEmail;
    private String requesterRole;
    private String summary;
    private String lotCode;
    private String requestedInformation;
    private String additionalDetail;
    private String status;
    private String statusLabel;
    private boolean requiresApproval;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String slaTarget;
    private String slaAuthority;
    private String jiraIssueKey;
    private String jiraRequestId;
    private String jiraRequestTypeId;
    private LocalDateTime jiraLastSyncAt;
    private String syncStatus;
    private String syncStatusLabel;
    private Integer syncAttempts;

    public ServiceRequestDTO() {}

    public ServiceRequestDTO(ServiceRequest entity) {
        this.id = entity.getId();
        this.code = entity.getCode();
        this.serviceCode = entity.getServiceCode();
        this.serviceTitle = entity.getServiceTitle();
        this.categoryKey = entity.getCategoryKey();
        this.categoryLabel = entity.getCategoryLabel();
        this.correlationId = entity.getCorrelationId();
        this.requesterUsername = entity.getRequesterUsername();
        this.requesterEmail = entity.getRequesterEmail();
        this.requesterRole = entity.getRequesterRole();
        this.summary = entity.getSummary();
        this.lotCode = entity.getLotCode();
        this.requestedInformation = entity.getRequestedInformation();
        this.additionalDetail = entity.getAdditionalDetail();
        if (entity.getStatus() != null) {
            this.status = entity.getStatus().name();
            this.statusLabel = entity.getStatus().getLabel();
        }
        this.requiresApproval = false;
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.slaTarget = entity.getSlaTarget();
        this.jiraIssueKey = entity.getJiraIssueKey();
        this.jiraRequestId = entity.getJiraRequestId();
        this.jiraRequestTypeId = entity.getJiraRequestTypeId();
        this.jiraLastSyncAt = entity.getJiraLastSyncAt();
        if (entity.getSyncStatus() != null) {
            this.syncStatus = entity.getSyncStatus().name();
            this.syncStatusLabel = entity.getSyncStatus().getLabel();
        }
        this.syncAttempts = entity.getSyncAttempts();
        this.slaAuthority = (entity.getJiraIssueKey() != null && !entity.getJiraIssueKey().isBlank())
                ? "JIRA_AUTHORITATIVE"
                : "LOCAL_ESTIMATE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }

    public String getCategoryKey() { return categoryKey; }
    public void setCategoryKey(String categoryKey) { this.categoryKey = categoryKey; }

    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String requesterRole) { this.requesterRole = requesterRole; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }

    public String getRequestedInformation() { return requestedInformation; }
    public void setRequestedInformation(String requestedInformation) { this.requestedInformation = requestedInformation; }

    public String getAdditionalDetail() { return additionalDetail; }
    public void setAdditionalDetail(String additionalDetail) { this.additionalDetail = additionalDetail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSlaTarget() { return slaTarget; }
    public void setSlaTarget(String slaTarget) { this.slaTarget = slaTarget; }

    public String getSlaAuthority() { return slaAuthority; }
    public void setSlaAuthority(String slaAuthority) { this.slaAuthority = slaAuthority; }

    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }

    public String getJiraRequestId() { return jiraRequestId; }
    public void setJiraRequestId(String jiraRequestId) { this.jiraRequestId = jiraRequestId; }

    public String getJiraRequestTypeId() { return jiraRequestTypeId; }
    public void setJiraRequestTypeId(String jiraRequestTypeId) { this.jiraRequestTypeId = jiraRequestTypeId; }

    public LocalDateTime getJiraLastSyncAt() { return jiraLastSyncAt; }
    public void setJiraLastSyncAt(LocalDateTime jiraLastSyncAt) { this.jiraLastSyncAt = jiraLastSyncAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getSyncStatusLabel() { return syncStatusLabel; }
    public void setSyncStatusLabel(String syncStatusLabel) { this.syncStatusLabel = syncStatusLabel; }

    public Integer getSyncAttempts() { return syncAttempts; }
    public void setSyncAttempts(Integer syncAttempts) { this.syncAttempts = syncAttempts; }
}
