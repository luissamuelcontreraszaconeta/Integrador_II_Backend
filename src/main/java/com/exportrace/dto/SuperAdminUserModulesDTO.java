package com.exportrace.dto;

import java.util.List;

public class SuperAdminUserModulesDTO {
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private List<ModuleDTO> inheritedModules;
    private List<ModuleDTO> individualGrantedModules;
    private List<ModuleDTO> availableModules;

    public SuperAdminUserModulesDTO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public List<ModuleDTO> getInheritedModules() { return inheritedModules; }
    public void setInheritedModules(List<ModuleDTO> inheritedModules) { this.inheritedModules = inheritedModules; }

    public List<ModuleDTO> getIndividualGrantedModules() { return individualGrantedModules; }
    public void setIndividualGrantedModules(List<ModuleDTO> individualGrantedModules) { this.individualGrantedModules = individualGrantedModules; }

    public List<ModuleDTO> getAvailableModules() { return availableModules; }
    public void setAvailableModules(List<ModuleDTO> availableModules) { this.availableModules = availableModules; }
}
