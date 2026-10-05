package com.exportrace.dto;

import com.exportrace.entity.AuditLog;
import java.time.LocalDateTime;

public class AuditLogDTO {
    private Long id;
    private Long userId;
    private String usernameSnapshot;
    private String userRole;
    private String action;
    private String module;
    private String entityType;
    private String entityId;
    private String description;
    private String previousValue;
    private String newValue;
    private String result;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public AuditLogDTO() {}

    public AuditLogDTO(AuditLog log) {
        this.id = log.getId();
        this.userId = log.getUserId();
        this.usernameSnapshot = log.getUsernameSnapshot();
        this.userRole = log.getUserRole();
        this.action = log.getAction();
        this.module = log.getModule();
        this.entityType = log.getEntityType();
        this.entityId = log.getEntityId();
        this.description = log.getDescription();
        this.previousValue = log.getPreviousValue();
        this.newValue = log.getNewValue();
        this.result = log.getResult();
        this.ipAddress = log.getIpAddress();
        this.userAgent = log.getUserAgent();
        this.createdAt = log.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsernameSnapshot() { return usernameSnapshot; }
    public void setUsernameSnapshot(String usernameSnapshot) { this.usernameSnapshot = usernameSnapshot; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
