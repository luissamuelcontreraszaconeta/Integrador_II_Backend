package com.exportrace.entity.enums;

public enum IncidentUrgency {
    HIGH("Alta", "No puede continuar con su trabajo"),
    MEDIUM("Media", "Continúa parcialmente con dificultades"),
    LOW("Baja", "Puede continuar con su trabajo");

    private final String label;
    private final String description;

    IncidentUrgency(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
