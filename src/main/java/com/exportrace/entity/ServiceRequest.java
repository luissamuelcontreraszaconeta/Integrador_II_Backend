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
    private String lotCode;

    @Column(name = "requested_information", nullable = false, columnDefinition = "TEXT")
    private String requestedInformation;

    @Column(name = "additional_detail", columnDefinition = "TEXT")
    private String additionalDetail;

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
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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
