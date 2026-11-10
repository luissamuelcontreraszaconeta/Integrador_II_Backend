package com.exportrace.dto;

import com.exportrace.entity.ServiceRequestComment;

import java.time.LocalDateTime;

public class ServiceRequestCommentDTO {

    private Long id;
    private Long serviceRequestId;
    private String jiraCommentId;
    private String authorDisplayName;
    private String body;
    private boolean publicComment;
    private LocalDateTime createdAt;
    private String source;

    public ServiceRequestCommentDTO() {}

    public ServiceRequestCommentDTO(ServiceRequestComment entity) {
        this.id = entity.getId();
        this.serviceRequestId = entity.getServiceRequestId();
        this.jiraCommentId = entity.getJiraCommentId();
        this.authorDisplayName = entity.getAuthorDisplayName();
        this.body = entity.getBody();
        this.publicComment = Boolean.TRUE.equals(entity.getPublicComment());
        this.createdAt = entity.getCreatedAt();
        this.source = entity.getSource();
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

    public boolean isPublicComment() { return publicComment; }
    public void setPublicComment(boolean publicComment) { this.publicComment = publicComment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
