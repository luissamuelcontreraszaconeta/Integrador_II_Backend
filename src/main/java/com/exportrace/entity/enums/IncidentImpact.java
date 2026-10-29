package com.exportrace.entity.enums;

public enum IncidentImpact {
    HIGH("Alto", "Bloquea un proceso crítico o afecta a múltiples usuarios/lotes"),
    MEDIUM("Medio", "Degrada parcialmente la operación sin detenerla por completo"),
    LOW("Bajo", "Inconveniente menor o consulta que no detiene el flujo");

    private final String label;
    private final String description;

    IncidentImpact(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static IncidentImpact fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return IncidentImpact.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Nivel de impacto inválido: " + value);
        }
    }
}
