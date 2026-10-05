package com.exportrace.dto;

import com.exportrace.entity.Notification;
import java.time.LocalDateTime;

public class NotificationDTO {
    private Long id;
    private Long userId;
    private String targetRole;
    private String title;
    private String message;
    private String type;
    private String priority;
    private String module;
    private String entityType;
    private String entityId;
    private String route;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public NotificationDTO() {}

    public NotificationDTO(Notification n) {
        this.id = n.getId();
        this.userId = n.getUser() != null ? n.getUser().getId() : null;
        this.targetRole = n.getTargetRole();
        this.title = n.getTitle();
        this.message = n.getMessage();
        this.type = n.getType();
        this.priority = n.getPriority();
        this.module = n.getModule();
        this.entityType = n.getEntityType();
        this.entityId = n.getEntityId();
        this.route = n.getRoute();
        this.read = n.isRead();
        this.createdAt = n.getCreatedAt();
        this.readAt = n.getReadAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
