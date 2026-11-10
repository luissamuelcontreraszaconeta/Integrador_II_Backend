package com.exportrace.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "service_request_comments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sr_comment_jira_id", columnNames = "jira_comment_id")
    },
    indexes = {
        @Index(name = "idx_sr_comment_request_id", columnList = "service_request_id"),
        @Index(name = "idx_sr_comment_created_at", columnList = "created_at")
    }
)
public class ServiceRequestComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_request_id", nullable = false)
    private Long serviceRequestId;

    @Column(name = "jira_comment_id", unique = true, length = 64)
    private String jiraCommentId;

    @Column(name = "author_display_name", nullable = false, length = 150)
    private String authorDisplayName;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "public_comment", nullable = false)
    private Boolean publicComment = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "source", nullable = false, length = 30)
    private String source = "JIRA";

    public ServiceRequestComment() {}

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.publicComment == null) {
            this.publicComment = Boolean.TRUE;
        }
        if (this.source == null || this.source.isBlank()) {
            this.source = "JIRA";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getServiceRequestId() { return serviceRequestId; }
    public void setServiceRequestId(Long serviceRequestId) { this.serviceRequestId = serviceRequestId; }

    public String getJiraCommentId() { return jiraCommentId; }
    public void setJiraCommentId(String jiraCommentId) { this.jiraCommentId = jiraCommentId; }

    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Boolean getPublicComment() { return publicComment; }
    public void setPublicComment(Boolean publicComment) { this.publicComment = publicComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
