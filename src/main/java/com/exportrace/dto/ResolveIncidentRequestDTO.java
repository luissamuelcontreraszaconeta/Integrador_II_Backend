package com.exportrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResolveIncidentRequestDTO {

    @NotBlank(message = "La justificación técnica es obligatoria.")
    @Size(min = 15, message = "La justificación técnica debe contener al menos 15 caracteres descriptivos.")
    private String technicalJustification;

    private String actionsTaken;
    private String evidenceUrl;
    private String observations;

    public ResolveIncidentRequestDTO() {}

    public ResolveIncidentRequestDTO(String technicalJustification, String actionsTaken, String observations) {
        this.technicalJustification = technicalJustification;
        this.actionsTaken = actionsTaken;
        this.observations = observations;
    }

    public String getTechnicalJustification() { return technicalJustification; }
    public void setTechnicalJustification(String technicalJustification) { this.technicalJustification = technicalJustification; }

    public String getActionsTaken() { return actionsTaken; }
    public void setActionsTaken(String actionsTaken) { this.actionsTaken = actionsTaken; }

    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
