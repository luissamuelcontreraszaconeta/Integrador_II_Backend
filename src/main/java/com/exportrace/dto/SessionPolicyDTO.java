package com.exportrace.dto;

import com.exportrace.entity.SessionPolicy;

import java.time.LocalDateTime;

public class SessionPolicyDTO {
    private Long id;
    private String role;
    private Integer idleTimeoutMinutes;
    private Integer absoluteTimeoutMinutes;
    private Integer warningBeforeMinutes;
    private Boolean enabled;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SessionPolicyDTO() {}

    public SessionPolicyDTO(SessionPolicy policy) {
        if (policy != null) {
            this.id = policy.getId();
            this.role = policy.getRole();
            this.idleTimeoutMinutes = policy.getIdleTimeoutMinutes();
            this.absoluteTimeoutMinutes = policy.getAbsoluteTimeoutMinutes();
            this.warningBeforeMinutes = policy.getWarningBeforeMinutes();
            this.enabled = policy.getEnabled();
            this.updatedAt = policy.getUpdatedAt();
            this.updatedBy = policy.getUpdatedBy();
        }
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
