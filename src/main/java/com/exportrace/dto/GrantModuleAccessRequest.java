package com.exportrace.dto;

import java.util.List;

public class GrantModuleAccessRequest {
    private List<Long> moduleIds;

    public GrantModuleAccessRequest() {}

    public GrantModuleAccessRequest(List<Long> moduleIds) {
        this.moduleIds = moduleIds;
    }

    public List<Long> getModuleIds() { return moduleIds; }
    public void setModuleIds(List<Long> moduleIds) { this.moduleIds = moduleIds; }
}
