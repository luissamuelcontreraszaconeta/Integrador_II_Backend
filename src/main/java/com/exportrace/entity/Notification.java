package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "target_role", nullable = true)
    private String targetRole; // SUPERADMIN, ADMINISTRADOR, QA, LOGISTICA, PRODUCCION, GERENCIA, ALL

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String type; // QUALITY_OBSERVED, COLD_CHAIN_ALERT, INSPECTION_PENDING, DOCUMENTATION_INCOMPLETE, CERTIFICATION_PENDING, CERTIFICATION_APPROVED, DISPATCH_PENDING, DISPATCH_AUTHORIZED, SECURITY_ALERT, INFO

    @Column(nullable = false)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @Column(nullable = false)
    private String module; // QUALITY, COLD_CHAIN, LOTS, LOGISTICS, CERTIFICATION, DISPATCH, SECURITY, SYSTEM

    @Column(name = "entity_type")
    private String entityType; // LOT, INSPECTION, CERTIFICATE, DISPATCH, USER

    @Column(name = "entity_id")
    private String entityId;

    private String route; // e.g., /lots/1, /quality/inspect/1, /certification, /dispatch/1

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification() {}

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Notification(User user, String targetRole, String title, String message, String type, String priority, String module, String entityType, String entityId, String route) {
        this.user = user;
        this.targetRole = targetRole;
        this.title = title;
        this.message = message;
        this.type = type;
        this.priority = priority != null ? priority : "NORMAL";
        this.module = module != null ? module : "SYSTEM";
        this.entityType = entityType;
        this.entityId = entityId;
        this.route = route;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
