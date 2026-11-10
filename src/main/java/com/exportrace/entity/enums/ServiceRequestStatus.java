package com.exportrace.entity.enums;

public enum ServiceRequestStatus {
    REGISTERED("Solicitud registrada"),
    IN_REVIEW("En revisión"),
    PENDING_APPROVAL("Pendiente de aprobación"),
    IN_PROGRESS("En progreso"),
    IN_IMPLEMENTATION("En implementación"),
    RESOLVED("Resuelta"),
    REJECTED("Rechazada"),
    COMPLETED("Completada");

    private final String label;

    ServiceRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
