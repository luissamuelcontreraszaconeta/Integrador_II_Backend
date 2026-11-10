package com.exportrace.dto;

import com.exportrace.entity.ServiceRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de salida seguro para ServiceRequest (6 servicios del catálogo + sincronización Jira -> ExporTrace).
 * No expone lastSyncError técnico crudo, rutas internas de disco ni credenciales.
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

    private String documentQueryType;
    private String observedField;
    private String problemDescription;
    private String fieldToCorrect;
    private String currentValue;
    private String proposedValue;
    private String justification;
    private String workerName;
    private String affectedUser;
    private String institutionalEmail;
    private String area;
    private String requiredRole;
    private String accessRequestType;
    private String currentRoleField;
    private String requiredPermissions;
    private String reason;

    private String status;
    private String statusLabel;
    private boolean requiresApproval;
    private String approvalStatus;
    private String approvalStatusLabel;
    private String jiraStatus;
    private String jiraSlaSummary;

    private boolean hasAttachment;
    private String attachmentOriginalName;
    private String attachmentMimeType;
    private Long attachmentSize;
    private boolean jiraAttachmentSynced;

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

    private List<ServiceRequestCommentDTO> comments = new ArrayList<>();

    public ServiceRequestDTO() {}

    public ServiceRequestDTO(ServiceRequest entity) {
        this(entity, null);
    }

    public ServiceRequestDTO(ServiceRequest entity, List<ServiceRequestCommentDTO> publicComments) {
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

        this.documentQueryType = entity.getDocumentQueryType();
        this.observedField = entity.getObservedField();
        this.problemDescription = entity.getProblemDescription();
        this.fieldToCorrect = entity.getFieldToCorrect();
        this.currentValue = entity.getCurrentValue();
        this.proposedValue = entity.getProposedValue();
        this.justification = entity.getJustification();
        this.workerName = entity.getWorkerName();
        this.affectedUser = entity.getAffectedUser();
        this.institutionalEmail = entity.getInstitutionalEmail();
        this.area = entity.getArea();
        this.requiredRole = entity.getRequiredRole();
        this.accessRequestType = entity.getAccessRequestType();
        this.currentRoleField = entity.getCurrentRoleField();
        this.requiredPermissions = entity.getRequiredPermissions();
        this.reason = entity.getReason();

        if (entity.getStatus() != null) {
            this.status = entity.getStatus().name();
            this.statusLabel = entity.getStatus().getLabel();
        }
        this.requiresApproval = Boolean.TRUE.equals(entity.getRequiresApproval())
                || "SR-INF-04".equalsIgnoreCase(entity.getServiceCode())
                || "SR-ACC-01".equalsIgnoreCase(entity.getServiceCode())
                || "SR-ACC-02".equalsIgnoreCase(entity.getServiceCode());

        this.approvalStatus = entity.getApprovalStatus() != null
                ? entity.getApprovalStatus()
                : (this.requiresApproval ? "PENDING_APPROVAL" : "NOT_REQUIRED");
        this.approvalStatusLabel = resolveApprovalStatusLabel(this.approvalStatus);

        this.jiraStatus = entity.getJiraStatus();
        this.jiraSlaSummary = entity.getJiraSlaSummary();

        this.hasAttachment = entity.getAttachmentOriginalName() != null && !entity.getAttachmentOriginalName().isBlank();
        this.attachmentOriginalName = entity.getAttachmentOriginalName();
        this.attachmentMimeType = entity.getAttachmentMimeType();
        this.attachmentSize = entity.getAttachmentSize();
        this.jiraAttachmentSynced = Boolean.TRUE.equals(entity.getJiraAttachmentSynced());

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

        if (publicComments != null) {
            this.comments = new ArrayList<>(publicComments);
        }
    }

    private static String resolveApprovalStatusLabel(String approvalStatus) {
        if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
            return "Aprobada";
        }
        if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
            return "Rechazada";
        }
        if ("PENDING_APPROVAL".equalsIgnoreCase(approvalStatus)) {
            return "Pendiente de aprobación";
        }
        return "No requiere aprobación";
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

    public String getDocumentQueryType() { return documentQueryType; }
    public void setDocumentQueryType(String documentQueryType) { this.documentQueryType = documentQueryType; }

    public String getObservedField() { return observedField; }
    public void setObservedField(String observedField) { this.observedField = observedField; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getFieldToCorrect() { return fieldToCorrect; }
    public void setFieldToCorrect(String fieldToCorrect) { this.fieldToCorrect = fieldToCorrect; }

    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }

    public String getProposedValue() { return proposedValue; }
    public void setProposedValue(String proposedValue) { this.proposedValue = proposedValue; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }

    public String getAffectedUser() { return affectedUser; }
    public void setAffectedUser(String affectedUser) { this.affectedUser = affectedUser; }

    public String getInstitutionalEmail() { return institutionalEmail; }
    public void setInstitutionalEmail(String institutionalEmail) { this.institutionalEmail = institutionalEmail; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getAccessRequestType() { return accessRequestType; }
    public void setAccessRequestType(String accessRequestType) { this.accessRequestType = accessRequestType; }

    public String getCurrentRoleField() { return currentRoleField; }
    public void setCurrentRoleField(String currentRoleField) { this.currentRoleField = currentRoleField; }

    public String getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(String requiredPermissions) { this.requiredPermissions = requiredPermissions; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApprovalStatusLabel() { return approvalStatusLabel; }
    public void setApprovalStatusLabel(String approvalStatusLabel) { this.approvalStatusLabel = approvalStatusLabel; }

    public String getJiraStatus() { return jiraStatus; }
    public void setJiraStatus(String jiraStatus) { this.jiraStatus = jiraStatus; }

    public String getJiraSlaSummary() { return jiraSlaSummary; }
    public void setJiraSlaSummary(String jiraSlaSummary) { this.jiraSlaSummary = jiraSlaSummary; }

    public boolean isHasAttachment() { return hasAttachment; }
    public void setHasAttachment(boolean hasAttachment) { this.hasAttachment = hasAttachment; }

    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String attachmentOriginalName) { this.attachmentOriginalName = attachmentOriginalName; }

    public String getAttachmentMimeType() { return attachmentMimeType; }
    public void setAttachmentMimeType(String attachmentMimeType) { this.attachmentMimeType = attachmentMimeType; }

    public Long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(Long attachmentSize) { this.attachmentSize = attachmentSize; }

    public boolean isJiraAttachmentSynced() { return jiraAttachmentSynced; }
    public void setJiraAttachmentSynced(boolean jiraAttachmentSynced) { this.jiraAttachmentSynced = jiraAttachmentSynced; }

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

    public List<ServiceRequestCommentDTO> getComments() { return comments; }
    public void setComments(List<ServiceRequestCommentDTO> comments) { this.comments = comments != null ? comments : new ArrayList<>(); }
}
