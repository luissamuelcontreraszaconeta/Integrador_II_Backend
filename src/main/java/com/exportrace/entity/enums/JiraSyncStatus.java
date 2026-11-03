package com.exportrace.entity.enums;

public enum JiraSyncStatus {
    PENDING("Pendiente de Sincronización"),
    SYNCED("Sincronizada con Jira"),
    ERROR("Error de Sincronización"),
    RETRY_PENDING("Reintento Pendiente");

    private final String label;

    JiraSyncStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
