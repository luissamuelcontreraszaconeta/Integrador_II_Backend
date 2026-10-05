package com.exportrace.dto;

import java.util.List;

public class UpdateRolePermissionsRequest {
    private List<String> permissions;

    public UpdateRolePermissionsRequest() {}

    public UpdateRolePermissionsRequest(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
}
