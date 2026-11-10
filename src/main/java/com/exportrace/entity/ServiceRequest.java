package com.exportrace.entity;

import com.exportrace.entity.enums.JiraSyncStatus;
import com.exportrace.entity.enums.ServiceRequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "service_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_request_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_service_request_correlation_id", columnNames = "correlation_id")
    },
    indexes = {
        @Index(name = "idx_sr_requester_user", columnList = "requester_user_id"),
        @Index(name = "idx_sr_service_code", columnList = "service_code"),
        @Index(name = "idx_sr_status", columnList = "status"),
        @Index(name = "idx_sr_sync_status", columnList = "sync_status"),
        @Index(name = "idx_sr_created_at", columnList = "created_at")
    }
)
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "service_code", nullable = false, length = 30)
    private String serviceCode;

    @Column(name = "service_title", nullable = false, length = 200)
    private String serviceTitle;

    @Column(name = "category_key", nullable = false, length = 40)
    private String categoryKey;

    @Column(name = "category_label", nullable = false, length = 100)
    private String categoryLabel;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 64)
    private String correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @Column(name = "requester_username", nullable = false, length = 150)
    private String requesterUsername;

    @Column(name = "requester_email", nullable = false, length = 150)
    private String requesterEmail;

    @Column(name = "requester_role", nullable = false, length = 50)
    private String requesterRole;

    @Column(name = "summary", nullable = false, length = 200)
    private String summary;

    @Column(name = "lot_code", nullable = false, length = 60)
    private String lotCode = "";

    @Column(name = "requested_information", nullable = false, columnDefinition = "TEXT")
    private String requestedInformation = "";

    @Column(name = "additional_detail", columnDefinition = "TEXT")
    private String additionalDetail;

    @Column(name = "document_query_type", length = 120)
    private String documentQueryType;

    @Column(name = "observed_field", length = 200)
    private String observedField;

    @Column(name = "problem_description", columnDefinition = "TEXT")
    private String problemDescription;

    @Column(name = "field_to_correct", length = 200)
    private String fieldToCorrect;

    @Column(name = "current_value", columnDefinition = "TEXT")
    private String currentValue;

    @Column(name = "proposed_value", columnDefinition = "TEXT")
    private String proposedValue;

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "worker_name", length = 150)
    private String workerName;

    @Column(name = "affected_user", length = 150)
    private String affectedUser;

    @Column(name = "institutional_email", length = 150)
    private String institutionalEmail;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "required_role", length = 100)
    private String requiredRole;

    @Column(name = "access_request_type", length = 100)
    private String accessRequestType;

    @Column(name = "current_role_field", length = 100)
    private String currentRoleField;

    @Column(name = "required_permissions", length = 150)
    private String requiredPermissions;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requires_approval")
    private Boolean requiresApproval = Boolean.FALSE;

    @Column(name = "approval_status", length = 40)
    private String approvalStatus = "NOT_REQUIRED";

    @Column(name = "jira_status", length = 100)
    private String jiraStatus;

    @Column(name = "jira_sla_summary", length = 255)
    private String jiraSlaSummary;

    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    @Column(name = "attachment_stored_path", length = 500)
    private String attachmentStoredPath;

    @Column(name = "attachment_mime_type", length = 100)
    private String attachmentMimeType;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "attachment_sha256", length = 64)
    private String attachmentSha256;

    @Column(name = "jira_attachment_synced")
    private Boolean jiraAttachmentSynced = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ServiceRequestStatus status = ServiceRequestStatus.REGISTERED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "sla_target", nullable = false, length = 100)
    private String slaTarget = "4 horas hábiles";

    @Column(name = "sla_authority", nullable = false, length = 40)
    private String slaAuthority = "LOCAL_ESTIMATE";

    @Column(name = "jira_issue_key", length = 50)
    private String jiraIssueKey;

    @Column(name = "jira_request_id", length = 50)
    private String jiraRequestId;

    @Column(name = "jira_request_type_id", length = 30)
    private String jiraRequestTypeId;

    @Column(name = "jira_last_sync_at")
    private LocalDateTime jiraLastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private JiraSyncStatus syncStatus = JiraSyncStatus.PENDING;

    @Column(name = "sync_attempts", nullable = false)
    private Integer syncAttempts = 0;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    @Version
    @Column(name = "version")
    private Long version;

    public ServiceRequest() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.status = ServiceRequestStatus.REGISTERED;
        this.syncStatus = JiraSyncStatus.PENDING;
        this.syncAttempts = 0;
        this.slaTarget = "4 horas hábiles";
        this.slaAuthority = "LOCAL_ESTIMATE";
        this.requiresApproval = Boolean.FALSE;
        this.approvalStatus = "NOT_REQUIRED";
        this.jiraAttachmentSynced = Boolean.FALSE;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = ServiceRequestStatus.REGISTERED;
        }
        if (this.syncStatus == null) {
            this.syncStatus = JiraSyncStatus.PENDING;
        }
        if (this.syncAttempts == null) {
            this.syncAttempts = 0;
        }
        if (this.slaTarget == null) {
            this.slaTarget = "4 horas hábiles";
        }
        if (this.slaAuthority == null) {
            this.slaAuthority = "LOCAL_ESTIMATE";
        }
        if (this.lotCode == null) {
            this.lotCode = "";
        }
        if (this.requestedInformation == null) {
            this.requestedInformation = this.summary != null ? this.summary : "";
        }
        if (this.requiresApproval == null) {
            this.requiresApproval = Boolean.FALSE;
        }
        if (this.approvalStatus == null) {
            this.approvalStatus = Boolean.TRUE.equals(this.requiresApproval) ? "PENDING_APPROVAL" : "NOT_REQUIRED";
        }
        if (this.jiraAttachmentSynced == null) {
            this.jiraAttachmentSynced = Boolean.FALSE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.lotCode == null) {
            this.lotCode = "";
        }
        if (this.requestedInformation == null) {
            this.requestedInformation = this.summary != null ? this.summary : "";
        }
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

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

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

    public Boolean getRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(Boolean requiresApproval) { this.requiresApproval = requiresApproval; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getJiraStatus() { return jiraStatus; }
    public void setJiraStatus(String jiraStatus) { this.jiraStatus = jiraStatus; }

    public String getJiraSlaSummary() { return jiraSlaSummary; }
    public void setJiraSlaSummary(String jiraSlaSummary) { this.jiraSlaSummary = jiraSlaSummary; }

    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String attachmentOriginalName) { this.attachmentOriginalName = attachmentOriginalName; }

    public String getAttachmentStoredPath() { return attachmentStoredPath; }
    public void setAttachmentStoredPath(String attachmentStoredPath) { this.attachmentStoredPath = attachmentStoredPath; }

    public String getAttachmentMimeType() { return attachmentMimeType; }
    public void setAttachmentMimeType(String attachmentMimeType) { this.attachmentMimeType = attachmentMimeType; }

    public Long getAttachmentSize() { return attachmentSize; }
    public void setAttachmentSize(Long attachmentSize) { this.attachmentSize = attachmentSize; }

    public String getAttachmentSha256() { return attachmentSha256; }
    public void setAttachmentSha256(String attachmentSha256) { this.attachmentSha256 = attachmentSha256; }

    public Boolean getJiraAttachmentSynced() { return jiraAttachmentSynced; }
    public void setJiraAttachmentSynced(Boolean jiraAttachmentSynced) { this.jiraAttachmentSynced = jiraAttachmentSynced; }

    public ServiceRequestStatus getStatus() { return status; }
    public void setStatus(ServiceRequestStatus status) { this.status = status; }

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

    public JiraSyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(JiraSyncStatus syncStatus) { this.syncStatus = syncStatus; }

    public Integer getSyncAttempts() { return syncAttempts; }
    public void setSyncAttempts(Integer syncAttempts) { this.syncAttempts = syncAttempts; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
