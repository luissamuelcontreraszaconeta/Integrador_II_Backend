package com.exportrace.entity.enums;

/**
 * Representa la respuesta a la pregunta guiada: "¿Puedes continuar con tu trabajo?"
 * Mapea determinísticamente hacia IncidentUrgency en IncidentPriorityService.
 */
public enum WorkContinuity {
    NO_PUEDE_CONTINUAR(IncidentUrgency.HIGH, "No puedo continuar con mi trabajo"),
    CONTINUA_PARCIALMENTE(IncidentUrgency.MEDIUM, "Puedo continuar parcialmente"),
    PUEDE_CONTINUAR(IncidentUrgency.LOW, "Puedo continuar con mi trabajo");

    private final IncidentUrgency mappedUrgency;
    private final String label;

    WorkContinuity(IncidentUrgency mappedUrgency, String label) {
        this.mappedUrgency = mappedUrgency;
        this.label = label;
    }

    public IncidentUrgency getMappedUrgency() {
        return mappedUrgency;
    }

    public String getLabel() {
        return label;
    }

    public static WorkContinuity fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return WorkContinuity.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Valor de continuidad operativa inválido: " + value);
        }
    }
}
