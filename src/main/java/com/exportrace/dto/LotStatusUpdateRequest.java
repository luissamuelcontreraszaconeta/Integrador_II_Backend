package com.exportrace.dto;

public class LotStatusUpdateRequest {
    private String status;
    private String userRole;
    private String userName;
    private String comment;

    public LotStatusUpdateRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
