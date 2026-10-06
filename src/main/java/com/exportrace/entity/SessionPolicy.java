package com.exportrace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_policies")
public class SessionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String role;

    @Column(nullable = false)
    private Integer idleTimeoutMinutes;

    @Column(nullable = false)
    private Integer absoluteTimeoutMinutes;

    @Column(nullable = false)
    private Integer warningBeforeMinutes;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime updatedAt;

    private String updatedBy;

    public SessionPolicy() {}

    public SessionPolicy(String role, Integer idleTimeoutMinutes, Integer absoluteTimeoutMinutes, Integer warningBeforeMinutes, String updatedBy) {
        this.role = role;
        this.idleTimeoutMinutes = idleTimeoutMinutes;
        this.absoluteTimeoutMinutes = absoluteTimeoutMinutes;
        this.warningBeforeMinutes = warningBeforeMinutes;
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }

    @PrePersist
    @PreUpdate
    public void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getIdleTimeoutMinutes() { return idleTimeoutMinutes; }
    public void setIdleTimeoutMinutes(Integer idleTimeoutMinutes) { this.idleTimeoutMinutes = idleTimeoutMinutes; }

    public Integer getAbsoluteTimeoutMinutes() { return absoluteTimeoutMinutes; }
    public void setAbsoluteTimeoutMinutes(Integer absoluteTimeoutMinutes) { this.absoluteTimeoutMinutes = absoluteTimeoutMinutes; }

    public Integer getWarningBeforeMinutes() { return warningBeforeMinutes; }
    public void setWarningBeforeMinutes(Integer warningBeforeMinutes) { this.warningBeforeMinutes = warningBeforeMinutes; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
