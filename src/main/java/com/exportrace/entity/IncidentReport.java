package com.exportrace.entity;

import com.exportrace.entity.enums.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "incident_reports",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_incident_code", columnNames = "code"),
        @UniqueConstraint(name = "uk_incident_correlation_id", columnNames = "correlation_id")
    },
    indexes = {
        @Index(name = "idx_incident_reporter_user", columnList = "reporter_user_id"),
        @Index(name = "idx_incident_status", columnList = "status"),
        @Index(name = "idx_incident_priority", columnList = "priority"),
        @Index(name = "idx_incident_sync_status", columnList = "sync_status"),
        @Index(name = "idx_incident_created_at", columnList = "created_at")
    }
)
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 64)
    private String correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;

    @Column(name = "reporter_username", nullable = false, length = 150)
    private String reporterUsername;

    @Column(name = "reporter_email", nullable = false, length = 150)
    private String reporterEmail;

    @Column(name = "reporter_role", nullable = false, length = 50)
    private String reporterRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private IncidentCategory category;

    @Column(name = "summary", nullable = false, length = 200)
    private String summary;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact", nullable = false, length = 20)
    private IncidentImpact impact;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private IncidentUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.REPORTED;

    @Column(name = "module", length = 60)
    private String module;

    @Column(name = "route", length = 255)
    private String route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_lot_id", nullable = true)
    private Lot relatedLot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "jira_issue_key", length = 50)
    private String jiraIssueKey;

    @Column(name = "jira_request_id", length = 50)
    private String jiraRequestId;

    @Column(name = "jira_last_sync_at")
    private LocalDateTime jiraLastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 30)
    private JiraSyncStatus syncStatus = JiraSyncStatus.PENDING;

    @Column(name = "sync_attempts", nullable = false)
    private Integer syncAttempts = 0;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "assigned_agent_name", length = 150)
    private String assignedAgentName;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    /**
     * LOCAL_ESTIMATE mientras jiraIssueKey sea null.
     * Una vez sincronizado con Jira Service Management, Jira es la autoridad operacional del SLA.
     */
    @Column(name = "sla_first_response_target_at")
    private LocalDateTime slaFirstResponseTargetAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "sla_resolution_target_at")
    private LocalDateTime slaResolutionTargetAt;

    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    @Version
    @Column(name = "version")
    private Long version;

    public IncidentReport() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = IncidentStatus.REPORTED;
        this.syncStatus = JiraSyncStatus.PENDING;
        this.syncAttempts = 0;
        this.slaBreached = false;
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
            this.status = IncidentStatus.REPORTED;
        }
        if (this.syncStatus == null) {
            this.syncStatus = JiraSyncStatus.PENDING;
        }
        if (this.syncAttempts == null) {
            this.syncAttempts = 0;
        }
        if (this.slaBreached == null) {
            this.slaBreached = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public User getReporterUser() { return reporterUser; }
    public void setReporterUser(User reporterUser) { this.reporterUser = reporterUser; }

    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }

    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public IncidentImpact getImpact() { return impact; }
    public void setImpact(IncidentImpact impact) { this.impact = impact; }

    public IncidentUrgency getUrgency() { return urgency; }
    public void setUrgency(IncidentUrgency urgency) { this.urgency = urgency; }

    public IncidentPriority getPriority() { return priority; }
    public void setPriority(IncidentPriority priority) { this.priority = priority; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public Lot getRelatedLot() { return relatedLot; }
    public void setRelatedLot(Lot relatedLot) { this.relatedLot = relatedLot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getJiraIssueKey() { return jiraIssueKey; }
    public void setJiraIssueKey(String jiraIssueKey) { this.jiraIssueKey = jiraIssueKey; }

    public String getJiraRequestId() { return jiraRequestId; }
    public void setJiraRequestId(String jiraRequestId) { this.jiraRequestId = jiraRequestId; }

    public LocalDateTime getJiraLastSyncAt() { return jiraLastSyncAt; }
    public void setJiraLastSyncAt(LocalDateTime jiraLastSyncAt) { this.jiraLastSyncAt = jiraLastSyncAt; }

    public JiraSyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(JiraSyncStatus syncStatus) { this.syncStatus = syncStatus; }

    public Integer getSyncAttempts() { return syncAttempts; }
    public void setSyncAttempts(Integer syncAttempts) { this.syncAttempts = syncAttempts; }

    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }

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

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
