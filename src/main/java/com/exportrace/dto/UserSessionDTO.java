package com.exportrace.dto;

import com.exportrace.entity.UserSession;
import java.time.LocalDateTime;

public class UserSessionDTO {
    private String id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revocationReason;
    private String ipAddress;
    private String userAgent;
    private String status;

    public UserSessionDTO() {}

    public UserSessionDTO(UserSession session) {
        if (session != null) {
            this.id = session.getId();
            if (session.getUser() != null) {
                this.userId = session.getUser().getId();
                this.userName = session.getUser().getNombre() + (session.getUser().getApellido() != null ? " " + session.getUser().getApellido() : "");
                this.userEmail = session.getUser().getEmail();
                this.userRole = session.getUser().getRole() != null ? session.getUser().getRole().getNombre() : "UNKNOWN";
            }
            this.createdAt = session.getCreatedAt();
            this.lastActivityAt = session.getLastActivityAt();
            this.expiresAt = session.getExpiresAt();
            this.revokedAt = session.getRevokedAt();
            this.revocationReason = session.getRevocationReason();
            this.ipAddress = session.getIpAddress();
            this.userAgent = session.getUserAgent();
            this.status = session.getStatus();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
