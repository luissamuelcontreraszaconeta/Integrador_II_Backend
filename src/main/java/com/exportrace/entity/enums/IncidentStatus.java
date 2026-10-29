package com.exportrace.entity.enums;

public enum IncidentStatus {
    REPORTED("Reportada"),
    TRIAGE("En Triaje"),
    IN_PROGRESS("En Progreso"),
    WAITING_USER("Esperando al Usuario"),
    ESCALATED("Escalada"),
    RESOLVED("Resuelta"),
    CLOSED("Cerrada"),
    REOPENED("Reabierta");

    private final String label;

    IncidentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
